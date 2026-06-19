package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.widget.Component;
import io.jterm.widget.Panel;

import java.util.List;

/** Window interface. */
public interface Window {
    String getTitle();
    void setTitle(String title);
    Panel getContents();
    TerminalPosition getPosition();
    TerminalSize getSize();
    void setHints(List<WindowHint> hints);
    List<WindowHint> getHints();
    void setBounds(TerminalPosition position, TerminalSize size);
    void draw(TextGraphics graphics);
    Component getFocusedComponent();
    void setFocusedComponent(Component component);

    /**
     * Called when a key is pressed and no focused child component consumed it,
     * or when the window has no focusable children. Default implementation does nothing.
     *
     * @param keyStroke the key stroke to handle
     */
    default void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {}
}
