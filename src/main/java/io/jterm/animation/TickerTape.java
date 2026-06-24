package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Animated stock ticker tape background. Items scroll right-to-left across a
 * single row: symbol, latest close price, and day change percentage. Green for
 * up, red for down. Data is supplied by a {@link PriceDataSupplier}; the default
 * supplier attempts to read from the family-office screener database and falls
 * back to a small built-in sample list when the driver or database is not
 * available.
 */
public class TickerTape implements AnimatedBackground {

    private static final int TARGET_FPS = 8;
    private static final int SCROLL_SPEED_CELLS = 1;
    private static final String SEPARATOR = "  |  ";
    private static final long REFRESH_INTERVAL_MS = 60_000;

    // Default sample data used when the screener database cannot be reached.
    private static final List<TickerItem> FALLBACK_ITEMS = List.of(
            new TickerItem("AAPL", 189.52, 186.00),
            new TickerItem("GOOGL", 141.80, 140.10),
            new TickerItem("MSFT", 378.91, 375.20),
            new TickerItem("AMZN", 178.35, 181.50),
            new TickerItem("NVDA", 875.28, 860.00),
            new TickerItem("TSLA", 242.10, 248.50),
            new TickerItem("META", 472.33, 465.00),
            new TickerItem("NFLX", 626.92, 615.00)
    );

    private final Supplier<List<TickerItem>> dataSupplier;

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private volatile int scrollOffset;
    private volatile List<TickerItem> items;
    private volatile long lastRefreshMs;

    /** Creates a ticker tape with data loaded from the default screener source. */
    public TickerTape(TerminalSize preferredSize) {
        this(preferredSize, defaultSupplier());
    }

    /** Creates a ticker tape with a custom data supplier. */
    public TickerTape(TerminalSize preferredSize, Supplier<List<TickerItem>> dataSupplier) {
        this.dataSupplier = Objects.requireNonNull(dataSupplier, "dataSupplier");
        onResize(preferredSize);
        refreshData();
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        // Periodically refresh prices from the data source
        if (System.currentTimeMillis() - lastRefreshMs >= REFRESH_INTERVAL_MS) {
            refreshData();
        }

        // Background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        if (items == null || items.isEmpty()) {
            return;
        }

        int cols = size.columns();
        int row = size.rows() - 1;

        StringBuilder builder = new StringBuilder();
        for (TickerItem item : items) {
            if (!builder.isEmpty()) {
                builder.append(SEPARATOR);
            }
            builder.append(item.renderString());
        }
        String text = builder.toString();
        if (text.isEmpty()) {
            return;
        }

        String padded = text + SEPARATOR;
        int totalWidth = padded.length();
        if (totalWidth <= 0) {
            return;
        }

        scrollOffset = (scrollOffset + SCROLL_SPEED_CELLS) % totalWidth;

        int startIdx = scrollOffset;
        for (int col = 0; col < cols; col++) {
            int idx = (startIdx + col) % totalWidth;
            char ch = padded.charAt(idx);
            int effectiveCol = col;

            // Determine which item this character belongs to so we can color it.
            AnsiColor fg = AnsiColor.BRIGHT_WHITE;
            SGR sgr = SGR.BOLD;

            int itemStart = 0;
            for (TickerItem item : items) {
                String rendered = item.renderString();
                int itemEnd = itemStart + rendered.length();
                if (idx >= itemStart && idx < itemEnd) {
                    fg = item.change() >= 0.0 ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;
                    break;
                }
                // Skip separator for the next item.
                itemStart = itemEnd + SEPARATOR.length();
            }
            // Separator characters keep bright white foreground.

            graphics.setCell(effectiveCol, row, new TextCell(ch, fg, AnsiColor.BLACK, sgr));
        }
    }

    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int targetFps() {
        return TARGET_FPS;
    }

    @Override
    public TerminalSize lastSize() {
        return lastSize;
    }

    /** Visible for tests: set the scroll offset directly. */
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    /** Visible for tests: current scroll offset. */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /** Visible for tests: current rendered items. */
    public List<TickerItem> getItems() {
        return items == null ? List.of() : List.copyOf(items);
    }

    /** Visible for tests: force a data reload. */
    public void refreshData() {
        try {
            this.items = dataSupplier.get();
        } catch (Exception e) {
            this.items = FALLBACK_ITEMS;
        }
        if (this.items == null || this.items.isEmpty()) {
            this.items = FALLBACK_ITEMS;
        }
        lastRefreshMs = System.currentTimeMillis();
    }

    private static Supplier<List<TickerItem>> defaultSupplier() {
        return () -> {
            String url = System.getProperty("screener.url");
            String user = System.getProperty("screener.user");
            String password = System.getProperty("screener.password");
            if (url == null || url.isBlank()) {
                return FALLBACK_ITEMS;
            }

            List<TickerItem> loaded = new ArrayList<>();
            String sql =
                "WITH latest AS (" +
                "  SELECT ticker, date, close, " +
                "         LAG(close) OVER (PARTITION BY ticker ORDER BY date) AS prev_close " +
                "  FROM screener.daily_prices" +
                "), ranked AS (" +
                "  SELECT ticker, date, close, prev_close, " +
                "         ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn " +
                "  FROM latest" +
                ") " +
                "SELECT ticker, close, prev_close FROM ranked WHERE rn = 1 AND prev_close IS NOT NULL " +
                "ORDER BY ticker";

            try (Connection conn = DriverManager.getConnection(url, user == null ? "" : user, password == null ? "" : password);
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String ticker = rs.getString("ticker");
                    double close = rs.getDouble("close");
                    double prev = rs.getDouble("prev_close");
                    loaded.add(new TickerItem(ticker, close, prev));
                }
            } catch (Exception e) {
                return FALLBACK_ITEMS;
            }

            return loaded.isEmpty() ? FALLBACK_ITEMS : loaded;
        };
    }

    /** Immutable ticker item: symbol, latest close, and previous close. */
    public static final class TickerItem {
        private final String symbol;
        private final double close;
        private final double previousClose;

        public TickerItem(String symbol, double close, double previousClose) {
            this.symbol = Objects.requireNonNull(symbol, "symbol");
            this.close = close;
            this.previousClose = previousClose;
        }

        public String symbol() { return symbol; }
        public double close() { return close; }
        public double previousClose() { return previousClose; }

        public double change() {
            if (previousClose == 0.0) {
                return 0.0;
            }
            return (close - previousClose) / previousClose * 100.0;
        }

        public String renderString() {
            String percent = String.format(java.util.Locale.US, "%.2f%%", change());
            return symbol + " " + String.format(java.util.Locale.US, "%.2f", close) + " " + percent;
        }

        @Override
        public String toString() {
            return renderString();
        }
    }
}
