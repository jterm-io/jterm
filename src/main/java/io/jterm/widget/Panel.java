package io.jterm.widget;

import io.jterm.layout.LayoutManager;

/**
 * Generic container with a {@link LayoutManager}.
 */
public class Panel extends AbstractContainer {
    public Panel() {}
    public Panel(LayoutManager layoutManager) { super(layoutManager); }
}
