package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5 edge-case tests for {@link DataGrid}.
 *
 * <p>Each test exercises an unusual or boundary condition and verifies that
 * the widget renders without throwing and produces sane output.
 */
class DataGridEdgeCaseTest {

    /** Simple record used as the row type. */
    record TestRow(String name, int age, double score) {}

    /** Record that can return nulls from every accessor. */
    record NullableRow(String name, Integer age, Double score) {}

    private Theme savedTheme;

    @BeforeEach
    void setUp() {
        savedTheme = ThemeManager.active();
        ThemeManager.setActive(Theme.DARK);
    }

    @AfterEach
    void tearDown() {
        ThemeManager.setActive(savedTheme);
    }

    // ---- Helpers -----------------------------------------------------------

    private DefaultGridModel<TestRow> modelWithRows(TestRow... rows) {
        var model = new DefaultGridModel<TestRow>();
        model.addRows(List.of(rows));
        return model;
    }

    private DataGrid<TestRow> gridWithRows(int width, int height, TestRow... rows) {
        var columns = List.of(
                GridColumn.text("Name", TestRow::name),
                GridColumn.intCol("Age", TestRow::age),
                GridColumn.doubleCol("Score", "%.2f", TestRow::score)
        );
        var grid = new DataGrid<>(columns, modelWithRows(rows));
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(width, height));
        return grid;
    }

    private ScreenBuffer drawGrid(DataGrid<?> grid, int width, int height) {
        var buffer = new ScreenBuffer(new TerminalSize(width, height));
        grid.draw(new TextGraphics(buffer));
        return buffer;
    }

    private String rowText(ScreenBuffer buf, int y, int width) {
        var sb = new StringBuilder();
        for (int x = 0; x < width; x++) {
            sb.append(buf.getCell(x, y).character());
        }
        return sb.toString();
    }

    // ---- 1. Empty model (0 rows) ------------------------------------------

    @Test
    void emptyModelRendersHeaderOnly() {
        var grid = gridWithRows(30, 5); // 0 rows
        var buf = assertDoesNotThrow(() -> drawGrid(grid, 30, 5));
        String header = rowText(buf, 0, 30);
        assertTrue(header.contains("Name"), "Header should be present with 0 rows: " + header);
        // Data rows (y>=1) should be blank (spaces), not contain any row data
        String row1 = rowText(buf, 1, 30);
        assertTrue(row1.isBlank(), "No data rows should render with 0 rows: " + row1);
    }

    // ---- 2. Empty model (0 columns) ---------------------------------------

    @Test
    void emptyColumnsRendersWithoutException() {
        var columns = List.<GridColumn<TestRow>>of();
        var model = new DefaultGridModel<TestRow>();
        model.addRow(new TestRow("A", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertDoesNotThrow(() -> drawGrid(grid, 10, 5));
    }

    // ---- 3. Single row ----------------------------------------------------

    @Test
    void singleRowRendersHeaderAndOneDataRow() {
        var grid = gridWithRows(30, 5, new TestRow("Solo", 42, 7.5));
        var buf = drawGrid(grid, 30, 5);
        String header = rowText(buf, 0, 30);
        assertTrue(header.contains("Name") && header.contains("Age"));
        String row1 = rowText(buf, 1, 30);
        assertTrue(row1.contains("Solo"), "Single data row should render: " + row1);
        // row 2 should be blank (no second data row)
        String row2 = rowText(buf, 2, 30);
        assertTrue(row2.isBlank(), "Second row should be blank with only 1 data row: " + row2);
    }

    // ---- 4. Very wide content truncation ----------------------------------

    @Test
    void veryWideContentTruncatedNoOverflow() {
        // Column with maxWidth(5) forces truncation of a very long name
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.<TestRow>text("N", r -> r.name()).withMaxWidth(5),
                GridColumn.<TestRow>text("Age", r -> String.valueOf(r.age()))
        );
        var model = new DefaultGridModel<TestRow>();
        model.addRow(new TestRow("ThisIsAVeryLongNameThatExceedsColumnWidth", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        var buf = drawGrid(grid, 20, 5);
        String row1 = rowText(buf, 1, 20);
        // The long name should be truncated to 5 chars, no overflow into the Age column
        assertTrue(row1.contains("Age") || row1.contains("1"),
                "Age column should still be visible after truncation: " + row1);
        assertFalse(row1.contains("VeryLongName"),
                "Truncated column should not leak full content into adjacent columns: " + row1);
    }

    // ---- 5. Very narrow viewport (1 column wide) --------------------------

    @Test
    void veryNarrowViewportRendersWithoutException() {
        var grid = gridWithRows(1, 5, new TestRow("Alice", 30, 95.5));
        assertDoesNotThrow(() -> drawGrid(grid, 1, 5));
    }

    // ---- 6. Null cell value ------------------------------------------------

    @Test
    void nullCellValueRendersAsEmptyString() {
        var columns = List.<GridColumn<NullableRow>>of(
                GridColumn.text("Name", NullableRow::name),
                GridColumn.intCol("Age", NullableRow::age)
        );
        var model = new DefaultGridModel<NullableRow>();
        model.addRow(new NullableRow("Test", null, null));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        var buf = drawGrid(grid, 20, 5);
        String row1 = rowText(buf, 1, 20);
        assertFalse(row1.contains("null"),
                "Null cell should render as empty string, not 'null': " + row1);
    }

    // ---- 7. All null cells ------------------------------------------------

    @Test
    void allNullCellsRenderEmpty() {
        var columns = List.<GridColumn<NullableRow>>of(
                GridColumn.text("Name", NullableRow::name),
                GridColumn.intCol("Age", NullableRow::age),
                GridColumn.doubleCol("Score", "%.2f", NullableRow::score)
        );
        var model = new DefaultGridModel<NullableRow>();
        model.addRow(new NullableRow(null, null, null));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        var buf = assertDoesNotThrow(() -> drawGrid(grid, 30, 5));
        String row1 = rowText(buf, 1, 30);
        assertFalse(row1.contains("null"),
                "No cell should contain the literal 'null': " + row1);
    }

    // ---- 8. Model swap at runtime -----------------------------------------

    @Test
    void modelSwapAtRuntimeResetsSelectionAndRendersNewData() {
        var grid = gridWithRows(30, 5,
                new TestRow("A", 1, 1.0),
                new TestRow("B", 2, 2.0),
                new TestRow("C", 3, 3.0));
        grid.setSelectedRow(2);
        assertEquals(2, grid.getSelectedRow());

        var newModel = new DefaultGridModel<TestRow>();
        newModel.addRow(new TestRow("X", 10, 100.0));
        newModel.addRow(new TestRow("Y", 20, 200.0));
        grid.setModel(newModel);

        assertEquals(0, grid.getSelectedRow(), "Selection should reset to 0 on model swap");
        var buf = drawGrid(grid, 30, 5);
        String row1 = rowText(buf, 1, 30);
        assertTrue(row1.contains("X"), "New data should render after model swap: " + row1);
        assertEquals(2, newModel.getRowCount());
    }

    // ---- 9. Column with maxWidth ------------------------------------------

    @Test
    void columnWithMaxWidthTruncates() {
        var columns = List.of(
                GridColumn.<TestRow>text("H", r -> r.name()).withMaxWidth(5)
        );
        var model = new DefaultGridModel<TestRow>();
        model.addRow(new TestRow("VeryLongContentHere", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buf = drawGrid(grid, 10, 5);
        String row1 = rowText(buf, 1, 10);
        // Content should be truncated to 5 chars
        assertTrue(row1.startsWith("VeryL"),
                "Long content should be truncated to 5 chars (maxWidth): " + row1);
        assertFalse(row1.contains("ContentHere"),
                "Content beyond maxWidth should not appear: " + row1);
    }

    // ---- 10. Column with minWidth -----------------------------------------

    @Test
    void columnWithMinWidthExpands() {
        // Column with minWidth(10) should expand short content to 10 display cols
        var columns = List.of(
                GridColumn.<TestRow>text("H", r -> r.name()).withMinWidth(10)
        );
        var model = new DefaultGridModel<TestRow>();
        model.addRow(new TestRow("Hi", 1, 1.0)); // "Hi" is only 2 chars
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        var buf = drawGrid(grid, 15, 5);
        String row1 = rowText(buf, 1, 15);
        // "Hi" + 8 spaces = 10 display columns, so position 0-9 should be "Hi        "
        assertEquals("Hi        ", row1.substring(0, 10),
                "Short content should be padded to minWidth(10): '" + row1.substring(0, 10) + "'");
    }

    // ---- 11. Negative number formatting -----------------------------------

    @Test
    void negativeNumberFormattedWithMinusSign() {
        var columns = List.of(
                GridColumn.<TestRow>doubleCol("Dev", "%+.2f", TestRow::score)
        );
        var model = new DefaultGridModel<TestRow>();
        model.addRow(new TestRow("X", 1, -3.14));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(15, 5));
        var buf = drawGrid(grid, 15, 5);
        String row1 = rowText(buf, 1, 15);
        assertTrue(row1.contains("-3.14"),
                "Negative number should include minus sign with %+.2f: " + row1);
    }

    // ---- 12. Large dataset (1000 rows) -------------------------------------

    @Test
    void largeDataset1000Rows() {
        var model = new DefaultGridModel<TestRow>();
        for (int i = 0; i < 1000; i++) {
            model.addRow(new TestRow("Row" + i, i, i * 0.5));
        }
        var columns = List.of(
                GridColumn.text("Name", TestRow::name),
                GridColumn.intCol("Age", TestRow::age)
        );
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 10));
        assertEquals(1000, model.getRowCount());
        assertDoesNotThrow(() -> drawGrid(grid, 40, 10));
        grid.setSelectedRow(500);
        assertEquals(500, grid.getSelectedRow());
        assertDoesNotThrow(() -> drawGrid(grid, 40, 10));
    }

    // ---- 13. Zero-width viewport -------------------------------------------

    @Test
    void zeroWidthViewportDoesNotCrash() {
        var grid = gridWithRows(0, 5, new TestRow("Alice", 30, 95.5));
        // getSize returns 0 columns — should degrade gracefully
        assertDoesNotThrow(() -> drawGrid(grid, 0, 5));
    }

    // ---- 14. Selected row beyond model size after clear -------------------

    @Test
    void selectedRowClampsAfterModelClear() {
        var model = new DefaultGridModel<TestRow>();
        for (int i = 0; i < 10; i++) {
            model.addRow(new TestRow("R" + i, i, i * 1.0));
        }
        var columns = List.of(GridColumn.text("N", TestRow::name));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        grid.setSelectedRow(5);
        assertEquals(5, grid.getSelectedRow());
        // Clear the model — selection should clamp to 0, no IndexOutOfBoundsException
        assertDoesNotThrow(() -> model.clear());
        assertEquals(0, grid.getSelectedRow(),
                "Selection should clamp to 0 after model clear");
        // Drawing after clear should not throw
        assertDoesNotThrow(() -> drawGrid(grid, 20, 5));
        // Accessing the selected item should return null (not throw)
        assertNull(grid.getSelectedItem(),
                "getSelectedItem should return null after model clear");
    }
}