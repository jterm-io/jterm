package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;

import java.util.List;

/**
 * N×M grid layout with equal cell sizes.
 *
 * <p>If either {@code columns} or {@code rows} is non-positive, the layout auto-sizes
 * that dimension to fit all children in a roughly square arrangement.
 * Any remaining area after integer division is given to the last column/row.</p>
 */
public class GridLayout implements LayoutManager {
    private final int columns;
    private final int rows;

    /**
     * Create a grid layout with the given column and row counts.
     *
     * @param columns number of columns (0 or negative for auto)
     * @param rows    number of rows (0 or negative for auto)
     */
    public GridLayout(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
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
        for (var c : children) {
            var ps = c.getPreferredSize();
            w = Math.max(w, ps.columns());
            h = Math.max(h, ps.rows());
        }
        int[] dims = effectiveDimensions(children.size());
        return new TerminalSize(w * dims[0], h * dims[1]);
    }

    /**
     * Lay out children within the given area.
     *
     * @param area the available area
     * @param children the child components
     */
    @Override
    public void doLayout(TerminalSize area, List<Component> children) {
        int[] dims = effectiveDimensions(children.size());
        int cols = dims[0];
        int rows = dims[1];
        if (cols == 0 || rows == 0) return;

        int baseW = area.columns() / Math.max(1, cols);
        int baseH = area.rows() / Math.max(1, rows);
        int extraW = area.columns() - baseW * cols;
        int extraH = area.rows() - baseH * rows;

        int i = 0;
        int y = 0;
        for (int r = 0; r < rows; r++) {
            int cellH = baseH + (r == rows - 1 ? extraH : 0);
            int x = 0;
            for (int c = 0; c < cols; c++) {
                if (i >= children.size()) return;
                int cellW = baseW + (c == cols - 1 ? extraW : 0);
                children.get(i).setBounds(new TerminalPosition(x, y), new TerminalSize(cellW, cellH));
                x += cellW;
                i++;
            }
            y += cellH;
        }
    }

    private int[] effectiveDimensions(int childCount) {
        int cols = columns;
        int rows = this.rows;
        if (cols > 0 && rows > 0) {
            return new int[]{cols, rows};
        }
        if (cols <= 0 && rows <= 0) {
            cols = Math.max(1, (int) Math.ceil(Math.sqrt(childCount)));
            rows = Math.max(1, (int) Math.ceil((double) childCount / cols));
        } else if (cols <= 0) {
            rows = Math.max(1, rows);
            cols = Math.max(1, (int) Math.ceil((double) childCount / rows));
        } else {
            // rows <= 0 and columns > 0
            cols = Math.max(1, cols);
            rows = Math.max(1, (int) Math.ceil((double) childCount / cols));
        }
        return new int[]{cols, rows};
    }
}
