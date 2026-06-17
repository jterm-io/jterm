package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Panel;

/** Simple window implementation. */
public class WindowImpl extends AbstractWindow {
    public WindowImpl(String title) { super(title); }
    public WindowImpl() { super(); }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
    }
}
