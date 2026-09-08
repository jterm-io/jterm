package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutManager;

import java.util.List;

/**
 * Container that holds child components and delegates layout to a
 * {@link LayoutManager}.
 */
public interface Container extends Component {
    /**
     * Returns a snapshot of the child list.
     *
     * @return a defensive copy of the children, in insertion order
     */
    List<Component> getChildren();

    /**
     * Adds a child with no layout constraints.
     *
     * @param component the child to add
     */
    void addComponent(Component component);

    /**
     * Adds a child with layout-specific constraints.
     *
     * @param component  the child to add
     * @param layoutData layout-specific constraints (must implement {@link io.jterm.layout.LayoutData} to be honored)
     */
    void addComponent(Component component, Object layoutData);

    /**
     * Removes a child.
     *
     * @param component the child to remove
     */
    void removeComponent(Component component);

    /**
     * Returns the active layout manager.
     *
     * @return the layout manager, or {@code null} if none is set
     */
    LayoutManager getLayoutManager();

    /**
     * Sets the active layout manager.
     *
     * @param layoutManager the layout manager to arrange children with
     */
    void setLayoutManager(LayoutManager layoutManager);
}
