package io.jterm.animation;

/**
 * Strategy interface for animated border effects.
 *
 * <p>Each animation frame, the {@link io.jterm.widget.AnimatedBorder} timer calls
 * {@link #update(long, BorderContext)} so the effect can modify
 * individual corner and edge characters before the border is redrawn.</p>
 */
public interface AnimatedBorderEffect {

    /**
     * Called on each animation frame.
     *
     * @param frame the monotonically increasing frame counter (starts at 0)
     * @param ctx   mutable context providing access to border characters
     */
    void update(long frame, BorderContext ctx);

    /**
     * Returns a human-readable name for this effect (used in logging/debugging).
     *
     * @return the effect name
     */
    String name();
}