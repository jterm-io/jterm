package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutManager;
import io.jterm.screen.ScreenBuffer;

/** Generic container with a LayoutManager. */
public class Panel extends AbstractContainer {
    public Panel() {}
    public Panel(LayoutManager layoutManager) { super(layoutManager); }
}
