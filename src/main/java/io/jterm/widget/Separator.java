package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Horizontal or vertical separator. */
public class Separator extends AbstractComponent {
    private final boolean vertical;

    /** Creates a horizontal separator. */
    public Separator() { this(false); }

    /**
     * Creates a separator with the given orientation.
     *
     * @param vertical {@code true} for a vertical divider, {@code false} for horizontal
     */
    public Separator(boolean vertical) { this.vertical = vertical; }

    /** Returns a size of 1×1 — a separator is a thin divider.
     * @return a 1×1 terminal size */
    @Override
    protected TerminalSize calculatePreferredSize() {
        return vertical ? new TerminalSize(1, 1) : new TerminalSize(1, 1);
    }

    /** Draws a horizontal or vertical divider line using box-drawing characters.
     * @param graphics the graphics context to draw with */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        var style = new TextCell(' ', theme.border(), theme.background());
        if (vertical) {
            for (int r = 0; r < size.rows(); r++) {
                graphics.drawString(0, r, "│", style);
            }
        } else {
            graphics.drawString(0, 0, "─".repeat(Math.max(0, size.columns())), style);
        }
    }
}
