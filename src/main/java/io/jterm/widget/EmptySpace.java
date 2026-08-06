package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Filler component. */
public class EmptySpace extends AbstractComponent {
    private TerminalSize size;

    /** Creates a new empty space with a default size of 1×1. */
    public EmptySpace() { this(new TerminalSize(1, 1)); }

    /** Creates a new empty space with the specified size.
     * @param size the preferred size of this filler component */
    public EmptySpace(TerminalSize size) { this.size = size; }

    /** Returns the preferred size of this empty space.
     * @return the configured size */
    @Override
    protected TerminalSize calculatePreferredSize() { return size; }

    /** Fills the component area with background color from the active theme.
     * @param graphics the graphics context to draw with */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        graphics.fillRectangle(0, 0, getSize().columns(), getSize().rows(), new TextCell(' ', theme.foreground(), theme.background()));
    }
}
