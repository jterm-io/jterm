package io.jterm.widget;

import io.jterm.layout.LayoutManager;

/** Generic container with a {@link LayoutManager}. */
public class Panel extends AbstractContainer {

    /** Creates a panel with the default layout. */
    public Panel() {}

    /**
     * Creates a panel with the given layout manager.
     *
     * @param layoutManager the layout manager for this panel's children
     */
    public Panel(LayoutManager layoutManager) { super(layoutManager); }
}