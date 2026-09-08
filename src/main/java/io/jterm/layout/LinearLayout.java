package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/**
 * Horizontal or vertical stacking layout.
 *
 * Components are arranged along the primary axis (horizontal = columns, vertical = rows).
 * The cross-axis size of each child equals the container's cross-axis size unless the child
 * is smaller and a non-FILL alignment is used. Extra space on the primary axis is distributed
 * to children whose layout data indicates {@link GrowPolicy#CAN_GROW}. Spacing is inserted
 * between adjacent visible children only.
 */
public class LinearLayout implements LayoutManager {
    /** Stacking direction of the layout's primary axis. */
    public enum Direction {
        /** Children are arranged left to right in columns. */
        HORIZONTAL,
        /** Children are arranged top to bottom in rows. */
        VERTICAL
    }

    /** Cross-axis alignment of children that are smaller than the container. */
    public enum Alignment {
        /** Align at the start of the cross axis. */
        BEGINNING,
        /** Center on the cross axis. */
        CENTER,
        /** Align at the end of the cross axis. */
        END,
        /** Stretch the child to fill the cross axis. */
        FILL
    }

    /** Whether a child may absorb extra primary-axis space. */
    public enum GrowPolicy {
        /** The child keeps its preferred size. */
        NONE,
        /** The child may grow to absorb extra primary-axis space. */
        CAN_GROW
    }

    /**
     * Layout data for components in a linear layout.
     * Specifies alignment and grow flags.
     */
    public static class LinearLayoutData implements LayoutData {
        private final Alignment alignment;
        private final GrowPolicy growPolicy;

        /**
         * Create layout data with default center alignment and no growth.
         */
        public LinearLayoutData() { this(Alignment.CENTER, GrowPolicy.NONE); }
        /**
         * Create layout data with the given alignment and no growth.
         *
         * @param alignment the alignment within the container
         */
        public LinearLayoutData(Alignment alignment) { this(alignment, GrowPolicy.NONE); }
        /**
         * Create layout data with center alignment and the given growth policy.
         *
         * @param growPolicy the growth policy
         */
        public LinearLayoutData(GrowPolicy growPolicy) { this(Alignment.CENTER, growPolicy); }
        /**
         * Create layout data with the given alignment and growth policy.
         *
         * @param alignment  the alignment within the container
         * @param growPolicy the growth policy
         */
        public LinearLayoutData(Alignment alignment, GrowPolicy growPolicy) {
            this.alignment = alignment;
            this.growPolicy = growPolicy;
        }

        /**
         * Return the alignment for this layout data.
         *
         * @return the alignment
         */
        public Alignment alignment() { return alignment; }
        /**
         * Return the growth policy for this layout data.
         *
         * @return the growth policy
         */
        public GrowPolicy growPolicy() { return growPolicy; }
    }

    private final Direction direction;
    private final int spacing;

    /**
     * Create a linear layout with the given direction and no spacing.
     *
     * @param direction the stacking direction
     */
    public LinearLayout(Direction direction) { this(direction, 0); }
    /**
     * Create a linear layout with the given direction and inter-child spacing.
     *
     * @param direction the stacking direction
     * @param spacing   gap between children (must be &gt;= 0)
     */
    public LinearLayout(Direction direction, int spacing) {
        if (spacing < 0) throw new IllegalArgumentException("spacing must be >= 0");
        this.direction = direction;
        this.spacing = spacing;
    }

    /**
     * Compute the preferred size for the given children.
     *
     * @param children the child components
     *
     * @return the computed preferred size
     */
    @Override
    public TerminalSize getPreferredSize(List<Component> children) {
        int w = 0, h = 0;
        int visibleCount = 0;
        for (var c : children) {
            if (!c.isVisible()) continue;
            var ps = c.getPreferredSize();
            if (direction == Direction.HORIZONTAL) {
                w += ps.columns();
                h = Math.max(h, ps.rows());
            } else {
                w = Math.max(w, ps.columns());
                h += ps.rows();
            }
            visibleCount++;
        }
        if (direction == Direction.HORIZONTAL) {
            w += spacing * Math.max(0, visibleCount - 1);
        } else {
            h += spacing * Math.max(0, visibleCount - 1);
        }
        return new TerminalSize(Math.max(w, 1), Math.max(h, 1));
    }

    /**
     * Lay out children within the given area.
     *
     * @param area the available area
     * @param children the child components
     */
    @Override
    public void doLayout(TerminalSize area, List<Component> children) {
        int totalPref = 0;
        int visibleCount = 0;
        int growableCount = 0;
        for (var c : children) {
            if (!c.isVisible()) continue;
            totalPref += primarySize(c.getPreferredSize());
            if (isGrowable(c)) growableCount++;
            visibleCount++;
        }

        int availablePrimary = (direction == Direction.HORIZONTAL ? area.columns() : area.rows())
            - spacing * Math.max(0, visibleCount - 1);
        int extra = availablePrimary - totalPref;

        int growPerChild = growableCount > 0 && extra > 0 ? extra / growableCount : 0;
        int growRemainder = growableCount > 0 && extra > 0 ? extra % growableCount : 0;

        int pos = 0;
        int growIndex = 0;
        for (var c : children) {
            if (!c.isVisible()) continue;
            var ps = c.getPreferredSize();
            int primary = primarySize(ps);
            int cross = crossSize(ps);
            int crossAvailable = direction == Direction.HORIZONTAL ? area.rows() : area.columns();

            if (isGrowable(c) && extra > 0) {
                primary += growPerChild;
                if (growIndex < growRemainder) primary += 1;
                growIndex++;
            }

            int crossPos = crossAlignmentOffset(cross, crossAvailable, alignmentOf(c));
            int crossSize = alignmentOf(c) == Alignment.FILL ? crossAvailable : cross;

            if (direction == Direction.HORIZONTAL) {
                c.setBounds(new TerminalPosition(pos, crossPos), new TerminalSize(primary, crossSize));
            } else {
                c.setBounds(new TerminalPosition(crossPos, pos), new TerminalSize(crossSize, primary));
            }
            pos += primary + spacing;
        }
    }

    private int primarySize(TerminalSize size) {
        return direction == Direction.HORIZONTAL ? size.columns() : size.rows();
    }

    private int crossSize(TerminalSize size) {
        return direction == Direction.HORIZONTAL ? size.rows() : size.columns();
    }

    private boolean isGrowable(Component c) {
        var data = c.getLayoutData();
        return data instanceof LinearLayoutData ld && ld.growPolicy() == GrowPolicy.CAN_GROW;
    }

    private Alignment alignmentOf(Component c) {
        var data = c.getLayoutData();
        if (data instanceof LinearLayoutData ld) return ld.alignment();
        return Alignment.CENTER;
    }

    private int crossAlignmentOffset(int childCross, int availableCross, Alignment alignment) {
        if (availableCross <= childCross || alignment == Alignment.FILL || alignment == Alignment.BEGINNING) return 0;
        if (alignment == Alignment.END) return availableCross - childCross;
        return (availableCross - childCross) / 2;
    }
}
