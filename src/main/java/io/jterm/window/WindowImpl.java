package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Panel;

/** Simple window implementation. */
public class WindowImpl extends AbstractWindow {
    /**
     * Create a window with the given title.
     *
     * @param title the window title
     */
    public WindowImpl(String title) { super(title); }
    /**
     * Create a window with an empty title.
     */
    public WindowImpl() { super(); }

    /**
     * Set the window position and size.
     *
     * @param position the terminal position
     * @param size the terminal dimensions
     */
    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
    }
}
