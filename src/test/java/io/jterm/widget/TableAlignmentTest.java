package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.model.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for per-column alignment support in {@link Table}.
 * Verifies right-alignment, default left-alignment, header alignment,
 * out-of-bounds safety, and separator visibility toggling.
 */
class TableAlignmentTest {

    /**
     * Creates a 2-column table with known content, draws it to a buffer,
     * and returns the buffer for cell inspection.
     *
     * @param width  the terminal width
     * @param col0   the content for column 0
     * @param col1   the content for column 1
     * @return the screen buffer after drawing
     */
    private ScreenBuffer drawTwoColumnTable(int width, String col0, String col1) {
        var model = new DefaultTableModel("H0", "H1");
        model.addRow(col0, col1);
        var table = new Table(model);
        var buffer = new ScreenBuffer(new TerminalSize(width, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(width, 5));
        table.draw(new TextGraphics(buffer));
        return buffer;
    }

    @Test
    void rightAlignedColumnPadsLeftWithSpaces() {
        // Column 0 set to RIGHT; short text "AB" should appear at the right edge
        // of the column, with spaces on the left.
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.RIGHT);
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        // Row 1 is the data row (row 0 is header).
        // Find where "AB" starts in row 1 — it should NOT be at x=0.
        // The text should be at the right edge of column 0.
        int col0Width = table.getColumnWidth(0);
        // "AB" has true width 2, so with right alignment it starts at col0Width - 2.
        int expectedStart = col0Width - 2;
        assertEquals('A', buffer.getCell(expectedStart, 1).character().charAt(0),
                "Right-aligned 'AB' should start at column-width - 2 = " + expectedStart);
        assertEquals('B', buffer.getCell(expectedStart + 1, 1).character().charAt(0));
        // The cell before the text should be a space (padding)
        if (expectedStart > 0) {
            assertEquals(' ', buffer.getCell(0, 1).character().charAt(0),
                    "Left padding should be a space");
        }
    }

    @Test
    void leftAlignedColumnIsDefaultAndPadsRight() {
        // Without setting alignment, text should be left-aligned (existing behavior).
        var buffer = drawTwoColumnTable(30, "AB", "CD");
        // "AB" should be at x=0 in the data row (row 1)
        assertEquals('A', buffer.getCell(0, 1).character().charAt(0));
        assertEquals('B', buffer.getCell(1, 1).character().charAt(0));
    }

    @Test
    void explicitlyLeftAlignedColumnMatchesDefault() {
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.LEFT);
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));
        // Same as default — text at x=0
        assertEquals('A', buffer.getCell(0, 1).character().charAt(0));
    }

    @Test
    void rightAlignedColumnDoesNotAffectOtherColumns() {
        // Column 0 is RIGHT, column 1 should still be LEFT (default).
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.RIGHT);
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        // Column 1 starts after column 0 width + separator (│ = 1 char)
        int col0Width = table.getColumnWidth(0);
        int col1Start = col0Width + 1; // +1 for │ separator
        assertEquals('C', buffer.getCell(col1Start, 1).character().charAt(0),
                "Column 1 should be left-aligned at position " + col1Start);
        assertEquals('D', buffer.getCell(col1Start + 1, 1).character().charAt(0));
    }

    @Test
    void headerRowIsAlsoRightAligned() {
        // When column alignment is set, the header row should also respect it.
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.RIGHT);
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        // Header is row 0. "H0" should be right-aligned in column 0.
        int col0Width = table.getColumnWidth(0);
        int expectedStart = col0Width - 2; // "H0" width = 2
        assertEquals('H', buffer.getCell(expectedStart, 0).character().charAt(0),
                "Header 'H0' should be right-aligned at position " + expectedStart);
        assertEquals('0', buffer.getCell(expectedStart + 1, 0).character().charAt(0));
    }

    @Test
    void getColumnAlignmentReturnsSetAlignment() {
        var table = new Table("A", "B");
        table.setColumnAlignment(1, Table.Alignment.RIGHT);
        assertEquals(Table.Alignment.RIGHT, table.getColumnAlignment(1));
        assertEquals(Table.Alignment.LEFT, table.getColumnAlignment(0),
                "Unset column should default to LEFT");
    }

    @Test
    void getColumnAlignmentReturnsLeftForUnsetColumn() {
        var table = new Table("A", "B");
        assertEquals(Table.Alignment.LEFT, table.getColumnAlignment(0));
        assertEquals(Table.Alignment.LEFT, table.getColumnAlignment(1));
    }

    @Test
    void setColumnAlignmentOutOfBoundsDoesNotCrash() {
        var table = new Table("A", "B");
        // Negative column index — should not throw
        assertDoesNotThrow(() -> table.setColumnAlignment(-1, Table.Alignment.RIGHT));
        // Column index beyond column count — should not throw
        assertDoesNotThrow(() -> table.setColumnAlignment(99, Table.Alignment.RIGHT));
    }

    @Test
    void getColumnAlignmentOutOfBoundsReturnsLeft() {
        var table = new Table("A", "B");
        assertEquals(Table.Alignment.LEFT, table.getColumnAlignment(-1));
        assertEquals(Table.Alignment.LEFT, table.getColumnAlignment(99));
    }

    @Test
    void rightAlignmentWorksWithSeparatorColumn() {
        // Verify that the │ separator is still drawn between columns when
        // one column is right-aligned.
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.RIGHT);
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        // The separator should be at position col0Width
        int col0Width = table.getColumnWidth(0);
        assertEquals('│', buffer.getCell(col0Width, 1).character().charAt(0),
                "Separator │ should be drawn at column 0 width position");
    }

    // ── Separator character tests ───────────────────────────────────────

    @Test
    void setColumnSeparatorCharReplacesDefaultPipe() {
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnSeparatorChar(":");
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        int col0Width = table.getColumnWidth(0);
        assertEquals(':', buffer.getCell(col0Width, 1).character().charAt(0),
                "Separator should be ':' when setColumnSeparatorChar(\":\") is called");
    }

    @Test
    void setColumnSeparatorCharSpaceRemovesVisibleSeparator() {
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnSeparatorChar(" ");
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        int col0Width = table.getColumnWidth(0);
        assertNotEquals('│', buffer.getCell(col0Width, 1).character().charAt(0),
                "Separator should not be '│' when separator char is a space");
    }

    @Test
    void columnSeparatorCharDefaultIsPipe() {
        var table = new Table("A", "B");
        assertEquals("│", table.getColumnSeparatorChar(),
                "Default column separator should be '│'");
    }

    @Test
    void columnSeparatorCharCanBeChangedAndReset() {
        var table = new Table("A", "B");
        table.setColumnSeparatorChar(":");
        assertEquals(":", table.getColumnSeparatorChar());
        table.setColumnSeparatorChar("│");
        assertEquals("│", table.getColumnSeparatorChar());
    }

    @Test
    void customSeparatorWithRightAlignmentPositionsSecondColumnCorrectly() {
        // Right-align column 0, use ":" separator. Column 1 text should start
        // right after column 0 width + 1 (colon separator = 1 char).
        var model = new DefaultTableModel("H0", "H1");
        model.addRow("AB", "CD");
        var table = new Table(model);
        table.setColumnAlignment(0, Table.Alignment.RIGHT);
        table.setColumnSeparatorChar(":");
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        table.draw(new TextGraphics(buffer));

        int col0Width = table.getColumnWidth(0);
        // With ":" separator, column 1 starts at col0Width + 1.
        int col1Start = col0Width + 1;
        assertEquals('C', buffer.getCell(col1Start, 1).character().charAt(0),
                "Column 1 text should start after the ':' separator");
    }
}