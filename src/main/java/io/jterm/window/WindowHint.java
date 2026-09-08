package io.jterm.window;

/**
 * Window hint flags.
 */
public enum WindowHint {
    /** The window is modal and blocks input to windows below it. */
    MODAL,
    /** The window occupies the entire terminal. */
    FULLSCREEN,
    /** The window is drawn without a border or decorations. */
    NO_DECORATIONS,
    /** The window is not redrawn by the normal render pass. */
    NO_POST_RENDERING,
    /** The window is sized to fit the terminal window. */
    FIT_TERMINAL_WINDOW,
    /** The window provides the background for the whole terminal. */
    BACKGROUND,
    /** Skip the opaque background fill in {@link AbstractWindow#draw} so lower
     * windows (e.g. an {@link AnimatedBackgroundWindow}) show through. */
    TRANSPARENT,
    /** Center the window on the terminal based on its preferred size,
     * leaving the window sized to fit its content rather than the full screen. */
    CENTERED
}
