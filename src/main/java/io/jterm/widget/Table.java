package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.Symbols;
import io.jterm.util.TerminalTextUtils;
import io.jterm.widget.model.DefaultTableModel;
import io.jterm.widget.model.TableModel;
import io.jterm.widget.model.TableModelEvent;
import io.jterm.widget.model.TableModelEventType;
import io.jterm.widget.model.TableModelListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Column/row table with headers and scrolling. */
public class Table extends AbstractComponent implements TableModelListener {
    private TableModel model;
    private volatile int selectedRow = 0;
    private volatile int scrollOffset = 0;
    private volatile int[] columnWidths;

    /** Per-column alignment array; defaults to {@code LEFT} for all columns. */
    private Alignment[] columnAlignments;

    /** Character drawn between columns. Defaults to {@code "│"}. Use {@code " "} for no visible separator. */
    private String columnSeparatorChar = "│";

    /**
     * Per-column text alignment for table cells.
     * <ul>
     *   <li>{@link #LEFT} — text starts at the left edge of the column (default)</li>
     *   <li>{@link #RIGHT} — text ends at the right edge of the column, padded on the left</li>
     * </ul>
     */
    public enum Alignment {
        /** Left-aligned: text at the left edge, padding on the right. */
        LEFT,
        /** Right-aligned: text at the right edge, padding on the left. */
        RIGHT
    }

    /**
     * Creates a table backed by the provided model.
     *
     * @param model the backing table model
     */
    public Table(TableModel model) {
        setModel(model);
    }

    /**
     * Creates a table with the given headers backed by a {@link DefaultTableModel}.
     *
     * @param headers the column headers
     */
    public Table(String... headers) {
        this(new DefaultTableModel(headers));
    }

    /**
     * Sets the backing model and attaches this table as a listener.
     *
     * @param model the new backing model
     */
    public void setModel(TableModel model) {
        if (this.model != null) {
            this.model.removeTableModelListener(this);
        }
        this.model = model;
        if (model != null) {
            model.addTableModelListener(this);
        }
        selectedRow = 0;
        scrollOffset = 0;
        invalidate();
    }

    /**
     * Returns the backing model.
     *
     * @return the current model
     */
    public TableModel getModel() {
        return model;
    }

