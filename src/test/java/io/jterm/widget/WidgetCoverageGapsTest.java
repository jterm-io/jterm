package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.model.DefaultGridModel;
import io.jterm.widget.model.GridColumn;
import io.jterm.widget.model.GridModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public methods in the widget package that previously had zero line coverage.
 */
class WidgetCoverageGapsTest {

    @Test
    void abstractComponentSetFocusable() {
        var comp = new EmptySpace();
        assertTrue(comp.isFocusable());
        comp.setFocusable(false);
        assertFalse(comp.isFocusable());
    }

    @Test
    void bordersBevelWrapsComponent() {
        var label = new Label("x");
        var border = Borders.bevel(label);
        assertNotNull(border);
        var ps = border.getPreferredSize();
        assertTrue(ps.columns() >= 3);
        assertTrue(ps.rows() >= 3);
    }

    @Test
    void textBoxForceUppercaseGetter() {
        var box = new TextBox();
        assertFalse(box.isForceUppercase());
        box.setForceUppercase(true);
        assertTrue(box.isForceUppercase());
        box.setValue("hello");
        assertEquals("HELLO", box.getValue());
    }

    @Test
    void menuSeparatorToString() {
        assertEquals("────────────────────", new MenuSeparator().toString());
    }

    @Test
    void tablePageDownAndPageUp() {
        var table = new Table("A");
        for (int i = 0; i < 20; i++) table.addRow("row" + i);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        table.setSelectedRow(0);

        table.pageDown();
        assertTrue(table.getSelectedRow() > 0);
        int afterDown = table.getSelectedRow();

        table.pageUp();
        assertTrue(table.getSelectedRow() < afterDown);
    }

    @Test
    void tablePageDownStopsAtBottom() {
        var table = new Table("A");
        for (int i = 0; i < 30; i++) table.addRow("row" + i);
        table.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        for (int i = 0; i < 10; i++) table.pageDown();
        assertTrue(table.getSelectedRow() >= 26, "pageDown should stop near the bottom");
    }

    @Test
    void buttonLabelGetterAndSetter() {
        var button = new Button("Old");
        assertEquals("Old", button.getLabel());
        button.setLabel("New");
        assertEquals("New", button.getLabel());
    }

    @Test
    void radioButtonDrawComponent() {
        var radio = new RadioButton("Option");
        radio.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 1));
        var buffer = new ScreenBuffer(new TerminalSize(12, 1));
        radio.drawComponent(new TextGraphics(buffer));
        assertEquals('(', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void listBoxRendererGetter() {
        var list = new ListBox<String>();
        assertNotNull(list.getRenderer());
        list.setRenderer(s -> "[" + s + "]");
        assertNotNull(list.getRenderer());
    }

    @Test
    void listBoxSelectionListenersDefensiveCopy() {
        var list = new ListBox<String>();
        Runnable listener = () -> {};
        list.addSelectionListener(listener);
        var listeners = list.getSelectionListeners();
        assertEquals(1, listeners.size());
        listeners.clear();
        assertEquals(1, list.getSelectionListeners().size());
    }

    @Test
    void menuAddMenuItemObject() {
        var menu = new Menu("File");
        var item = new MenuItem("Open", () -> {});
        menu.addMenuItem(item);
        assertEquals(1, menu.getEntries().size());
        assertSame(item, menu.getEntries().get(0));
    }

    // ---- DataGrid coverage --------------------------------------------------

    /** Row record used by DataGrid coverage tests. */
    record GridRow(String name, int age, double score) {}

    @Test
    void dataGridExtendsAbstractComponent() {
        var columns = List.of(GridColumn.text("Name", GridRow::name));
        var grid = new DataGrid<GridRow>(columns);
        assertTrue(grid instanceof AbstractComponent,
                "DataGrid should extend AbstractComponent");
    }

    @Test
    void dataGridInstantiatedAndDrawn() {
        var columns = List.of(
                GridColumn.text("Name", GridRow::name),
                GridColumn.intCol("Age", GridRow::age),
                GridColumn.doubleCol("Score", "%.2f", GridRow::score)
        );
        var model = new DefaultGridModel<GridRow>();
        model.addRows(List.of(
                new GridRow("Alice", 30, 95.5),
                new GridRow("Bob", 25, 87.3)
        ));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 5));
        var buffer = new ScreenBuffer(new TerminalSize(30, 5));
        assertDoesNotThrow(() -> grid.draw(new TextGraphics(buffer)));
        // Header row should contain column headers
        String header = buffer.getCell(0, 0).character()
                + buffer.getCell(1, 0).character()
                + buffer.getCell(2, 0).character()
                + buffer.getCell(3, 0).character();
        assertTrue(header.contains("Name"), "Header should contain 'Name'");
    }

    @Test
    void dataGridPublicMethodsAccessible() {
        var columns = List.of(
                GridColumn.text("Name", GridRow::name),
                GridColumn.intCol("Age", GridRow::age)
        );
        var model = new DefaultGridModel<GridRow>();
        model.addRows(List.of(
                new GridRow("Alice", 30, 1.0),
                new GridRow("Bob", 25, 2.0),
                new GridRow("Carol", 40, 3.0)
        ));
        var grid = new DataGrid<>(columns, model);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));

        // getSelectedRow / setSelectedRow
        assertEquals(0, grid.getSelectedRow());
        grid.setSelectedRow(2);
        assertEquals(2, grid.getSelectedRow());

        // getSelectedItem
        var selected = grid.getSelectedItem();
        assertNotNull(selected, "getSelectedItem should return non-null for valid selection");
        assertEquals("Carol", selected.name());

        // getColumns
        var cols = grid.getColumns();
        assertNotNull(cols, "getColumns should return non-null");
        assertEquals(2, cols.size(), "Should have 2 columns");

        // getModel / setModel
        GridModel<GridRow> m = grid.getModel();
        assertNotNull(m, "getModel should return non-null");
        var newModel = new DefaultGridModel<GridRow>();
        newModel.addRow(new GridRow("Dave", 50, 4.0));
        grid.setModel(newModel);
        assertNotNull(grid.getModel(), "getModel should return non-null after setModel");
        assertEquals(1, grid.getModel().getRowCount(), "New model should have 1 row");

        // addSelectionListener — add listener, then change selection to trigger it
        var fired = new boolean[]{false};
        grid.addSelectionListener(() -> fired[0] = true);
        // newModel has only 1 row (index 0), so set to 0 won't change selection.
        // Add a second row to the model so we can move selection to index 1.
        newModel.addRow(new GridRow("Eve", 35, 5.0));
        grid.setSelectedRow(1);
        assertTrue(fired[0], "Selection listener should fire on selection change");

        // pageUp / pageDown — need more rows than viewport to scroll
        var bigModel = new DefaultGridModel<GridRow>();
        for (int i = 0; i < 20; i++) {
            bigModel.addRow(new GridRow("R" + i, i, i));
        }
        grid.setModel(bigModel);
        grid.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 5));
        grid.setSelectedRow(0);
        grid.pageDown();
        assertTrue(grid.getSelectedRow() > 0, "pageDown should move selection forward");
        int afterDown = grid.getSelectedRow();
        grid.pageUp();
        assertTrue(grid.getSelectedRow() < afterDown, "pageUp should move selection backward");
    }
}
