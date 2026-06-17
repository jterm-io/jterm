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
    /** Returns a snapshot of the child list. */
    List<Component> getChildren();

    /** Adds a child with no layout constraints. */
    void addComponent(Component component);

    /** Adds a child with layout-specific constraints. */
    void addComponent(Component component, Object layoutData);

    /** Removes a child. */
    void removeComponent(Component component);

    /** Returns the active layout manager. */
    LayoutManager getLayoutManager();

    /** Sets the active layout manager. */
    void setLayoutManager(LayoutManager layoutManager);
}
