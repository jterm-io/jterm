package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;

/** Base implementation of Component with bounds, parent, renderer, and invalidation. */
public abstract class AbstractComponent implements Component {
    private TerminalPosition position = TerminalPosition.TOP_LEFT;
    private TerminalSize size = TerminalSize.ZERO;
    private TerminalSize preferredSize;
    private boolean visible = true;
    private boolean invalid = true;
    private LayoutData layoutData;
    private Component parent;
    private boolean focused;

    @Override
    public TerminalSize getSize() { return size; }

    @Override
    public TerminalPosition getPosition() { return position; }

    @Override
    public TerminalSize getPreferredSize() {
        if (invalid || preferredSize == null) {
            preferredSize = calculatePreferredSize();
            invalid = false;
        }
        return preferredSize;
    }

    protected abstract TerminalSize calculatePreferredSize();

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        this.position = position;
        this.size = size;
        invalidate();
    }

    @Override
    public void invalidate() {
        invalid = true;
        preferredSize = null;
    }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void setVisible(boolean visible) { this.visible = visible; }

    @Override
    public LayoutData getLayoutData() { return layoutData; }

    @Override
    public void setLayoutData(LayoutData data) { this.layoutData = data; }

    @Override
    public Component getParent() { return parent; }

    @Override
    public void setParent(Component parent) { this.parent = parent; }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        // default: ignore
    }

    @Override
    public void draw(TextGraphics graphics) {
        drawComponent(graphics);
    }

    protected abstract void drawComponent(TextGraphics graphics);
}