    /** {@inheritDoc} — clamps selection/scroll on model changes. */
    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.type() == TableModelEventType.ROWS_REMOVED
                || e.type() == TableModelEventType.ROWS_CHANGED
                || e.type() == TableModelEventType.STRUCTURE_CHANGED) {
            if (model != null && selectedRow >= model.getRowCount()) {
                selectedRow = Math.max(0, model.getRowCount() - 1);
            }
        }
        // On STRUCTURE_CHANGED, clamp selection to valid range (already done
        // above) but do NOT reset to row 0 — that discards the user's selection
        // on every model refresh. Only reset scroll if the selection is now
        // above the viewport.
        if (e.type() == TableModelEventType.STRUCTURE_CHANGED) {
            if (scrollOffset > selectedRow) {
                scrollOffset = selectedRow;
            }
        }
        ensureVisible();
        invalidate();
    }

    /**
     * Convenience: appends a row if the backing model is a {@link DefaultTableModel}.
     *
     * @param cells the row cell values
     * @throws IllegalStateException if the model is not a DefaultTableModel
     */
    public void addRow(String... cells) {
        if (model instanceof DefaultTableModel dtm) {
            dtm.addRow(cells);
        } else {
            throw new IllegalStateException("Table was not created with a DefaultTableModel; add rows via the model");
        }
    }

    /**
     * Removes all rows from the backing model (if it is a {@link DefaultTableModel}).
     *
     * <p>This is the correct way to clear a table's rows. Calling
     * {@code getTableModelRows().clear()} does <strong>not</strong> work because
     * {@link #getTableModelRows()} returns a defensive copy of the row data.</p>
     */
    public void clearRows() {
        if (model instanceof DefaultTableModel dtm) {
            dtm.clear();
        } else {
            throw new IllegalStateException("Table was not created with a DefaultTableModel; clear rows via the model");
        }
    }

    /**
     * Sets the selected row, clamped to the valid range.
     *
     * @param index the desired row index
     */
    public void setSelectedRow(int index) {
        int count = model == null ? 0 : model.getRowCount();
        this.selectedRow = Math.max(0, Math.min(count - 1, index));
        ensureVisible();
        invalidate();
    }

    /** Returns the index of the currently selected row. */
    public int getSelectedRow() {
        return selectedRow;
    }

    /**
     * Computes the preferred size based on column widths and row count.
     *
     * @return the preferred terminal size
     */
    @Override
    protected TerminalSize calculatePreferredSize() {
        int totalWidth = model == null ? 1 : model.getColumnCount() + 1;
        if (model != null) {
            for (int i = 0; i < model.getColumnCount(); i++) {
                int max = TerminalTextUtils.getTrueWidth(model.getColumnName(i));
                int rowCount = model.getRowCount();
                for (int r = 0; r < rowCount; r++) {
                    max = Math.max(max, TerminalTextUtils.getTrueWidth(model.getValueAt(r, i)));
                }
                totalWidth += max;
            }
        }
        int rowCount = model == null ? 0 : model.getRowCount();
        return new TerminalSize(totalWidth, Math.max(3, Math.min(rowCount + 1, 12)));
    }

    /**
     * Renders the header row and the visible data rows with separators.
     *
     * @param graphics the text-graphics target
     */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        updateColumnWidths(size.columns());
        int y = 0;
        int columnCount = model == null ? 0 : model.getColumnCount();
        // Header
        List<String> headerCells = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            headerCells.add(model.getColumnName(i));
        }
        drawRow(graphics, y++, headerCells, new TextCell(' ', theme.headerFg(), theme.headerBg(), SGR.BOLD), true);
        // Rows
        int rowCount = model == null ? 0 : model.getRowCount();
        for (int i = scrollOffset; i < rowCount && y < size.rows(); i++) {
            boolean selected = i == selectedRow;
            var style = (selected && isFocused())
                    ? new TextCell(' ', theme.selectionFg(), theme.selectionBg())
                    : new TextCell(' ', theme.foreground(), theme.background());
            List<String> rowCells = new ArrayList<>();
            for (int c = 0; c < columnCount; c++) {
                rowCells.add(model.getValueAt(i, c));
            }
            drawRow(graphics, y++, rowCells, style, false);
        }
        // Clear remaining rows
        while (y < size.rows()) {
            graphics.fillRectangle(0, y++, size.columns(), 1, new TextCell(' ', theme.foreground(), theme.background()));
        }
    }

    private void updateColumnWidths(int availableWidth) {
        int columnCount = model == null ? 0 : model.getColumnCount();
        if (columnWidths == null || columnWidths.length != columnCount) {
            columnWidths = new int[columnCount];
        }
        int minPerCol = Math.max(3, availableWidth / Math.max(1, columnCount) - 1);
        for (int i = 0; i < columnCount; i++) {
            int max = TerminalTextUtils.getTrueWidth(model.getColumnName(i));
            int rowCount = model.getRowCount();
            for (int r = 0; r < rowCount; r++) {
                max = Math.max(max, TerminalTextUtils.getTrueWidth(model.getValueAt(r, i)));
            }
            columnWidths[i] = Math.max(minPerCol, Math.min(max, availableWidth / Math.max(1, columnCount)));
        }
    }

    private void drawRow(TextGraphics graphics, int y, List<String> cells, TextCell style, boolean isHeader) {
        int x = 0;
        int columnCount = model == null ? 0 : model.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            String text = i < cells.size() ? cells.get(i) : "";
            String truncated = TerminalTextUtils.truncate(text, columnWidths[i]);
            int textWidth = TerminalTextUtils.getTrueWidth(truncated);
            Alignment alignment = getColumnAlignment(i);
            if (alignment == Alignment.RIGHT) {
                int pad = columnWidths[i] - textWidth;
                if (pad > 0) {
                    graphics.fillRectangle(x, y, pad, 1, style);
                }
                graphics.drawString(x + pad, y, truncated, style);
            } else {
                graphics.drawString(x, y, truncated, style);
                int pad = columnWidths[i] - textWidth;
                if (pad > 0) {
                    graphics.fillRectangle(x + textWidth, y, pad, 1, style);
                }
            }
            x += columnWidths[i];
            if (i < columnCount - 1) {
                graphics.drawString(x, y, columnSeparatorChar, style);
                x += TerminalTextUtils.getTrueWidth(columnSeparatorChar);
            }
        }
    }

    /**
     * Handles arrow, page, home/end, and Emacs-style row navigation keys.
     *
     * @param keyStroke the keystroke to handle
     * @return {@code true} if the keystroke was consumed
     */
    @Override
    public boolean handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        // Emacs-style key bindings (Ctrl+letter arrives as CHARACTER with ctrl=true)
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.ctrl()) {
            switch (keyStroke.character()) {
                case 'P', 'p' -> { setSelectedRow(selectedRow - 1); return true; }
                case 'N', 'n' -> { setSelectedRow(selectedRow + 1); return true; }
                case 'V', 'v' -> { pageDown(); return true; }
                default -> { return false; }  // Ignore other Ctrl+letter combos
            }
        }
        switch (keyStroke.type()) {
            case ARROW_UP -> { setSelectedRow(selectedRow - 1); return true; }
            case ARROW_DOWN -> { setSelectedRow(selectedRow + 1); return true; }
            case PAGE_UP -> { pageUp(); return true; }
            case PAGE_DOWN -> { pageDown(); return true; }
            case HOME -> { setSelectedRow(0); return true; }
            case END -> { if (model != null) setSelectedRow(model.getRowCount() - 1); return true; }
            default -> { return false; }
        }
    }

    private void ensureVisible() {
        var visibleRows = Math.max(1, getSize().rows() - 1);
        if (selectedRow < scrollOffset) scrollOffset = selectedRow;
        if (selectedRow >= scrollOffset + visibleRows) scrollOffset = selectedRow - visibleRows + 1;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    /** Scroll down by one viewport height (page down). */
    public void pageDown() {
        int visibleRows = Math.max(1, getSize().rows() - 1);
        int rowCount = model == null ? 0 : model.getRowCount();
        scrollOffset = Math.min(scrollOffset + visibleRows, Math.max(0, rowCount - visibleRows));
        setSelectedRow(scrollOffset);
    }

    /** Scroll up by one viewport height (page up). */
    public void pageUp() {
        int visibleRows = Math.max(1, getSize().rows() - 1);
        scrollOffset = Math.max(0, scrollOffset - visibleRows);
        setSelectedRow(scrollOffset);
    }

    /**
     * Updates bounds and re-ensures the selected row is visible.
     *
     * @param position the new position
     * @param size     the new size
     */
    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }

    /**
     * Sets the alignment for a specific column. Out-of-bounds column indices
     * are silently ignored (no exception thrown).
     *
     * @param col       the column index (0-based)
     * @param alignment the alignment to set; if {@code null}, defaults to {@link Alignment#LEFT}
     */
    public void setColumnAlignment(int col, Alignment alignment) {
        int columnCount = model == null ? 0 : model.getColumnCount();
        if (col < 0 || col >= columnCount) {
            return; // silently ignore out-of-bounds
        }
        if (columnAlignments == null || columnAlignments.length != columnCount) {
            columnAlignments = new Alignment[columnCount];
            Arrays.fill(columnAlignments, Alignment.LEFT);
        }
        columnAlignments[col] = (alignment == null) ? Alignment.LEFT : alignment;
        invalidate();
    }

    /**
     * Returns the alignment for the given column. Returns {@link Alignment#LEFT}
     * for out-of-bounds indices or unset columns.
     *
     * @param col the column index (0-based)
     * @return the alignment, or {@code LEFT} if not set or out of bounds
     */
    public Alignment getColumnAlignment(int col) {
        if (columnAlignments == null || col < 0 || col >= columnAlignments.length) {
            return Alignment.LEFT;
        }
        Alignment a = columnAlignments[col];
        return a == null ? Alignment.LEFT : a;
    }

    /**
     * Returns the computed width of the given column (in terminal columns).
     * This is the actual width used during rendering, which may differ from
     * the preferred width depending on available space.
     *
     * @param col the column index (0-based)
     * @return the column width in terminal columns, or 0 if not yet computed or out of bounds
     */
    public int getColumnWidth(int col) {
        if (columnWidths == null || col < 0 || col >= columnWidths.length) {
            return 0;
        }
        return columnWidths[col];
    }

    /**
     * Sets the character drawn between columns. Defaults to {@code "│"}.
     * Use {@code ":"} for label-value config screens, or {@code " "} for
     * no visible separator.
     *
     * @param separator the separator character to draw between columns
     */
    public void setColumnSeparatorChar(String separator) {
        this.columnSeparatorChar = separator != null ? separator : "│";
        invalidate();
    }

    /**
     * Returns the current column separator character.
     *
     * @return the separator character drawn between columns
     */
    public String getColumnSeparatorChar() {
        return columnSeparatorChar;
    }

    /**
     * Returns a defensive copy of all rows currently in the backing model.
     *
     * @return list of rows; each row is a list of cell strings
     */
    public List<List<String>> getTableModelRows() {
        int rowCount = model == null ? 0 : model.getRowCount();
        int columnCount = model == null ? 0 : model.getColumnCount();
        List<List<String>> result = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            List<String> cells = new ArrayList<>();
            for (int c = 0; c < columnCount; c++) {
                cells.add(model.getValueAt(r, c));
            }
            result.add(cells);
        }
        return result;
    }
}
