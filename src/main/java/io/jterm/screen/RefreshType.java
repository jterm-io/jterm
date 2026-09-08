package io.jterm.screen;

/** Screen refresh strategy. */
public enum RefreshType {
    /** Repaint only cells that changed since the last refresh. */
    DELTA,

    /** Repaint every cell of the screen. */
    COMPLETE,

    /** Let the screen decide: delta refresh normally, complete after a resize. */
    AUTOMATIC
}