package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.Symbols;
import io.jterm.util.TerminalTextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Column/row table with headers and scrolling. */
public class Table extends AbstractComponent {
    private final List<String> headers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<List<String>> rows = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile int selectedRow = 0;
    private volatile int scrollOffset = 0;
    private volatile int[] columnWidths;

    public Table(String... headers) {
        this.headers.addAll(Arrays.asList(headers));
    }

    public void addRow(String... cells) {
        rows.add(new ArrayList<>(Arrays.asList(cells)));
        invalidate();
    }

    public void setSelectedRow(int index) {
        this.selectedRow = Math.max(0, Math.min(rows.size() - 1, index));
        ensureVisible();
        invalidate();
    }

    public int getSelectedRow() { return selectedRow; }

    @Override
    protected TerminalSize calculatePreferredSize() {
        int totalWidth = headers.size() + 1;
        for (var h : headers) totalWidth += TerminalTextUtils.getTrueWidth(h);
        return new TerminalSize(totalWidth, Math.max(3, Math.min(rows.size() + 1, 12)));
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        updateColumnWidths(size.columns());
        int y = 0;
        // Header
        drawRow(graphics, y++, headers, new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLUE, SGR.BOLD), true);
        // Rows
        for (int i = scrollOffset; i < rows.size() && y < size.rows(); i++) {
            boolean selected = i == selectedRow;
            var style = selected
                ? new TextCell(' ', AnsiColor.BLACK, AnsiColor.WHITE)
                : new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
            drawRow(graphics, y++, rows.get(i), style, false);
        }
        // Clear remaining rows
        while (y < size.rows()) {
            graphics.fillRectangle(0, y++, size.columns(), 1, new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        }
    }

    private void updateColumnWidths(int availableWidth) {
        if (columnWidths == null || columnWidths.length != headers.size()) {
            columnWidths = new int[headers.size()];
        }
        int minPerCol = Math.max(3, availableWidth / Math.max(1, headers.size()) - 1);
        for (int i = 0; i < headers.size(); i++) {
            int max = TerminalTextUtils.getTrueWidth(headers.get(i));
            for (var row : rows) {
                if (i < row.size()) max = Math.max(max, TerminalTextUtils.getTrueWidth(row.get(i)));
            }
            columnWidths[i] = Math.max(minPerCol, Math.min(max, availableWidth / headers.size()));
        }
    }

    private void drawRow(TextGraphics graphics, int y, List<String> cells, TextCell style, boolean isHeader) {
        int x = 0;
        for (int i = 0; i < headers.size(); i++) {
            String text = i < cells.size() ? cells.get(i) : "";
            String truncated = TerminalTextUtils.truncate(text, columnWidths[i]);
            graphics.drawString(x, y, truncated, style);
            int pad = columnWidths[i] - TerminalTextUtils.getTrueWidth(truncated);
            if (pad > 0) graphics.fillRectangle(x + TerminalTextUtils.getTrueWidth(truncated), y, pad, 1, style);
            x += columnWidths[i];
            if (i < headers.size() - 1) {
                graphics.drawString(x, y, "│", style);
                x++;
            }
        }
    }

    @Override
    public void handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) {
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
    public void setBounds(io.jterm.core.TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        ensureVisible();
    }
}
