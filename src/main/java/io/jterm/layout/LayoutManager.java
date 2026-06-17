package io.jterm.layout;

import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/** Layout engine: sizes and positions child components within a container. */
public interface LayoutManager {
    TerminalSize getPreferredSize(List<Component> children);
    void doLayout(TerminalSize area, List<Component> children);
}
