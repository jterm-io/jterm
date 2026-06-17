package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/** Horizontal or vertical stacking with configurable alignment. */
public class LinearLayout implements LayoutManager {
    public enum Direction { HORIZONTAL, VERTICAL }
    public enum Alignment { BEGINNING, CENTER, END, FILL }

    private final Direction direction;
    private final int spacing;

    public LinearLayout(Direction direction) { this(direction, 0); }
    public LinearLayout(Direction direction, int spacing) {
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
        if (direction == Direction.HORIZONTAL) w += spacing * Math.max(0, visibleCount - 1);
        else h += spacing * Math.max(0, visibleCount - 1);
        return new TerminalSize(Math.max(w, 1), Math.max(h, 1));
    }

    @Override
    public void doLayout(TerminalSize area, List<Component> children) {
        int totalPref = 0;
        int visibleCount = 0;
        for (var c : children) {
            if (!c.isVisible()) continue;
            totalPref += direction == Direction.HORIZONTAL ? c.getPreferredSize().columns() : c.getPreferredSize().rows();
            visibleCount++;
        }
        int extra = (direction == Direction.HORIZONTAL ? area.columns() : area.rows()) - totalPref - spacing * Math.max(0, visibleCount - 1);
        int pos = 0;
        int index = 0;
        for (var c : children) {
            if (!c.isVisible()) continue;
            var ps = c.getPreferredSize();
            int size = direction == Direction.HORIZONTAL ? ps.columns() : ps.rows();
            int cross = direction == Direction.HORIZONTAL ? ps.rows() : ps.columns();
            int crossPos = 0, crossSize = direction == Direction.HORIZONTAL ? area.rows() : area.columns();
            // Simple alignment: center
            if (crossSize > cross) crossPos = (crossSize - cross) / 2;
            if (direction == Direction.HORIZONTAL) {
                c.setBounds(new TerminalPosition(pos, crossPos), new TerminalSize(size, crossSize));
            } else {
                c.setBounds(new TerminalPosition(crossPos, pos), new TerminalSize(crossSize, size));
            }
            pos += size + spacing;
            index++;
        }
    }
}
