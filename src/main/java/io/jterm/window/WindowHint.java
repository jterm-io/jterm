package io.jterm.window;

/** Window hint flags. */
public enum WindowHint {
    MODAL,
    FULLSCREEN,
    NO_DECORATIONS,
    NO_POST_RENDERING,
    FIT_TERMINAL_WINDOW,
    BACKGROUND,
    /** Skip the opaque background fill in {@link AbstractWindow#draw} so lower
     * windows (e.g. an {@link AnimatedBackgroundWindow}) show through. */
    TRANSPARENT,
    /** Center the window on the terminal based on its preferred size,
     * leaving the window sized to fit its content rather than the full screen. */
    CENTERED
}
