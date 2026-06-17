package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/** N×M grid layout with equal cell sizes. */
public class GridLayout implements LayoutManager {
    private final int columns;
    private final int rows;

    public GridLayout(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    @Override
    public TerminalSize getPreferredSize(List<Component> children) {
        int w = 0, h = 0;
        for (var c : children) {
            var ps = c.getPreferredSize();
            w = Math.max(w, ps.columns());
            h = Math.max(h, ps.rows());
        }
        return new TerminalSize(w * columns, h * rows);
    }

    @Override
    public void doLayout(TerminalSize area, List<Component> children) {
        int cellW = area.columns() / Math.max(1, columns);
        int cellH = area.rows() / Math.max(1, rows);
        int i = 0;
        for (var c : children) {
            if (i >= columns * rows) break;
            int col = i % columns;
            int row = i / columns;
            c.setBounds(new TerminalPosition(col * cellW, row * cellH), new TerminalSize(cellW, cellH));
            i++;
        }
    }
}
