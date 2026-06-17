package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/**
 * Layout engine: sizes and positions child components within a container.
 *
 * Implementations must be stateless with respect to the container; the only
 * mutable side-effect permitted is calling {@link Component#setBounds} on the
 * supplied children.
 */
public interface LayoutManager {
    /**
     * Returns the preferred size of the container for the given children.
     */
    TerminalSize getPreferredSize(List<Component> children);

    /**
     * Positions and sizes the children within the given area.
     */
    void doLayout(TerminalSize area, List<Component> children);
}
