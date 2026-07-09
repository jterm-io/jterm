package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.CellStyle;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import io.jterm.util.TerminalTextUtils;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;
import io.jterm.widget.model.GridListener;
import io.jterm.widget.model.GridModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A generic tabular data widget with a frozen header row, per-column alignment,
 * per-column format strings, per-cell styling, and vertical/horizontal scrolling.
 *
 * @param <T> the row type
 */
public class DataGrid<T> extends AbstractComponent implements GridListener {

    private GridModel<T> model;
    private List<GridColumn<T>> columns;
    private volatile int selectedRow = 0;
    private volatile int scrollOffsetY = 0;
    private volatile int scrollOffsetX = 0;
    private volatile int[] columnWidths;

    private final List<Runnable> selectionListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a DataGrid with the given columns and an empty {@link DefaultGridModel}.
     *
     * @param columns the column definitions
     */
    public DataGrid(List<GridColumn<T>> columns) {
        this(columns, new DefaultGridModel<>());
    }

    /**
     * Creates a DataGrid with the given columns and model.
     *
     * @param columns the column definitions
     * @param model   the backing row model
     */
    public DataGrid(List<GridColumn<T>> columns, GridModel<T> model) {
        this.columns = columns;
        setModel(model);
    }

    // ---- Model / Columns ---------------------------------------------------

    /**
     * Replaces the backing model. Unregisters from the old model, registers
     * with the new, resets selection/scroll, and invalidates.
     */
    public void setModel(GridModel<T> model) {
        if (this.model != null) {
            this.model.removeGridListener(this);
        }
        this.model = model;
        if (model != null) {
            model.addGridListener(this);
        }
        selectedRow = 0;
        scrollOffsetY = 0;
        scrollOffsetX = 0;
        invalidate();
    }

    /** Sets the column definitions and invalidates. */
    public void setColumns(List<GridColumn<T>> columns) {
        this.columns = columns;
        invalidate();
    }

    /** Returns the current model. */
    public GridModel<T> getModel() {
        return model;
    }

    /** Returns the column definitions. */
    public List<GridColumn<T>> getColumns() {
        return columns;
    }

    // ---- Selection --------------------------------------------------------

    /** Returns the currently selected row index. */
    public int getSelectedRow() {
        return selectedRow;
    }

    /** Sets the selected row, clamping to valid range, then ensures visibility. */
    public void setSelectedRow(int index) {
        int count = model == null ? 0 : model.getRowCount();
        int old = selectedRow;
        if (count == 0) {
            selectedRow = 0;
        } else {
            selectedRow = Math.max(0, Math.min(count - 1, index));
        }
        ensureVisible();
        if (old != selectedRow) {
            fireSelectionChanged();
        }
        invalidate();
    }

    /** Returns the row at the selected index, or null if the model is empty. */
    public T getSelectedItem() {
        if (model == null || model.getRowCount() == 0) return null;
        if (selectedRow < 0 || selectedRow >= model.getRowCount()) return null;
        return model.getRow(selectedRow);
    }

    /** Adds a selection listener that fires when the selected row changes. */
    public void addSelectionListener(Runnable listener) {
        selectionListeners.add(listener);
    }

    private void fireSelectionChanged() {
        for (var l : selectionListeners) l.run();
    }

    // ---- GridListener -----------------------------------------------------

    @Override
    public void gridChanged() {
        int count = model == null ? 0 : model.getRowCount();
        if (selectedRow >= count) {
            selectedRow = Math.max(0, count - 1);
        }
        ensureVisible();
        invalidate();
    }

    // ---- Rendering --------------------------------------------------------

    @Override
    protected TerminalSize calculatePreferredSize() {
        int colCount = columns == null ? 0 : columns.size();
        if (colCount == 0) {
            return new TerminalSize(1, 3);
        }

        // Compute header-based widths
        int totalWidth = 0;
        int[] widths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            GridColumn<T> col = columns.get(c);
            int w = TerminalTextUtils.getTrueWidth(col.header());
            if (col.minWidth() > 0) w = Math.max(w, col.minWidth());
            if (col.maxWidth() > 0) w = Math.min(w, col.maxWidth());
            widths[c] = w;
            totalWidth += w;
        }
        totalWidth += Math.max(0, colCount - 1); // separators

