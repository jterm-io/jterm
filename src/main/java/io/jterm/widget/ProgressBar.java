package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.Symbols;

/** Horizontal progress bar. */
public class ProgressBar extends AbstractComponent {
    private int value;
    private int max = 100;
    private boolean showPercentage = true;

    public ProgressBar() {}

    public ProgressBar(int max) { this.max = max; }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(max, value));
        invalidate();
    }

    public int getValue() { return value; }
    public int getMax() { return max; }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(showPercentage ? 20 : 14, 1);
    }

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
        graphics.drawString(0, 0, sb.toString(), new TextCell(' ', AnsiColor.GREEN, AnsiColor.DEFAULT));
    }
}
