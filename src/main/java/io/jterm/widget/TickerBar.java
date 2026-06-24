package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.List;
import java.util.Objects;

/**
 * Single-line stock ticker bar component for embedding in screen layouts.
 * Renders ticker symbols with prices and change percentages, colored
 * green (up) or red (down). Designed as a {@code NORTH} border-layout header.
 *
 * <p>This is the static (non-animated) companion to
 * {@link io.jterm.animation.TickerTape}, intended for interactive
 * screens rather than animated backgrounds.
 */
public class TickerBar extends AbstractComponent {

    private static final String SEPARATOR = "  |  ";
    private static final int PADDING = 1;

    private volatile List<TickerEntry> entries = List.of();

    /** Creates an empty ticker bar. Call {@link #setEntries(List)} to populate. */
    public TickerBar() {
        setFocusable(false);
    }

    /** Creates a ticker bar with the given entries. */
    public TickerBar(List<TickerEntry> entries) {
        setFocusable(false);
        setEntries(entries);
    }

    /** Updates the ticker data and triggers a repaint. */
    public void setEntries(List<TickerEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        invalidate();
    }

    public List<TickerEntry> getEntries() {
        return entries;
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(80, 1);  // single row, width is flexible
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        if (size.rows() <= 0 || size.columns() <= 0) return;

        // Fill background with black.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        if (entries.isEmpty()) return;

        int col = PADDING;
        for (int i = 0; i < entries.size() && col < size.columns(); i++) {
            TickerEntry entry = entries.get(i);

            // Separator before items (except the first).
            if (i > 0) {
                for (int j = 0; j < SEPARATOR.length() && col < size.columns(); j++) {
                    graphics.setCell(col, 0, new TextCell(SEPARATOR.charAt(j),
                            AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK));
                    col++;
                }
            }

            // Render: "SYM $PRICE +/-X.XX%"
            AnsiColor color = entry.changePercent() >= 0
                    ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;

            // Symbol in bold white.
            for (int j = 0; j < entry.symbol().length() && col < size.columns(); j++) {
                graphics.setCell(col, 0, new TextCell(entry.symbol().charAt(j),
                        AnsiColor.BRIGHT_WHITE, AnsiColor.BLACK, SGR.BOLD));
                col++;
            }

            // Space between symbol and price.
            if (col < size.columns()) {
                graphics.setCell(col, 0, new TextCell(' ', color, AnsiColor.BLACK));
                col++;
            }

            // Price and percent in green/red.
            String priceAndPct = formatPriceAndPct(entry);
            for (int j = 0; j < priceAndPct.length() && col < size.columns(); j++) {
                graphics.setCell(col, 0, new TextCell(priceAndPct.charAt(j),
                        color, AnsiColor.BLACK, SGR.BOLD));
                col++;
            }
        }
    }

    private static String formatPriceAndPct(TickerEntry entry) {
        String pct = String.format(java.util.Locale.US, "%+.2f%%", entry.changePercent());
        String price = String.format(java.util.Locale.US, "%.2f", entry.price());
        return price + " " + pct;
    }

    /** Immutable ticker entry: symbol, price, and day change percentage. */
    public record TickerEntry(String symbol, double price, double changePercent) {
        public TickerEntry {
            Objects.requireNonNull(symbol, "symbol");
        }
    }
}