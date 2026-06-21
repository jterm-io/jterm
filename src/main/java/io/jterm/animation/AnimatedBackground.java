package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;

/**
 * Animated background contract. Implementations render full-frame animation
 * into a backing buffer supplied by the GUI, advance their internal state on
 * a virtual-thread render loop, and re-layout when the terminal resizes.
 */
public interface AnimatedBackground {
    /** Called once per frame. Draw the animated content into the graphics buffer. */
    void renderFrame(TextGraphics graphics, TerminalSize size);

    /** Called when the terminal size changes. The background should re-layout. */
    void onResize(TerminalSize newSize);

    /** Start the animation loop. Typically spawns a virtual thread. */
    void start();

    /** Stop the animation loop and clean up. */
    void stop();

    /** Returns true if the animation is currently running. */
    boolean isRunning();

    /** Target frames per second. Default 15 (BBS terminals are slow, keep CPU low). */
    default int targetFps() { return 15; }

    /** Returns the last terminal size passed to {@link #onResize}, or null if never resized. */
    default TerminalSize lastSize() { return null; }
}
