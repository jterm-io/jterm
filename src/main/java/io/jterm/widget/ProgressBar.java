package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.Symbols;

/** Horizontal progress bar. */
public class ProgressBar extends AbstractComponent {
    private volatile int value;
    private volatile int max = 100;
    private volatile boolean showPercentage = true;

    /** Creates a progress bar with a default maximum of 100. */
    public ProgressBar() {}

    /**
     * Creates a progress bar with the given maximum value.
     *
     * @param max the maximum value
     */
    public ProgressBar(int max) { this.max = max; }

    /**
     * Sets the current value, clamped to {@code [0, max]}.
     *
     * @param value the new value
     */
    public void setValue(int value) {
        this.value = Math.max(0, Math.min(max, value));
        invalidate();
    }

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    public int getValue() { return value; }

    /**
     * Returns the maximum value.
     *
     * @return the maximum value
     */
    public int getMax() { return max; }

    /**
     * Returns the preferred size: 20 columns when percentage is shown, else 14.
     *
     * @return the preferred terminal size
     */
    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(showPercentage ? 20 : 14, 1);
    }

    /**
     * Renders the bar with filled/light blocks and optional percentage label.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        int barCols = showPercentage ? size.columns() - 6 : size.columns();
        int filled = max == 0 ? 0 : (int) Math.round((double) value / max * barCols);
        var sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < barCols; i++) {
            sb.append(i < filled ? Symbols.BLOCK_SOLID : Symbols.BLOCK_LIGHT);
        }
        sb.append(']');
        if (showPercentage) {
            sb.append(String.format(" %3d%%", max == 0 ? 0 : (int) Math.round((double) value / max * 100)));
        }
        var theme = ThemeManager.active();
        graphics.drawString(0, 0, sb.toString(), new TextCell(' ', theme.accent(), theme.background()));
    }
}
