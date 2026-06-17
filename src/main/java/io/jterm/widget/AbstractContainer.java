package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;
import io.jterm.layout.LayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Base container implementation with child list and layout delegation.
 *
 * {@link #calculatePreferredSize()} delegates to the layout manager, and
 * {@link #setBounds} triggers re-layout of children.
 */
public abstract class AbstractContainer extends AbstractComponent implements Container {
    private final List<Component> children = new ArrayList<>();
    private LayoutManager layoutManager;

    public AbstractContainer() {}

    public AbstractContainer(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public List<Component> getChildren() { return new ArrayList<>(children); }

    @Override
    public void addComponent(Component component) {
        children.add(component);
        component.setParent(this);
        invalidate();
    }

    @Override
    public void addComponent(Component component, Object layoutData) {
        if (layoutData instanceof LayoutData ld) {
            component.setLayoutData(ld);
        }
        addComponent(component);
    }

    @Override
    public void removeComponent(Component component) {
        if (children.remove(component)) {
            component.setParent(null);
            invalidate();
        }
    }

    @Override
    public LayoutManager getLayoutManager() { return layoutManager; }

    @Override
    public void setLayoutManager(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        invalidate();
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        if (layoutManager == null) return new TerminalSize(1, 1);
        return layoutManager.getPreferredSize(children);
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        if (layoutManager != null) {
            layoutManager.doLayout(size, children);
        }
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        for (var child : children) {
            if (!child.isVisible()) continue;
            var pos = child.getPosition();
            var size = child.getSize();
            if (size.columns() <= 0 || size.rows() <= 0) continue;
            var sub = io.jterm.graphics.TextGraphicsExtensions.subGraphics(graphics, pos, size);
            child.draw(sub);
        }
    }
}
