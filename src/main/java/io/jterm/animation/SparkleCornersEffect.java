package io.jterm.animation;

/**
 * Animated border effect that cycles the four corner characters through
 * Unicode block elements, creating a twinkling/sparkling appearance.
 *
 * <p>Each corner is offset by 90 degrees (4 steps in the 16-step cycle)
 * so they sparkle in sequence: TL at phase 0, TR at phase 4,
 * BR at phase 8, BL at phase 12. Edge characters remain unchanged.</p>
 */
public class SparkleCornersEffect implements AnimatedBorderEffect {

    /** The 16-step sparkle character cycle. */
    private static final char[] CYCLE = {
            '\u2598', '\u259D', '\u2596', '\u2597',
            '\u258C', '\u2590', '\u2580', '\u2584',
            '\u2588', '\u2584', '\u2580', '\u2590',
            '\u258C', '\u2597', '\u2596', '\u259D'
    };

    /** Phase offsets for each corner (90 degrees apart in a 16-step cycle). */
    private static final int TL_PHASE = 0;
    private static final int TR_PHASE = 4;
    private static final int BR_PHASE = 8;
    private static final int BL_PHASE = 12;

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
        return "sparkle-corners";
    }
}