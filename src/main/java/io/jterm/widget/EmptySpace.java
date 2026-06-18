package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Filler component. */
public class EmptySpace extends AbstractComponent {
    private TerminalSize size;

    public EmptySpace() { this(new TerminalSize(1, 1)); }
    public EmptySpace(TerminalSize size) { this.size = size; }

    @Override
    protected TerminalSize calculatePreferredSize() { return size; }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var theme = ThemeManager.active();
        graphics.fillRectangle(0, 0, getSize().columns(), getSize().rows(), new TextCell(' ', theme.foreground(), theme.background()));
    }
}
