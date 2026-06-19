package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;

import java.util.EnumSet;

/**
 * Base implementation of {@link Component}.
 *
 * Tracks bounds, parent, layout data, visibility, and focus state. Subclasses
 * provide preferred size calculation and rendering via
 * {@link #calculatePreferredSize()} and {@link #drawComponent(TextGraphics)}.
 */
public abstract class AbstractComponent implements Component {
    private volatile TerminalPosition position = TerminalPosition.TOP_LEFT;
    private volatile TerminalSize size = TerminalSize.ZERO;
    private volatile TerminalSize preferredSize;
    private volatile boolean visible = true;
    private volatile boolean invalid = true;
    private volatile LayoutData layoutData;
    private volatile Component parent;
    private volatile boolean focused;

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

    /**
     * Computes the component's natural preferred size.
     */
    protected abstract TerminalSize calculatePreferredSize();

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        this.position = position;
        this.size = size;
    }

    @Override
    public void invalidate() {
        invalid = true;
        preferredSize = null;
        if (parent != null) parent.invalidate();
    }

    @Override
    public boolean isVisible() { return visible; }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        invalidate();
    }

    @Override
    public LayoutData getLayoutData() { return layoutData; }

    @Override
    public void setLayoutData(LayoutData data) { this.layoutData = data; }

    @Override
    public Component getParent() { return parent; }

    @Override
    public TerminalPosition toGlobal() {
        int col = position.column();
        int row = position.row();
        Component p = parent;
        while (p != null) {
            col += p.getPosition().column();
            row += p.getPosition().row();
            p = p.getParent();
        }
        return new TerminalPosition(col, row);
    }

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

    /**
     * Renders this component into the supplied graphics context.
     *
     * The graphics size matches {@link #getSize()} after layout.
     */
    protected abstract void drawComponent(TextGraphics graphics);
}
