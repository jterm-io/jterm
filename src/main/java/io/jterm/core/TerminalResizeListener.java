package io.jterm.core;

/** Listener invoked when the terminal reports a new size. */
@FunctionalInterface
public interface TerminalResizeListener {
    void onResized(TerminalSize newSize);
}
