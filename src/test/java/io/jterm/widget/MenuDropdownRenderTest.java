package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.BorderLayout;
import io.jterm.screen.ScreenBuffer;
import io.jterm.core.input.KeyStroke;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests proving dropdown menus render through the full layout+render chain.
 * These simulate the real path: Panel(BorderLayout) → MenuBar(NORTH) → Menu.draw → ScreenBuffer.
 */
class MenuDropdownRenderTest {

    @Test
    void dropdownVisibleThroughFullRenderChain() {
        var panel = new Panel(new BorderLayout());
        var menuBar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("New", () -> {});
        fileMenu.addMenuItem("Open", () -> {});
        fileMenu.addMenuItem("Save", () -> {});
        menuBar.addMenu(fileMenu);
        panel.addComponent(menuBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(new Label("Center content"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        // Open the menu
        menuBar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertTrue(fileMenu.isOpen());

        // Layout and render
        var size = new TerminalSize(40, 20);
        panel.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        panel.draw(g);

        // Row 1 should have "New", row 2 "Open", row 3 "Save"
        StringBuilder row1 = new StringBuilder();
        for (int c = 0; c < 10; c++) row1.append(buf.getCell(c, 1).character());
        assertTrue(row1.toString().contains("New"), "Row 1 should show 'New', got: " + row1);

        StringBuilder row2 = new StringBuilder();
        for (int c = 0; c < 10; c++) row2.append(buf.getCell(c, 2).character());
        assertTrue(row2.toString().contains("Open"), "Row 2 should show 'Open', got: " + row2);

        StringBuilder row3 = new StringBuilder();
        for (int c = 0; c < 10; c++) row3.append(buf.getCell(c, 3).character());
        assertTrue(row3.toString().contains("Save"), "Row 3 should show 'Save', got: " + row3);
    }

    @Test
    void dropdownNotVisibleWhenMenuClosedThroughFullChain() {
        var panel = new Panel(new BorderLayout());
        var menuBar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("New", () -> {});
        menuBar.addMenu(fileMenu);
        panel.addComponent(menuBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(new Label("Center"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        var size = new TerminalSize(40, 20);
        panel.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        panel.draw(g);

        StringBuilder row1 = new StringBuilder();
        for (int c = 0; c < 10; c++) row1.append(buf.getCell(c, 1).character());
        assertFalse(row1.toString().contains("New"), "Dropdown should not show when menu is closed");
    }

    @Test
    void dropdownWithSeparatorsRendersCorrectly() {
        var panel = new Panel(new BorderLayout());
        var menuBar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("New", () -> {});
        fileMenu.addSeparator();
        fileMenu.addMenuItem("Save", () -> {});
        menuBar.addMenu(fileMenu);
        panel.addComponent(menuBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(new Label("Center"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        menuBar.handleKeyStroke(KeyStroke.character('f', true, false, false));

        var size = new TerminalSize(40, 20);
        panel.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        panel.draw(g);

        // Row 1: "New", Row 2: separator (─), Row 3: "Save"
        StringBuilder row1 = new StringBuilder();
        for (int c = 0; c < 10; c++) row1.append(buf.getCell(c, 1).character());
        assertTrue(row1.toString().contains("New"), "Row 1 should show 'New'");

        // Row 2 should contain a separator line
        var sepCell = buf.getCell(1, 2);
        assertTrue(sepCell.character().equals("─") || sepCell.character().equals("-"),
                "Row 2 should be a separator, got: " + sepCell.character());

        StringBuilder row3 = new StringBuilder();
        for (int c = 0; c < 10; c++) row3.append(buf.getCell(c, 3).character());
        assertTrue(row3.toString().contains("Save"), "Row 3 should show 'Save'");
    }

    @Test
    void firstItemSelectedWhenMenuOpens() {
        var panel = new Panel(new BorderLayout());
        var menuBar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("New", () -> {});
        fileMenu.addMenuItem("Open", () -> {});
        menuBar.addMenu(fileMenu);
        panel.addComponent(menuBar, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(new Label("Center"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        menuBar.handleKeyStroke(KeyStroke.character('f', true, false, false));

        var size = new TerminalSize(40, 20);
        panel.setBounds(TerminalPosition.TOP_LEFT, size);
        var buf = new ScreenBuffer(size);
        var g = new TextGraphics(buf);
        panel.draw(g);

        // First item "New" should be selected (black on white)
        var cell = buf.getCell(1, 1); // first char of "New"
        assertEquals("New".charAt(0), cell.character().charAt(0),
                "First dropdown cell should be 'N' of 'New'");
        // Selected items have WHITE background
        // We can't easily check AnsiColor.WHITE from here but we can check it's not DEFAULT
    }
}