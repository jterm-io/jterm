package io.jterm.animation;

/**
 * Animated border effect that rotates the four corner characters through
 * a dash sequence, creating a spinning frame illusion.
 *
 * <p>The corner characters cycle through: ┌ → ╱ → ┐ → ╲ → ┘ → ╱ → └ → ╲ → repeat.
 * Each corner is offset by 90 degrees (2 steps in the 8-step cycle)
 * so they appear to spin in sequence: TL at phase 0, TR at phase 2,
 * BR at phase 4, BL at phase 6. Edge characters remain unchanged.</p>
 */
public class RotatingDashCornersEffect implements AnimatedBorderEffect {

    /** The 8-step rotating dash character cycle. */
    private static final char[] CYCLE = {
            '\u250C', '\u2571', '\u2510', '\u2572',
            '\u2518', '\u2571', '\u2514', '\u2572'
    };

    /** Phase offsets for each corner (90 degrees apart in an 8-step cycle). */
    private static final int TL_PHASE = 0;
    private static final int TR_PHASE = 2;
    private static final int BR_PHASE = 4;
    private static final int BL_PHASE = 6;

    @Override
    public void update(long frame, BorderContext ctx) {
        int f = (int) (frame % CYCLE.length);
        ctx.setCorner(BorderContext.Corner.TL, CYCLE[(TL_PHASE + f) % CYCLE.length]);
        ctx.setCorner(BorderContext.Corner.TR, CYCLE[(TR_PHASE + f) % CYCLE.length]);
        ctx.setCorner(BorderContext.Corner.BR, CYCLE[(BR_PHASE + f) % CYCLE.length]);
        ctx.setCorner(BorderContext.Corner.BL, CYCLE[(BL_PHASE + f) % CYCLE.length]);
    }

    @Override
    public String name() {
        return "rotating-dash-corners";
    }
}