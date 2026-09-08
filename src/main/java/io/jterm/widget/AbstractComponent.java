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
 *
 * <p>Constructs with default state: position (0,0), zero size, visible,
 * focusable, and no parent or layout data.
 */
public abstract class AbstractComponent implements Component {

    /**
     * Creates a component with default state (visible, unfocused, focusable,
     * no parent or layout data).
     */
    public AbstractComponent() {}
    private volatile TerminalPosition position = TerminalPosition.TOP_LEFT;
    private volatile TerminalSize size = TerminalSize.ZERO;
    private volatile TerminalSize preferredSize;
    private volatile boolean visible = true;
    private volatile boolean invalid = true;
    private volatile LayoutData layoutData;
    private volatile Component parent;
    private volatile boolean focused;
    private volatile boolean focusable = true;

    /** {@inheritDoc} */
    @Override
    public TerminalSize getSize() { return size; }

    /** {@inheritDoc} */
    @Override
    public TerminalPosition getPosition() { return position; }

    /** {@inheritDoc} */
    @Override
    public TerminalSize getPreferredSize() {
        if (preferredSizeOverride != null) return preferredSizeOverride;
        if (invalid || preferredSize == null) {
            preferredSize = calculatePreferredSize();
            invalid = false;
        }
        return preferredSize;
    }

    /**
     * Override the calculated preferred size. Set to null to revert to auto-calculation.
     *
     * @param size fixed preferred size, or {@code null} to revert to auto-calculation
     */
    public void setPreferredSizeOverride(TerminalSize size) {
        this.preferredSizeOverride = size;
        invalidate();
    }

    private volatile TerminalSize preferredSizeOverride;

    /**
     * Computes the component's natural preferred size.
     *
     * @return the preferred size based on the component's content
     */
    protected abstract TerminalSize calculatePreferredSize();

    /** {@inheritDoc} */
    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        this.position = position;
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override
    public void invalidate() {
        invalid = true;
        preferredSize = null;
        if (parent != null) parent.invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVisible() { return visible; }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public LayoutData getLayoutData() { return layoutData; }

    /** {@inheritDoc} */
    @Override
    public void setLayoutData(LayoutData data) { this.layoutData = data; }

    /** {@inheritDoc} */
    @Override
    public Component getParent() { return parent; }

    /** {@inheritDoc} */
    @Override
    public void setParent(Component parent) { this.parent = parent; }

    /** {@inheritDoc} */
    @Override
    public boolean isFocused() { return focused; }

    /** {@inheritDoc} */
    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() { return focusable; }

    /** {@inheritDoc} */
    @Override
    public void setFocusable(boolean focusable) { this.focusable = focusable; }

    /** {@inheritDoc} */
    @Override
    public boolean handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        // default: not consumed
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void draw(TextGraphics graphics) {
        drawComponent(graphics);
    }

    /**
     * Renders this component into the supplied graphics context.
     *
     * The graphics size matches {@link #getSize()} after layout.
     *
     * @param graphics graphics context sized to this component's bounds
     */
    protected abstract void drawComponent(TextGraphics graphics);
}
