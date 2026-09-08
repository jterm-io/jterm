package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
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
 * M4 tests: scrolling, selection, and key handling for {@link DataGrid}.
 */
class DataGridScrollTest {

    /** Simple record used as the row type for tests (same shape as M3 tests). */
    record TestRow(String name, int age, double score) {}

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

    private DataGrid<TestRow> gridWithRows(int width, int height, List<TestRow> rows) {
        return gridWithRows(width, height, rows.toArray(new TestRow[0]));
    }

    private List<TestRow> tenRows() {
        return java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> new TestRow("R" + i, i, i * 1.5))
                .toList();
    }

    // KeyStroke factories
    private static KeyStroke arrow(KeyType type) {
        return new KeyStroke(type);
    }

    private static KeyStroke ctrl(char c) {
        return KeyStroke.character(c, true, false, false);
    }

    private static KeyStroke shiftArrow(KeyType type) {
        return new KeyStroke(type, '\0', false, false, true);
    }

    // ---- Tests -------------------------------------------------------------

    @Test
    void arrowDownMovesSelection() {
        var grid = gridWithRows(30, 6, tenRows());
        assertEquals(0, grid.getSelectedRow());
        grid.handleKeyStroke(arrow(KeyType.ARROW_DOWN));
        assertEquals(1, grid.getSelectedRow());
    }

    @Test
    void arrowUpMovesSelection() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(3);
        grid.handleKeyStroke(arrow(KeyType.ARROW_UP));
        assertEquals(2, grid.getSelectedRow());
    }

    @Test
    void arrowDownClampsAtLastRow() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(9); // last row
        grid.handleKeyStroke(arrow(KeyType.ARROW_DOWN));
        assertEquals(9, grid.getSelectedRow(), "Should clamp at last row");
    }

    @Test
    void arrowUpClampsAtFirstRow() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(0);
        grid.handleKeyStroke(arrow(KeyType.ARROW_UP));
        assertEquals(0, grid.getSelectedRow(), "Should clamp at first row");
    }

    @Test
    void ctrlPMovesUp() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(5);
        grid.handleKeyStroke(ctrl('P'));
        assertEquals(4, grid.getSelectedRow());
    }

    @Test
    void ctrlNMovesDown() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(5);
        grid.handleKeyStroke(ctrl('N'));
        assertEquals(6, grid.getSelectedRow());
    }

    @Test
    void ctrlVPagesDown() {
        var grid = gridWithRows(30, 6, tenRows()); // visibleRows = 6 - 1 = 5
        assertEquals(0, grid.getSelectedRow());
        grid.handleKeyStroke(ctrl('V'));
        // pageDown sets scrollOffsetY = min(0+5, 10-5)=5, setSelectedRow(5)
        assertEquals(5, grid.getSelectedRow(), "Ctrl+V should page down");
    }

    @Test
    void pageDownAdvancesScrollOffsetY() {
        var grid = gridWithRows(30, 6, tenRows()); // visibleRows = 5
        assertEquals(0, grid.getScrollOffsetY());
        grid.handleKeyStroke(arrow(KeyType.PAGE_DOWN));
        assertEquals(5, grid.getScrollOffsetY(), "PAGE_DOWN should advance scrollOffsetY");
    }

    @Test
    void pageUpGoesBack() {
        var grid = gridWithRows(30, 6, tenRows()); // visibleRows = 5
        // move down first
        grid.handleKeyStroke(arrow(KeyType.PAGE_DOWN));
        assertEquals(5, grid.getScrollOffsetY());
        // page up
        grid.handleKeyStroke(arrow(KeyType.PAGE_UP));
        assertEquals(0, grid.getScrollOffsetY(), "PAGE_UP should reduce scrollOffsetY");
    }

    @Test
    void homeGoesToFirstRow() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(7);
        grid.handleKeyStroke(arrow(KeyType.HOME));
        assertEquals(0, grid.getSelectedRow(), "HOME should select first row");
    }

    @Test
    void endGoesToLastRow() {
        var grid = gridWithRows(30, 6, tenRows());
        grid.setSelectedRow(0);
        grid.handleKeyStroke(arrow(KeyType.END));
        assertEquals(9, grid.getSelectedRow(), "END should select last row");
    }

    @Test
    void enterFiresActivationListener() {
        var grid = gridWithRows(30, 6, tenRows());
        var fired = new boolean[]{false};
        grid.addActivationListener(() -> fired[0] = true);
        grid.handleKeyStroke(arrow(KeyType.ENTER));
        assertTrue(fired[0], "ENTER should fire activation listener");
    }

    @Test
    void enterDoesNotFireSelectionListener() {
        // ENTER is activation, not selection: arrow keys move the selection
        // and fire selection listeners; only Enter fires activation.
        var grid = gridWithRows(30, 6, tenRows());
        var fired = new boolean[]{false};
        grid.addSelectionListener(() -> fired[0] = true);
        grid.handleKeyStroke(arrow(KeyType.ENTER));
        assertFalse(fired[0], "ENTER must not fire selection listener");
    }

    @Test
    void scrollLeftClampsAtZero() {
        var grid = gridWithRows(30, 6, tenRows());
        assertEquals(0, grid.getScrollOffsetX());
        grid.scrollLeft();
        assertEquals(0, grid.getScrollOffsetX(), "scrollOffsetX can't go below 0");
    }

    @Test
    void scrollRightAdvances() {
        // Build a grid with many wide columns so total width exceeds the narrow viewport.
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.text("Col1", TestRow::name),
                GridColumn.text("Col2", TestRow::name),
                GridColumn.text("Col3", TestRow::name),
                GridColumn.text("Col4", TestRow::name)
        );
        var model = modelWithRows(new TestRow("Name", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        // Narrow viewport: 10 cols. Each column header is 4 wide + 1 sep = total ~19.
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        // need columnWidths computed — draw triggers computeColumnWidths
        var buf = new io.jterm.screen.ScreenBuffer(new TerminalSize(10, 5));
        grid.draw(new io.jterm.graphics.TextGraphics(buf));
        assertEquals(0, grid.getScrollOffsetX());
        grid.scrollRight();
        assertTrue(grid.getScrollOffsetX() > 0, "scrollOffsetX should advance when there's room");
    }

    @Test
    void ensureVisibleAdjustsScrollOffsetY() {
        // Viewport height 3 → 1 header + 2 data rows visible.
        var grid = gridWithRows(30, 3, tenRows());
        assertEquals(0, grid.getScrollOffsetY());
        // Select row 5 — below the viewport (visible rows are 0,1).
        grid.setSelectedRow(5);
        assertTrue(grid.getScrollOffsetY() > 0,
                "ensureVisible should move scrollOffsetY so row 5 is visible: " + grid.getScrollOffsetY());
        // selectedRow should be within [scrollOffsetY, scrollOffsetY + visibleRows)
        int visibleRows = Math.max(1, grid.getSize().rows() - 1);
        assertTrue(grid.getSelectedRow() >= grid.getScrollOffsetY()
                        && grid.getSelectedRow() < grid.getScrollOffsetY() + visibleRows,
                "selectedRow should be within the viewport");
    }

    @Test
    void selectionListenerFiresOnUpDown() {
        var grid = gridWithRows(30, 6, tenRows());
        var firedCount = new int[]{0};
        grid.addSelectionListener(() -> firedCount[0]++);
        grid.handleKeyStroke(arrow(KeyType.ARROW_DOWN));
        assertEquals(1, firedCount[0], "Listener should fire once on arrow down");
        grid.handleKeyStroke(arrow(KeyType.ARROW_UP));
        assertEquals(2, firedCount[0], "Listener should fire once on arrow up");
    }

    @Test
    void arrowLeftScrollsLeft() {
        // Make a grid wide enough to scroll, then scroll right, then left.
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.text("Col1", TestRow::name),
                GridColumn.text("Col2", TestRow::name),
                GridColumn.text("Col3", TestRow::name),
                GridColumn.text("Col4", TestRow::name)
        );
        var model = modelWithRows(new TestRow("Name", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buf = new io.jterm.screen.ScreenBuffer(new TerminalSize(10, 5));
        grid.draw(new io.jterm.graphics.TextGraphics(buf));
        grid.scrollRight();
        int afterRight = grid.getScrollOffsetX();
        assertTrue(afterRight > 0);
        grid.handleKeyStroke(arrow(KeyType.ARROW_LEFT));
        assertEquals(afterRight - 1, grid.getScrollOffsetX(), "ARROW_LEFT should decrement scrollOffsetX");
    }

    @Test
    void arrowRightScrollsRight() {
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.text("Col1", TestRow::name),
                GridColumn.text("Col2", TestRow::name),
                GridColumn.text("Col3", TestRow::name),
                GridColumn.text("Col4", TestRow::name)
        );
        var model = modelWithRows(new TestRow("Name", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buf = new io.jterm.screen.ScreenBuffer(new TerminalSize(10, 5));
        grid.draw(new io.jterm.graphics.TextGraphics(buf));
        assertEquals(0, grid.getScrollOffsetX());
        grid.handleKeyStroke(arrow(KeyType.ARROW_RIGHT));
        assertEquals(1, grid.getScrollOffsetX(), "ARROW_RIGHT should increment scrollOffsetX");
    }

    @Test
    void shiftArrowLeftScrollsLeft() {
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.text("Col1", TestRow::name),
                GridColumn.text("Col2", TestRow::name),
                GridColumn.text("Col3", TestRow::name),
                GridColumn.text("Col4", TestRow::name)
        );
        var model = modelWithRows(new TestRow("Name", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buf = new io.jterm.screen.ScreenBuffer(new TerminalSize(10, 5));
        grid.draw(new io.jterm.graphics.TextGraphics(buf));
        grid.scrollRight();
        int afterRight = grid.getScrollOffsetX();
        grid.handleKeyStroke(shiftArrow(KeyType.ARROW_LEFT));
        assertEquals(afterRight - 1, grid.getScrollOffsetX(), "Shift+ARROW_LEFT should scroll left");
    }

    @Test
    void shiftArrowRightScrollsRight() {
        var columns = List.<GridColumn<TestRow>>of(
                GridColumn.text("Col1", TestRow::name),
                GridColumn.text("Col2", TestRow::name),
                GridColumn.text("Col3", TestRow::name),
                GridColumn.text("Col4", TestRow::name)
        );
        var model = modelWithRows(new TestRow("Name", 1, 1.0));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var buf = new io.jterm.screen.ScreenBuffer(new TerminalSize(10, 5));
        grid.draw(new io.jterm.graphics.TextGraphics(buf));
        grid.handleKeyStroke(shiftArrow(KeyType.ARROW_RIGHT));
        assertEquals(1, grid.getScrollOffsetX(), "Shift+ARROW_RIGHT should scroll right");
    }
}