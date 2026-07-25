package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;

/**
 * Short-lived screen transition effect (fade, wipe, dissolve, slide, etc.).
 * <p>
 * Unlike {@link AnimatedBackground} which loops forever, a TransitionEffect
 * runs once for {@link #durationMs()} milliseconds and then completes. The
 * caller drives the effect by calling {@link #renderFrame} with a progress
 * value from 0.0 (start) to 1.0 (end).
* <p>
 * The graphics buffer passed to {@link #renderFrame} already contains the NEW
 * screen content (fully drawn). The transition effect modifies cells in-place
 * to blend between old and new content based on the progress value.
 */
public interface TransitionEffect {
    /** Total duration in milliseconds. */
    long durationMs();

    /**
     * Render a single frame of the transition.
     *
     * @param graphics the graphics buffer (already contains NEW screen content)
     * @param size     terminal size
     * @param progress 0.0 (start, old screen visible) to 1.0 (end, new screen visible)
     */
    void renderFrame(TextGraphics graphics, TerminalSize size, double progress);

    /** Default FPS for transitions — 30 (fast, under 1 second). */
    default int targetFps() { return 30; }
}