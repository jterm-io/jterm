package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.model.DefaultTableModel;
import org.junit.jupiter.api.Test;

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
}
