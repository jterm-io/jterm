package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import io.jterm.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animated single-line stock ticker bar that scrolls right-to-left.
 * Designed for embedding in screen layouts (e.g. status bar at the bottom).
 *
 * <p>Call {@link #startAnimation()} to begin scrolling and
 * {@link #stopAnimation()} to stop. The animation runs on a daemon thread
 * that advances the scroll offset and triggers repaints via
 * {@link #invalidate()}.
 *
 * <p>This is the embeddable companion to
 * {@link io.jterm.animation.TickerTape}, for interactive screens
 * rather than animated backgrounds.
 */
public class TickerBar extends AbstractComponent {

    private static final int SCROLL_SPEED_CELLS = 1;
    private static final int TARGET_FPS = 8;  // matches TickerTape animated background
    private static final String SEPARATOR = "  |  ";

    private volatile List<TickerEntry> entries = List.of();
    private volatile int scrollOffset = 0;
    private volatile Runnable onTick;
    private final AtomicBoolean animating = new AtomicBoolean(false);
    private AnimationTimer timer;

    // Pre-built scroll text and per-character color map, rebuilt on setEntries.
    private volatile char[] scrollChars = new char[0];
    private volatile AnsiColor[] scrollColors = new AnsiColor[0];

    /** Creates an empty ticker bar. Call {@link #setEntries(List)} to populate. */
    public TickerBar() {
        setFocusable(false);
    }

    /** Creates a ticker bar with the given entries. */
    public TickerBar(List<TickerEntry> entries) {
        setFocusable(false);
        setEntries(entries);
    }

    /**
     * Sets a callback invoked after each scroll tick. The callback should
     * call {@code gui.requestRefresh()} to trigger a screen repaint.
     * Without this, the animation only updates on keystrokes.
     */
    public void setOnTick(Runnable onTick) {
        this.onTick = onTick;
    }

    /** Updates the ticker data and triggers a repaint. */
    public void setEntries(List<TickerEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.scrollOffset = 0;
        rebuildScrollData();
        invalidate();
    }

    public List<TickerEntry> getEntries() {
        return entries;
    }

    /** Visible for tests: current scroll offset. */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /** Visible for tests: set scroll offset directly. */
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }

    /**
     * Starts the scroll animation on a daemon thread. Safe to call multiple
     * times — only the first call starts the scheduler.
     */
    public void startAnimation() {
        if (animating.compareAndSet(false, true)) {
            timer = new AnimationTimer(TARGET_FPS, this::tick);
            timer.start();
        }
    }

    /**
     * Stops the scroll animation. Safe to call when not animating.
     */
    public void stopAnimation() {
        if (animating.compareAndSet(true, false)) {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
    }

    public boolean isAnimating() {
        return animating.get();
    }

    /**
     * Advances the scroll offset by one cell and triggers a repaint.
     * Called automatically by the animation scheduler, or can be called
     * manually for testing.
     */
    void tick() {
        if (scrollChars.length == 0) return;
        scrollOffset = (scrollOffset + SCROLL_SPEED_CELLS) % scrollChars.length;
        invalidate();
        if (onTick != null) {
            onTick.run();
        }
    }

    /** Visible for tests: length of the full scroll text. */
    int buildScrollTextLen() {
        return scrollChars.length;
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

        if (scrollChars.length == 0) return;

        int totalWidth = scrollChars.length;
        int cols = size.columns();

        for (int col = 0; col < cols; col++) {
            int idx = (scrollOffset + col) % totalWidth;
            char ch = scrollChars[idx];
            AnsiColor fg = scrollColors[idx];
            SGR[] mods = (fg == AnsiColor.BRIGHT_BLACK) ? new SGR[0] : new SGR[]{SGR.BOLD};
            graphics.setCell(col, 0, new TextCell(ch, fg, AnsiColor.BLACK, mods));
        }
    }

    /**
     * Pre-builds the scroll text and per-character color array from the
     * current entries. Called once on {@link #setEntries(List)} so that
     * {@link #drawComponent(TextGraphics)} is O(cols) per frame with no
     * string allocation.
     */
    private void rebuildScrollData() {
        if (entries.isEmpty()) {
            scrollChars = new char[0];
            scrollColors = new AnsiColor[0];
            return;
        }

        StringBuilder text = new StringBuilder();
        List<AnsiColor> colors = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            TickerEntry entry = entries.get(i);
            AnsiColor priceColor = entry.changePercent() >= 0
                    ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;

            // Separator before entries (except the first).
            if (i > 0) {
                for (int j = 0; j < SEPARATOR.length(); j++) {
                    text.append(SEPARATOR.charAt(j));
                    colors.add(AnsiColor.BRIGHT_BLACK);
                }
            }

            // Symbol in bold white.
            for (int j = 0; j < entry.symbol().length(); j++) {
                text.append(entry.symbol().charAt(j));
                colors.add(AnsiColor.BRIGHT_WHITE);
            }

            // Space between symbol and price.
            text.append(' ');
            colors.add(priceColor);

            // Price and percent in green/red.
            String priceAndPct = formatPriceAndPct(entry);
            for (int j = 0; j < priceAndPct.length(); j++) {
                text.append(priceAndPct.charAt(j));
                colors.add(priceColor);
            }
        }

        // Trailing separator for seamless wrap.
        for (int j = 0; j < SEPARATOR.length(); j++) {
            text.append(SEPARATOR.charAt(j));
            colors.add(AnsiColor.BRIGHT_BLACK);
        }

        char[] chars = new char[text.length()];
        text.getChars(0, text.length(), chars, 0);
        scrollChars = chars;
        scrollColors = colors.toArray(new AnsiColor[0]);
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