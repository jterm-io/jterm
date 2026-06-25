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
     * Returns the window's preferred content size. The default implementation
     * returns the current size; windows that want to be centered or auto-sized
     * should override this to report their natural dimensions.
     */
    default TerminalSize getPreferredSize() { return getSize(); }

    /**
     * Called when a key is pressed and no focused child component consumed it,
     * or when the window has no focusable children. Default implementation does nothing.
     *
     * @param keyStroke the key stroke to handle
     */
    default void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {}

    /**
     * Called when the window is added to a GUI (e.g. via ScreenManager.push).
     * Override to set focus, register listeners, or perform initialization.
     * Default implementation does nothing.
     */
    default void open(DefaultTextGUI gui) {}

    /**
     * Called when the window is being removed from the GUI (user navigates away,
     * disconnects, or the session ends). Override to unregister listeners, release
     * resources, or perform cleanup. Default implementation does nothing.
     */
    default void close() {}
}
