package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.widget.Component;
import io.jterm.widget.Panel;

import java.util.ArrayList;
import java.util.List;

/** Base window with content panel, title bar, and decorations. */
public class AbstractWindow implements Window {
    private volatile String title;
    private final Panel contents;
    private volatile TerminalPosition position = TerminalPosition.TOP_LEFT;
    private volatile TerminalSize size = new TerminalSize(40, 20);
    private final List<WindowHint> hints = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile Component focusedComponent;
    private volatile int titleBarHeight = 1;

    public AbstractWindow(String title) {
        this.title = title;
        this.contents = new Panel();
    }

    public AbstractWindow() { this(""); }

    @Override
    public String getTitle() { return title; }

    @Override
    public void setTitle(String title) { this.title = title; }

    @Override
    public Panel getContents() { return contents; }

    @Override
    public TerminalPosition getPosition() { return position; }

    @Override
    public TerminalSize getSize() { return size; }

    @Override
    public void setHints(List<WindowHint> hints) {
        this.hints.clear();
        this.hints.addAll(hints);
    }

    @Override
    public List<WindowHint> getHints() { return new ArrayList<>(hints); }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        this.position = position;
        this.size = size;
        if (hints.contains(WindowHint.NO_DECORATIONS)) {
            contents.setBounds(position, size);
        } else {
            titleBarHeight = (title == null || title.isEmpty()) ? 1 : 1;
            contents.setBounds(new TerminalPosition(position.column() + 1, position.row() + titleBarHeight),
                new TerminalSize(size.columns() - 2, size.rows() - 1 - titleBarHeight));
        }
    }

    @Override
    public void draw(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        // Fill the entire window area with background so lower windows don't bleed through
        // — unless the window opted into TRANSPARENT, in which case we skip the fill
        // and let whatever was drawn by lower-z-order windows (e.g. AnimatedBackgroundWindow)
        // show through the empty cells.
        if (!hints.contains(WindowHint.TRANSPARENT)) {
            var bgCell = new TextCell(' ', theme.foreground(), theme.background());
            graphics.fillRectangle(0, 0, size.columns(), size.rows(), bgCell);
        }

        if (!hints.contains(WindowHint.NO_DECORATIONS)) {
            drawDecorations(graphics);
            var pos = contents.getPosition();
            var sz = contents.getSize();
            // contents.getPosition() is absolute, but the graphics context is
            // already offset to the window's position by the caller. Use the
            // relative offset so content lands in the right place for centered
            // windows (not just fullscreen at (0,0)).
            var relPos = new TerminalPosition(
                    pos.column() - getPosition().column(),
                    pos.row() - getPosition().row());
            var sub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(graphics, relPos, sz);
            contents.draw(sub);
        } else {
            contents.draw(graphics);
        }
    }

    protected void drawDecorations(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        var borderCell = new TextCell(' ', theme.border(), theme.background());
        // Top bar with title
        var titleStyle = new TextCell(' ', theme.titleFg(), theme.titleBg(), SGR.BOLD);
        graphics.fillRectangle(0, 0, size.columns(), 1, titleStyle);
        if (title != null && !title.isEmpty()) {
            graphics.drawString(2, 0, " " + title + " ", titleStyle);
        }
        // Sides
        for (int r = 1; r < size.rows(); r++) {
            graphics.drawString(0, r, "│", borderCell);
            graphics.drawString(size.columns() - 1, r, "│", borderCell);
        }
        // Bottom
        graphics.drawString(0, size.rows() - 1, "└" + "─".repeat(Math.max(0, size.columns() - 2)) + "┘", borderCell);
    }

    @Override
    public Component getFocusedComponent() { return focusedComponent; }

    @Override
    public void setFocusedComponent(Component component) {
        if (focusedComponent != null) focusedComponent.setFocused(false);
        this.focusedComponent = component;
        if (focusedComponent != null) focusedComponent.setFocused(true);
    }
}