        int rowCount = model == null ? 0 : model.getRowCount();
        int height = Math.min(rowCount + 1, 12);
        height = Math.max(3, height);

        return new TerminalSize(Math.max(1, totalWidth), height);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        int colCount = columns == null ? 0 : columns.size();
        int rowCount = model == null ? 0 : model.getRowCount();
        int viewportRows = size.rows();
        int viewportCols = size.columns();

        if (colCount == 0 || viewportRows == 0 || viewportCols == 0) {
            // Nothing to draw — clear with background
            graphics.fillRectangle(0, 0, viewportCols, viewportRows,
                    new TextCell(' ', theme.foreground(), theme.background()));
            return;
        }

        // 1. Compute column widths based on visible rows + headers
        computeColumnWidths(size, theme);

        // Determine which columns to render (horizontal scroll)
        int startCol = Math.max(0, scrollOffsetX);
        int availableWidth = viewportCols;
        int endCol = startCol;
        int usedWidth = 0;
        for (int c = startCol; c < colCount; c++) {
            int w = columnWidths[c];
            int sep = (c > startCol) ? 1 : 0;
            if (usedWidth + sep + w > availableWidth && c > startCol) break;
            usedWidth += sep + w;
            endCol = c + 1;
        }

        // 2. Draw header row at y=0 (frozen)
        drawHeaderRow(graphics, size, theme, startCol, endCol);

        // 3. Draw data rows
        int y = 1;
        int visibleRows = Math.max(1, viewportRows - 1);
        int lastRow = Math.min(rowCount, scrollOffsetY + visibleRows);
        for (int r = scrollOffsetY; r < lastRow && y < viewportRows; r++) {
            boolean selected = (r == selectedRow);
            T row = model.getRow(r);
            drawDataRow(graphics, size, theme, row, r, selected, startCol, endCol, y);
            y++;
        }

