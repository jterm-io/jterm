package io.jterm.core;

/** Listener invoked when the terminal reports a new size. */
@FunctionalInterface
public interface TerminalResizeListener {
    /**
     * Called after the terminal has been resized.
     *
     * @param newSize the new terminal size
     */
    void onResized(TerminalSize newSize);
}
