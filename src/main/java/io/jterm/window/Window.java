package io.jterm.window;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.widget.Component;
import io.jterm.widget.Panel;

import java.util.List;

/** Window interface. */
public interface Window {
    /**
     * Returns the window's title, as displayed by the window manager's decoration.
     *
     * @return the current title
     */
    String getTitle();

    /**
     * Sets the window's title, as displayed by the window manager's decoration.
     *
     * @param title the new title
     */
    void setTitle(String title);

    /**
     * Returns the panel holding this window's content.
     *
     * @return the root content panel
     */
    Panel getContents();

    /**
     * Returns the window's current top-left position on screen.
     *
     * @return the current position
     */
    TerminalPosition getPosition();

    /**
     * Returns the window's current size.
     *
     * @return the current size in columns × rows
     */
    TerminalSize getSize();

    /**
     * Replaces the window's hints, which adjust how the window manager
     * positions and decorates it.
     *
     * @param hints the hints to apply
     */
    void setHints(List<WindowHint> hints);

    /**
     * Returns the window's current hints.
     *
     * @return the current hints
     */
    List<WindowHint> getHints();

    /**
     * Sets the window's position and size.
     *
     * @param position new top-left position on screen
     * @param size     new size in columns × rows
     */
    void setBounds(TerminalPosition position, TerminalSize size);

    /**
     * Renders the window (content panel plus decoration) into the graphics context.
     *
     * @param graphics graphics context covering the window's bounds
     */
    void draw(TextGraphics graphics);

    /**
     * Returns the child component that currently holds keyboard focus.
     *
     * @return the focused component, or {@code null} if none
     */
    Component getFocusedComponent();

    /**
     * Gives keyboard focus to a child component.
     *
     * @param component the component to focus
     */
    void setFocusedComponent(Component component);

    /**
     * Returns the window's preferred content size. The default implementation
     * returns the current size; windows that want to be centered or auto-sized
     * should override this to report their natural dimensions.
     *
     * @return the preferred content size
     */
    default TerminalSize getPreferredSize() { return getSize(); }

    /**
     * Returns the interval in milliseconds after which the GUI should repaint
     * this window even when no input arrives, or {@code 0} (the default) to
     * disable auto-refresh.
     *
     * <p>Use for windows that display time-derived state (idle timers, clocks,
     * live counters) so the visible values stay fresh between keystrokes. The
     * repaint goes through the normal diff-based refresh, so an unchanged
     * frame costs only the changed cells. The callback timing is best-effort:
     * the event loop checks the interval on every idle spin, so the actual
     * period is the declared interval plus scheduling slop.</p>
     *
     * @return the auto-refresh interval in milliseconds, or 0 to disable
     */
    default long autoRefreshIntervalMillis() { return 0L; }

    /**
     * Called when a key is pressed and no focused child component consumed it,
     * or when the window has no focusable children. Default implementation does nothing.
     *
     * @param keyStroke the key stroke to handle
     * @return true if the keystroke was consumed, false otherwise
     */
    default boolean handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) { return false; }

    /**
     * Called when the window is added to a GUI (e.g. via ScreenManager.push).
     * Override to set focus, register listeners, or perform initialization.
     * Default implementation does nothing.
     *
     * @param gui the GUI this window was added to
     */
    default void open(DefaultTextGUI gui) {}

    /**
     * Called when the window is being removed from the GUI (user navigates away,
     * disconnects, or the session ends). Override to unregister listeners, release
     * resources, or perform cleanup. Default implementation does nothing.
     */
    default void close() {}
}
