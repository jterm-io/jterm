package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private static final String SEPARATOR = "  |  ";

    private volatile List<TickerEntry> entries = List.of();
    private volatile int scrollOffset = 0;
    private final AtomicBoolean animating = new AtomicBoolean(false);
    private ScheduledExecutorService animator;

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
        this.scrollOffset = 0;
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
            animator = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TickerBar-animator");
                t.setDaemon(true);
                return t;
            });
            animator.scheduleAtFixedRate(this::tick, 200, 200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the scroll animation. Safe to call when not animating.
     */
    public void stopAnimation() {
        if (animating.compareAndSet(true, false)) {
            if (animator != null) {
                animator.shutdownNow();
                animator = null;
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
        if (entries.isEmpty()) return;
        String fullText = buildScrollText();
        if (fullText.isEmpty()) return;
        scrollOffset = (scrollOffset + SCROLL_SPEED_CELLS) % fullText.length();
        invalidate();
    }

    /** Visible for tests: length of the full scroll text. */
    int buildScrollTextLen() {
        return buildScrollText().length();
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

        // Build the full scrollable text and a parallel color map.
        String fullText = buildScrollText();
        if (fullText.isEmpty()) return;

        int totalWidth = fullText.length();
        int cols = size.columns();

        for (int col = 0; col < cols; col++) {
            int idx = (scrollOffset + col) % totalWidth;
            char ch = fullText.charAt(idx);

            // Determine color for this position.
            AnsiColor fg = colorAtPosition(idx);
            SGR[] mods = (fg == AnsiColor.BRIGHT_BLACK) ? new SGR[0] : new SGR[]{SGR.BOLD};

            graphics.setCell(col, 0, new TextCell(ch, fg, AnsiColor.BLACK, mods));
        }
    }

    /**
     * Builds the full scrollable text: entry1 | entry2 | ... | (wrap).
     * The text is padded with a separator at the end so it loops seamlessly.
     */
    private String buildScrollText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(SEPARATOR);
            sb.append(formatEntry(entries.get(i)));
        }
        sb.append(SEPARATOR);  // trailing separator for seamless wrap
        return sb.toString();
    }

    private static String formatEntry(TickerEntry entry) {
        String pct = String.format(java.util.Locale.US, "%+.2f%%", entry.changePercent());
        String price = String.format(java.util.Locale.US, "%.2f", entry.price());
        return entry.symbol() + " " + price + " " + pct;
    }

    /**
     * Returns the foreground color for a character at the given position
     * in the full scroll text. Symbols are white, prices/percentages are
     * green/red, separators are dim.
     */
    private AnsiColor colorAtPosition(int pos) {
        int offset = 0;
        for (int i = 0; i < entries.size(); i++) {
            TickerEntry entry = entries.get(i);
            String entryText = formatEntry(entry);
            int entryLen = entryText.length();

            // Entry region.
            if (pos >= offset && pos < offset + entryLen) {
                int relPos = pos - offset;
                // Symbol is the first entry.symbol().length() chars + 1 space.
                int symLen = entry.symbol().length();
                if (relPos <= symLen) {
                    return AnsiColor.BRIGHT_WHITE;
                }
                // Rest is price + percent — colored by direction.
                return entry.changePercent() >= 0 ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;
            }
            offset += entryLen;

            // Separator region after this entry.
            if (pos >= offset && pos < offset + SEPARATOR.length()) {
                return AnsiColor.BRIGHT_BLACK;
            }
            offset += SEPARATOR.length();
        }
        // Trailing separator.
        return AnsiColor.BRIGHT_BLACK;
    }

    /** Immutable ticker entry: symbol, price, and day change percentage. */
    public record TickerEntry(String symbol, double price, double changePercent) {
        public TickerEntry {
            Objects.requireNonNull(symbol, "symbol");
        }
    }
}