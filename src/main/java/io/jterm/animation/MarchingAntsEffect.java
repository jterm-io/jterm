package io.jterm.animation;

/**
 * Animated border effect that produces a "marching ants" selection-box animation.
 *
 * <p>Dashes scroll around the border perimeter: top and bottom edges move right,
 * left and right edges move down. Horizontal edges use '-' for dashes and '·'
 * (middle dot) for gaps; vertical edges use '|' for dashes and '·' for gaps.
 * Corners are left unchanged from the base border style.</p>
 *
 * <p>The dash and gap lengths are configurable via the constructor; defaults are
 * 3 dashes and 2 gaps (matching the classic Photoshop selection marquee).</p>
 */
public class MarchingAntsEffect implements AnimatedBorderEffect {

    private static final String H_DASH = "-";
    private static final String V_DASH = "|";
    private static final String GAP = "·";

    private final int dashLength;
    private final int gapLength;
    private final int cycleLength;

    /** Creates a MarchingAntsEffect with default dash=3, gap=2. */
    public MarchingAntsEffect() {
        this(3, 2);
    }

    /** Creates a MarchingAntsEffect with the given dash and gap lengths. */
    public MarchingAntsEffect(int dashLength, int gapLength) {
        if (dashLength < 1) throw new IllegalArgumentException("dashLength must be >= 1");
        if (gapLength < 1) throw new IllegalArgumentException("gapLength must be >= 1");
        this.dashLength = dashLength;
        this.gapLength = gapLength;
        this.cycleLength = dashLength + gapLength;
    }

    @Override
    public void update(long frame, BorderContext ctx) {
        int offset = (int) (frame % cycleLength);
        int width = ctx.getSize().columns();
        int height = ctx.getSize().rows();

        // Top edge: positions 1..width-2
        for (int c = 1; c < width - 1; c++) {
            ctx.setEdge(BorderContext.Side.TOP, c - 1, isDash(c - 1, offset) ? H_DASH : GAP);
        }

        // Bottom edge: positions 1..width-2, same pattern
        for (int c = 1; c < width - 1; c++) {
            ctx.setEdge(BorderContext.Side.BOTTOM, c - 1, isDash(c - 1, offset) ? H_DASH : GAP);
        }

        // Left edge: positions 1..height-2
        for (int r = 1; r < height - 1; r++) {
            ctx.setEdge(BorderContext.Side.LEFT, r - 1, isDash(r - 1, offset) ? V_DASH : GAP);
        }

        // Right edge: positions 1..height-2, same pattern
        for (int r = 1; r < height - 1; r++) {
            ctx.setEdge(BorderContext.Side.RIGHT, r - 1, isDash(r - 1, offset) ? V_DASH : GAP);
        }
    }

    /**
     * Returns true if the given position (0-based, within the edge) should show
     * a dash character at the given frame offset.
     */
    private boolean isDash(int position, int offset) {
        int posInCycle = (position - offset) % cycleLength;
        if (posInCycle < 0) posInCycle += cycleLength;
        return posInCycle < dashLength;
    }

    @Override
    public String name() {
        return "marching-ants";
    }
}