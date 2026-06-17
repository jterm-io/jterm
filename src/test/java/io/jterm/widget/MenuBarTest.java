package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuBarTest {

    @Test
    void addMenuIncreasesCount() {
        var bar = new MenuBar();
        assertEquals(0, bar.getMenus().size());
        bar.addMenu(new Menu("File"));
        bar.addMenu(new Menu("Edit"));
        assertEquals(2, bar.getMenus().size());
    }

    @Test
    void noActiveMenuByDefault() {
        var bar = new MenuBar();
        bar.addMenu(new Menu("File"));
        assertEquals(-1, bar.getActiveMenuIndex());
        assertFalse(bar.hasOpenMenu());
    }

    @Test
    void ctrlMnemonicOpensMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);
        bar.addMenu(new Menu("Edit"));

        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertTrue(fileMenu.isOpen());
        assertEquals(0, bar.getActiveMenuIndex());
        assertTrue(bar.hasOpenMenu());
    }

    @Test
    void ctrlMnemonicCaseInsensitive() {
        var bar = new MenuBar();
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Copy", () -> {});
        bar.addMenu(editMenu);

        bar.handleKeyStroke(KeyStroke.character('E', true, false, false));
        assertTrue(editMenu.isOpen());
    }

    @Test
    void arrowRightSwitchesToNextMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Copy", () -> {});
        bar.addMenu(fileMenu);
        bar.addMenu(editMenu);

        // Open File menu
        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertTrue(fileMenu.isOpen());

        // Arrow right → switch to Edit
        bar.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertFalse(fileMenu.isOpen());
        assertTrue(editMenu.isOpen());
        assertEquals(1, bar.getActiveMenuIndex());
    }

    @Test
    void arrowLeftWrapsToLastMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Copy", () -> {});
        bar.addMenu(fileMenu);
        bar.addMenu(editMenu);

        // Open File menu (index 0)
        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));

        // Arrow left → wraps to Edit (last menu)
        bar.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertFalse(fileMenu.isOpen());
        assertTrue(editMenu.isOpen());
        assertEquals(1, bar.getActiveMenuIndex());
    }

    @Test
    void arrowRightWrapsToFirstMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Copy", () -> {});
        bar.addMenu(fileMenu);
        bar.addMenu(editMenu);

        // Open Edit menu (index 1)
        bar.handleKeyStroke(KeyStroke.character('e', true, false, false));

        // Arrow right → wraps to File (first menu)
        bar.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertFalse(editMenu.isOpen());
        assertTrue(fileMenu.isOpen());
        assertEquals(0, bar.getActiveMenuIndex());
    }

    @Test
    void escapeClosesActiveMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertTrue(bar.hasOpenMenu());

        bar.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertFalse(bar.hasOpenMenu());
        assertFalse(fileMenu.isOpen());
    }

    @Test
    void enterActivatesItemInActiveMenu() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        var fired = new boolean[1];
        fileMenu.addMenuItem("Quit", () -> fired[0] = true);
        bar.addMenu(fileMenu);

        // Open File menu and press Enter on first item
        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        bar.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
        assertFalse(bar.hasOpenMenu());
    }

    @Test
    void arrowDownNavigatesIntoDropdown() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        var fired = new boolean[1];
        fileMenu.addMenuItem("First", () -> {});
        fileMenu.addMenuItem("Second", () -> fired[0] = true);
        bar.addMenu(fileMenu);

        // Open menu, arrow down to second item, Enter
        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        bar.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        bar.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }

    @Test
    void closeAllResetsState() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        bar.handleKeyStroke(KeyStroke.character('f', true, false, false));
        assertTrue(bar.hasOpenMenu());

        bar.closeAll();
        assertFalse(bar.hasOpenMenu());
        assertEquals(-1, bar.getActiveMenuIndex());
        assertFalse(fileMenu.isOpen());
    }

    @Test
    void drawDoesNotThrow() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        var buf = new ScreenBuffer(new TerminalSize(40, 10));
        var g = new TextGraphics(buf);
        bar.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 1));
        assertDoesNotThrow(() -> bar.drawComponent(g));
    }

    @Test
    void preferredSizeAggregatesMenus() {
        var bar = new MenuBar();
        bar.addMenu(new Menu("File"));
        bar.addMenu(new Menu("Edit"));
        bar.addMenu(new Menu("Help"));
        var ps = bar.getPreferredSize();
        // " File " (6) + " Edit " (6) + " Help " (6) = 18
        assertEquals(18, ps.columns());
        assertEquals(1, ps.rows());
    }

    @Test
    void unknownCtrlKeyDoesNothing() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        bar.handleKeyStroke(KeyStroke.character('z', true, false, false));
        assertFalse(bar.hasOpenMenu());
    }

    @Test
    void inputWithoutCtrlAndNoOpenMenuDoesNothing() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        // Regular 'f' without Ctrl should not open any menu
        bar.handleKeyStroke(KeyStroke.character('f', false, false, false));
        assertFalse(bar.hasOpenMenu());
    }

    @Test
    void altMnemonicOpensMenu() {
        // Bug: MenuBar only checked ctrl(), not alt() — Alt+letter was ignored.
        // Fix: MenuBar now accepts both ctrl() and alt().
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);
        bar.addMenu(new Menu("Edit"));

        bar.handleKeyStroke(KeyStroke.character('f', false, true, false)); // Alt+F
        assertTrue(fileMenu.isOpen());
        assertEquals(0, bar.getActiveMenuIndex());
        assertTrue(bar.hasOpenMenu());
    }

    @Test
    void altMnemonicCaseInsensitive() {
        var bar = new MenuBar();
        var editMenu = new Menu("Edit");
        editMenu.addMenuItem("Copy", () -> {});
        bar.addMenu(editMenu);

        bar.handleKeyStroke(KeyStroke.character('E', false, true, false)); // Alt+E
        assertTrue(editMenu.isOpen());
    }

    @Test
    void altMnemonicOpensHelpMenu() {
        // Bug: Ctrl+H (0x08) was consumed as Backspace — Help menu couldn't open via Ctrl+H.
        // Alt+H sends ESC+h, which was decoded correctly but MenuBar ignored alt().
        // Now both are fixed. Verify Alt+H works.
        var bar = new MenuBar();
        bar.addMenu(new Menu("File"));
        var helpMenu = new Menu("Help");
        helpMenu.addMenuItem("About", () -> {});
        bar.addMenu(helpMenu);

        bar.handleKeyStroke(KeyStroke.character('h', false, true, false)); // Alt+H
        assertTrue(helpMenu.isOpen());
        assertEquals(1, bar.getActiveMenuIndex());
    }

    @Test
    void plainCharWithoutModifierDoesNothing() {
        var bar = new MenuBar();
        var fileMenu = new Menu("File");
        fileMenu.addMenuItem("Open", () -> {});
        bar.addMenu(fileMenu);

        // 'f' with no modifiers should not open any menu
        bar.handleKeyStroke(KeyStroke.character('f', false, false, false));
        assertFalse(bar.hasOpenMenu());
    }
}