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
    public enum Direction { HORIZONTAL, VERTICAL }
    public enum Alignment { BEGINNING, CENTER, END, FILL }
    public enum GrowPolicy { NONE, CAN_GROW }

    public static class LinearLayoutData implements LayoutData {
        private final Alignment alignment;
        private final GrowPolicy growPolicy;

        public LinearLayoutData() { this(Alignment.CENTER, GrowPolicy.NONE); }
        public LinearLayoutData(Alignment alignment) { this(alignment, GrowPolicy.NONE); }
        public LinearLayoutData(GrowPolicy growPolicy) { this(Alignment.CENTER, growPolicy); }
        public LinearLayoutData(Alignment alignment, GrowPolicy growPolicy) {
            this.alignment = alignment;
            this.growPolicy = growPolicy;
        }

        public Alignment alignment() { return alignment; }
        public GrowPolicy growPolicy() { return growPolicy; }
    }

    private final Direction direction;
    private final int spacing;

    public LinearLayout(Direction direction) { this(direction, 0); }
    public LinearLayout(Direction direction, int spacing) {
        if (spacing < 0) throw new IllegalArgumentException("spacing must be >= 0");
        this.direction = direction;
        this.spacing = spacing;
    }

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
