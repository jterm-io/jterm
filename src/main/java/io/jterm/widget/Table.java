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

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.type() == TableModelEventType.ROWS_REMOVED
                || e.type() == TableModelEventType.ROWS_CHANGED
                || e.type() == TableModelEventType.STRUCTURE_CHANGED) {
            if (model != null && selectedRow >= model.getRowCount()) {
                selectedRow = Math.max(0, model.getRowCount() - 1);
            }
        }
        if (e.type() == TableModelEventType.STRUCTURE_CHANGED) {
            selectedRow = 0;
            scrollOffset = 0;
        }
        ensureVisible();
        invalidate();
    }

    public void addRow(String... cells) {
        if (model instanceof DefaultTableModel dtm) {
            dtm.addRow(cells);
        } else {
            throw new IllegalStateException("Table was not created with a DefaultTableModel; add rows via the model");
        }
    }

    public void setSelectedRow(int index) {
        int count = model == null ? 0 : model.getRowCount();
        this.selectedRow = Math.max(0, Math.min(count - 1, index));
        ensureVisible();
        invalidate();
    }

    public int getSelectedRow() {
        return selectedRow;
    }

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
            var style = selected
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
            graphics.drawString(x, y, truncated, style);
            int pad = columnWidths[i] - TerminalTextUtils.getTrueWidth(truncated);
            if (pad > 0) {
                graphics.fillRectangle(x + TerminalTextUtils.getTrueWidth(truncated), y, pad, 1, style);
            }
            x += columnWidths[i];
            if (i < columnCount - 1) {
                graphics.drawString(x, y, "│", style);
                x++;
            }
        }
    }

    @Override
    public void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
        // Emacs-style key bindings (Ctrl+letter arrives as CHARACTER with ctrl=true)
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.ctrl()) {
            switch (keyStroke.character()) {
                case 'P', 'p' -> { setSelectedRow(selectedRow - 1); return; }
                case 'N', 'n' -> { setSelectedRow(selectedRow + 1); return; }
                default -> { return; }  // Ignore other Ctrl+letter combos
            }
        }
        switch (keyStroke.type()) {
            case ARROW_UP -> setSelectedRow(selectedRow - 1);
            case ARROW_DOWN -> setSelectedRow(selectedRow + 1);
            default -> {}
        }
    }

    private void ensureVisible() {
        var visibleRows = Math.max(1, getSize().rows() - 1);
        if (selectedRow < scrollOffset) scrollOffset = selectedRow;
        if (selectedRow >= scrollOffset + visibleRows) scrollOffset = selectedRow - visibleRows + 1;
        if (scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    public void setBounds(TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }

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
