package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;

/**
 * Animated background contract. Implementations render full-frame animation
 * into a backing buffer supplied by the GUI, advance their internal state on
 * a virtual-thread render loop, and re-layout when the terminal resizes.
 */
public interface AnimatedBackground {
    /**
     * Called once per frame. Draw the animated content into the graphics buffer.
     *
     * @param graphics graphics context to render the frame into
     * @param size     current terminal size
     */
    void renderFrame(TextGraphics graphics, TerminalSize size);

    /**
     * Called when the terminal size changes. The background should re-layout.
     *
     * @param newSize the new terminal size
     */
    void onResize(TerminalSize newSize);

    /**
     * Stop the animation loop and clean up.
     */
    void stop();

    /** Start the animation loop. Typically spawns a virtual thread. */
    void start();

    /**
     * Returns true if the animation is currently running.
     *
     * @return true if the animation is currently running
     */
    boolean isRunning();

    /**
     * Target frames per second. Default 15 (BBS terminals are slow, keep CPU low).
     *
     * @return the target frame rate in frames per second
     */
    default int targetFps() { return 15; }

    /**
     * Returns the last terminal size passed to {@link #onResize}, or null if never resized.
     *
     * @return the last known terminal size, or {@code null} if never resized
     */
    default TerminalSize lastSize() { return null; }

    /**
     * Advance internal animation state by one frame. Called before each
     * {@link #renderFrame}. Implementations that derive state from wall-clock
     * time (e.g. TwinkleStarfield) can ignore this; implementations with
     * step-based state (e.g. CircuitBoard, TypewriterEffect) must override.
     *
     * @param nowNanos current time in nanoseconds, from {@link System#nanoTime()}
     */
    default void tick(long nowNanos) {}
}