        // 4. Clear remaining rows with background
        while (y < viewportRows) {
            graphics.fillRectangle(0, y, viewportCols, 1,
                    new TextCell(' ', theme.foreground(), theme.background()));
            y++;
        }
    }

    private void computeColumnWidths(TerminalSize size, Theme theme) {
        int colCount = columns.size();
        if (columnWidths == null || columnWidths.length != colCount) {
            columnWidths = new int[colCount];
        }
        int rowCount = model == null ? 0 : model.getRowCount();
        int viewportRows = size.rows();
        int visibleRows = Math.max(1, viewportRows - 1);
        int lastRow = Math.min(rowCount, scrollOffsetY + visibleRows);

        for (int c = 0; c < colCount; c++) {
            GridColumn<T> col = columns.get(c);
            int max = TerminalTextUtils.getTrueWidth(col.header());

            for (int r = scrollOffsetY; r < lastRow; r++) {
                T row = model.getRow(r);
                Object raw = col.accessor().apply(row);
                String formatted = formatValue(col, raw);
                max = Math.max(max, TerminalTextUtils.getTrueWidth(formatted));
            }

            // Respect minWidth
            if (col.minWidth() > 0) {
                max = Math.max(max, col.minWidth());
            }
            // Respect maxWidth
            if (col.maxWidth() > 0) {
                max = Math.min(max, col.maxWidth());
            }
            columnWidths[c] = max;
        }
    }

    private void drawHeaderRow(TextGraphics graphics, TerminalSize size, Theme theme,
                                int startCol, int endCol) {
        int y = 0;
        int x = 0;
        TextCell headerStyle = new TextCell(' ', theme.headerFg(), theme.headerBg(), SGR.BOLD);

        for (int c = startCol; c < endCol; c++) {
            GridColumn<T> col = columns.get(c);
            int w = columnWidths[c];

            // Draw separator before column (except first visible)
            if (c > startCol) {
                graphics.drawString(x, y, "│", headerStyle);
                x++;
            }

            String header = col.header();
            GridColumn.Alignment resolved = col.resolveAlignment();
            String aligned = alignText(header, w, resolved);
            graphics.drawString(x, y, aligned, headerStyle);
            x += w;
        }

        // Fill remaining header line with header background
        if (x < size.columns()) {
            graphics.fillRectangle(x, 0, size.columns() - x, 1, headerStyle);
        }
    }

    private void drawDataRow(TextGraphics graphics, TerminalSize size, Theme theme,
                              T row, int rowIndex, boolean selected,
                              int startCol, int endCol, int y) {
        int x = 0;

        for (int c = startCol; c < endCol; c++) {
            GridColumn<T> col = columns.get(c);
            int w = columnWidths[c];

            // Draw separator before column (except first visible)
            if (c > startCol) {
                var sepStyle = selected
                        ? new TextCell(' ', theme.selectionFg(), theme.selectionBg())
                        : new TextCell(' ', theme.foreground(), theme.background());
                graphics.drawString(x, y, "│", sepStyle);
                x++;
            }

            // Get raw value and format
            Object rawValue = col.accessor().apply(row);
            String formatted = formatValue(col, rawValue);

            // Resolve alignment
            GridColumn.Alignment resolved = col.resolveAlignment();
            String aligned = alignText(formatted, w, resolved);

            // Determine cell colors
            Color fg;
            Color bg;
            if (selected) {
                fg = theme.selectionFg();
                bg = theme.selectionBg();
            } else if (col.styler() != null) {
                CellStyle style = col.styler().apply(rawValue);
                if (style != null) {
                    fg = style.fg() != null ? style.fg() : theme.foreground();
                    bg = style.bg() != null ? style.bg() : theme.background();
                } else {
                    fg = theme.foreground();
                    bg = theme.background();
                }
            } else {
                fg = theme.foreground();
                bg = theme.background();
            }

            TextCell cellStyle = new TextCell(' ', fg, bg);
            graphics.drawString(x, y, aligned, cellStyle);
            x += w;
        }

        // Fill remaining line with background
        if (x < size.columns()) {
            var fillStyle = selected
                    ? new TextCell(' ', theme.selectionFg(), theme.selectionBg())
                    : new TextCell(' ', theme.foreground(), theme.background());
            graphics.fillRectangle(x, y, size.columns() - x, 1, fillStyle);
        }
    }

    // ---- Helpers ----------------------------------------------------------

    /**
     * Formats a raw cell value using the column's format string.
     * Null → empty string; format != null → {@link String#format}; else → {@link String#valueOf}.
     */
    private String formatValue(GridColumn<T> col, Object value) {
        if (value == null) return "";
        if (col.format() != null) {
            return String.format(col.format(), value);
        }
        return String.valueOf(value);
    }

    /**
     * Pads or truncates the text to exactly {@code width} display columns based
     * on the alignment.
     *
     * @param text     the input text
     * @param width    the target width in terminal columns
     * @param alignment LEFT, CENTER, or RIGHT
     * @return the aligned text, exactly {@code width} columns wide
     */
    String alignText(String text, int width, GridColumn.Alignment alignment) {
        if (width <= 0) return "";
        String truncated = TerminalTextUtils.truncate(text, width);
        int textWidth = TerminalTextUtils.getTrueWidth(truncated);
        int pad = width - textWidth;
        if (pad <= 0) return truncated;

        return switch (alignment) {
            case LEFT -> truncated + " ".repeat(pad);
            case RIGHT -> " ".repeat(pad) + truncated;
            case CENTER -> {
                int left = pad / 2;
                int right = pad - left;
                yield " ".repeat(left) + truncated + " ".repeat(right);
            }
            case AUTO -> truncated + " ".repeat(pad); // AUTO resolves to LEFT
        };
    }

    // ---- Scrolling --------------------------------------------------------

    /**
     * Ensures the selected row is visible within the viewport, adjusting
     * the vertical scroll offset as needed.
     */
    void ensureVisible() {
        var size = getSize();
        int visibleRows = Math.max(1, size.rows() - 1);
        if (selectedRow < scrollOffsetY) {
            scrollOffsetY = selectedRow;
        }
        if (selectedRow >= scrollOffsetY + visibleRows) {
            scrollOffsetY = selectedRow - visibleRows + 1;
        }
        if (scrollOffsetY < 0) {
            scrollOffsetY = 0;
        }
    }

    // ---- Key handling -----------------------------------------------------

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        // Emacs-style key bindings (Ctrl+letter arrives as CHARACTER with ctrl=true)
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.ctrl()) {
            switch (keyStroke.character()) {
                case 'P', 'p' -> { setSelectedRow(selectedRow - 1); return; }
                case 'N', 'n' -> { setSelectedRow(selectedRow + 1); return; }
                case 'V', 'v' -> { pageDown(); return; }
                default -> { return; }  // Ignore other Ctrl+letter combos
            }
        }
        switch (keyStroke.type()) {
            case ARROW_UP -> setSelectedRow(selectedRow - 1);
            case ARROW_DOWN -> setSelectedRow(selectedRow + 1);
            case ARROW_LEFT, ARROW_RIGHT -> {
                if (keyStroke.type() == KeyType.ARROW_LEFT) scrollLeft();
                else scrollRight();
            }
            case PAGE_UP -> pageUp();
            case PAGE_DOWN -> pageDown();
            case HOME -> setSelectedRow(0);
            case END -> {
                if (model != null) setSelectedRow(model.getRowCount() - 1);
            }
            case ENTER -> fireSelectionChanged();
            default -> {}
        }
    }

    // ---- Page up/down -----------------------------------------------------

    /** Scroll down by one viewport height (page down). */
    public void pageDown() {
        var size = getSize();
        int visibleRows = Math.max(1, size.rows() - 1);
        int rowCount = model == null ? 0 : model.getRowCount();
        scrollOffsetY = Math.min(scrollOffsetY + visibleRows, Math.max(0, rowCount - visibleRows));
        setSelectedRow(scrollOffsetY);
    }

    /** Scroll up by one viewport height (page up). */
    public void pageUp() {
        var size = getSize();
        int visibleRows = Math.max(1, size.rows() - 1);
        scrollOffsetY = Math.max(0, scrollOffsetY - visibleRows);
        setSelectedRow(scrollOffsetY);
    }

    // ---- Horizontal scrolling --------------------------------------------

    /** Returns the current horizontal scroll offset. */
    public int getScrollOffsetX() {
        return scrollOffsetX;
    }

    /** Returns the current vertical scroll offset. */
    public int getScrollOffsetY() {
        return scrollOffsetY;
    }

    /** Scrolls the grid one column left, clamped at 0. */
    public void scrollLeft() {
        scrollOffsetX = Math.max(0, scrollOffsetX - 1);
        invalidate();
    }

    /** Scrolls the grid one column right, clamped to max horizontal offset. */
    public void scrollRight() {
        scrollOffsetX = Math.min(maxScrollOffsetX(), scrollOffsetX + 1);
        invalidate();
    }

    /** Maximum horizontal scroll offset (total column width − viewport width). */
    private int maxScrollOffsetX() {
        int colCount = columns == null ? 0 : columns.size();
        if (colCount == 0) return 0;
        int totalWidth = totalColumnWidth();
        int viewportWidth = getSize().columns();
        return Math.max(0, totalWidth - viewportWidth);
    }

    /**
     * Total width of all columns including separators (columnCount − 1).
     */
    private int totalColumnWidth() {
        int colCount = columns == null ? 0 : columns.size();
        if (colCount == 0) return 0;
        // columnWidths is populated during draw; fall back to header-based widths
        int total = 0;
        for (int c = 0; c < colCount; c++) {
            int w;
            if (columnWidths != null && c < columnWidths.length) {
                w = columnWidths[c];
            } else {
                GridColumn<T> col = columns.get(c);
                w = TerminalTextUtils.getTrueWidth(col.header());
                if (col.minWidth() > 0) w = Math.max(w, col.minWidth());
                if (col.maxWidth() > 0) w = Math.min(w, col.maxWidth());
            }
            total += w;
        }
        total += Math.max(0, colCount - 1); // separators
        return total;
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }
}