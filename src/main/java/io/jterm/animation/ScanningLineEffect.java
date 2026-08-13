package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.style.AnsiColor;
import io.jterm.widget.Border;

/**
 * Animated border effect where a bright character travels around the border
 * perimeter like a scanner, one position per tick.
 *
 * <p>The highlight moves clockwise: top edge left→right, right edge top→bottom,
 * bottom edge right→left, left edge bottom→top. The highlighted cell uses
 * {@link AnsiColor#BRIGHT_WHITE} by default, and the rest of the border
 * stays at the base style.</p>
 *
 * <p>On each frame, the effect calls {@link BorderContext#resetToStyle()} first
 * to clear the previous highlight, then sets the new highlighted position
 * with the highlight character and color.</p>
 */
public class ScanningLineEffect implements AnimatedBorderEffect {

    /** Default highlight character. */
    static final char DEFAULT_HIGHLIGHT_CHAR = '\u25A0'; // ■

    private final char highlightChar;
    private final int speed;

    /** Creates a ScanningLineEffect with default highlight char (■) and speed 1. */
    public ScanningLineEffect() {
        this(DEFAULT_HIGHLIGHT_CHAR);
    }

    /** Creates a ScanningLineEffect with the given highlight character and speed 1.
     *
     * @param highlightChar the character to display at the scanning position
     */
    public ScanningLineEffect(char highlightChar) {
        this(highlightChar, 1);
    }

    /** Creates a ScanningLineEffect with default highlight char and given speed.
     *
     * @param speed cells per frame (must be >= 1)
     */
    public ScanningLineEffect(int speed) {
        this(DEFAULT_HIGHLIGHT_CHAR, speed);
    }

    /** Creates a ScanningLineEffect with the given highlight character and speed.
     *
     * @param highlightChar the character to display at the scanning position
     * @param speed cells per frame (must be >= 1)
     * @throws IllegalArgumentException if highlightChar is null or speed &lt; 1
     */
    public ScanningLineEffect(char highlightChar, int speed) {
        if (speed < 1) throw new IllegalArgumentException("speed must be >= 1");
        this.highlightChar = highlightChar;
        this.speed = speed;
    }

    @Override
    public void update(long frame, BorderContext ctx) {
        ctx.resetToStyle();

        TerminalSize size = ctx.getSize();
        int perimeter = getPerimeterLength(size);
        if (perimeter <= 0) return;

        int position = (int) ((frame * speed) % perimeter);
        setHighlightAtPosition(position, ctx, size);
    }

    @Override
    public String name() {
        return "scanning-line";
    }

    /**
     * Computes the perimeter length for a border of the given size.
     * The perimeter is the total number of border cells:
     * 2 * (cols - 1) + 2 * (rows - 1).
     *
     * @param size the border dimensions
     * @return the number of cells on the border perimeter
     */
    public int getPerimeterLength(TerminalSize size) {
        if (size.columns() < 2 || size.rows() < 2) return 0;
        return 2 * (size.columns() - 1) + 2 * (size.rows() - 1);
    }

    /**
     * Sets the highlight character and color at the given perimeter position.
     *
     * <p>Position order (clockwise from top-left corner):
     * <ul>
     *   <li>0 = TL corner</li>
     *   <li>1 .. (cols-2) = top edge (left to right)</li>
     *   <li>(cols-1) = TR corner</li>
     *   <li>cols .. (cols+rows-3) = right edge (top to bottom)</li>
     *   <li>(cols+rows-2) = BR corner</li>
     *   <li>(cols+rows-1) .. (2*cols+rows-4) = bottom edge (right to left)</li>
     *   <li>(2*cols+rows-3) = BL corner</li>
     *   <li>(2*cols+rows-2) .. (perimeter-1) = left edge (bottom to top)</li>
     * </ul>
     */
    private void setHighlightAtPosition(int position, BorderContext ctx, TerminalSize size) {
        int cols = size.columns();
        int rows = size.rows();

        // Corner positions (clockwise from top-left)
        int tlPos = 0;
        int trPos = cols - 1;
        int brPos = cols - 1 + rows - 1;
        int blPos = 2 * (cols - 1) + rows - 1;

        if (position == tlPos) {
            ctx.setCorner(BorderContext.Corner.TL, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position == trPos) {
            ctx.setCorner(BorderContext.Corner.TR, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position == brPos) {
            ctx.setCorner(BorderContext.Corner.BR, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position == blPos) {
            ctx.setCorner(BorderContext.Corner.BL, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position < trPos) {
            // Top edge (between TL and TR corners, left to right)
            int edgeIndex = position - tlPos - 1;
            ctx.setEdge(BorderContext.Side.TOP, edgeIndex, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position < brPos) {
            // Right edge (between TR and BR corners, top to bottom)
            int edgeIndex = position - trPos - 1;
            ctx.setEdge(BorderContext.Side.RIGHT, edgeIndex, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else if (position < blPos) {
            // Bottom edge (between BR and BL corners, right to left)
            int bottomEdgeCount = cols - 2;
            int edgeIndex = bottomEdgeCount - 1 - (position - brPos - 1);
            ctx.setEdge(BorderContext.Side.BOTTOM, edgeIndex, highlightChar, AnsiColor.BRIGHT_WHITE);
        } else {
            // Left edge (between BL and TL corners, bottom to top)
            int leftEdgeCount = rows - 2;
            int edgeIndex = leftEdgeCount - 1 - (position - blPos - 1);
            ctx.setEdge(BorderContext.Side.LEFT, edgeIndex, highlightChar, AnsiColor.BRIGHT_WHITE);
        }
    }
}