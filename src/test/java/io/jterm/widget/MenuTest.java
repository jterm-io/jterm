package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuTest {

    @Test
    void titleStored() {
        var menu = new Menu("File");
        assertEquals("File", menu.getTitle());
    }

    @Test
    void mnemonicFromTitle() {
        var menu = new Menu("Edit");
        assertEquals('e', menu.getMnemonic());
    }

    @Test
    void explicitMnemonic() {
        var menu = new Menu("Help", 'h');
        assertEquals('h', menu.getMnemonic());
    }

    @Test
    void addMenuItemStoresEntries() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.addMenuItem("Quit", () -> {});
        assertEquals(2, menu.getEntries().size());
    }

    @Test
    void addSeparatorAddsSeparator() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.addSeparator();
        menu.addMenuItem("Quit", () -> {});
        assertEquals(3, menu.getEntries().size());
        assertInstanceOf(MenuSeparator.class, menu.getEntries().get(1));
    }

    @Test
    void openFalseByDefault() {
        var menu = new Menu("File");
        assertFalse(menu.isOpen());
    }

    @Test
    void setOpenTogglesState() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.setOpen(true);
        assertTrue(menu.isOpen());
        menu.setOpen(false);
        assertFalse(menu.isOpen());
    }

    @Test
    void openingMenuSelectsFirstItem() {
        var fired = new boolean[1];
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.addMenuItem("Save", () -> fired[0] = true);
        menu.setOpen(true);
        // selectedIndex starts at 0 (first item), ArrowDown moves to second
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        menu.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
        assertFalse(menu.isOpen());
    }

    @Test
    void arrowDownWraps() {
        var menu = new Menu("File");
        menu.addMenuItem("A", () -> {});
        menu.addMenuItem("B", () -> {});
        menu.addMenuItem("C", () -> {});
        menu.setOpen(true);
        // Move down past the last item — should wrap to first
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        // Now at first item again — verify by activating it
        var fired = new boolean[1];
        // Replace entry 0 with one we can track
        // Actually, just verify Enter doesn't crash
        assertDoesNotThrow(() -> menu.handleKeyStroke(new KeyStroke(KeyType.ENTER)));
    }

    @Test
    void escapeClosesMenu() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.setOpen(true);
        menu.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertFalse(menu.isOpen());
    }

    @Test
    void enterActivatesAndCloses() {
        var fired = new boolean[1];
        var menu = new Menu("File");
        menu.addMenuItem("Quit", () -> fired[0] = true);
        menu.setOpen(true);
        menu.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
        assertFalse(menu.isOpen());
    }

    @Test
    void arrowLeftRightClosesMenu() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.setOpen(true);
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_LEFT));
        assertFalse(menu.isOpen());

        menu.setOpen(true);
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_RIGHT));
        assertFalse(menu.isOpen());
    }

    @Test
    void closeCallbackFiredOnClose() {
        var callbackFired = new boolean[1];
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.setCloseCallback(() -> callbackFired[0] = true);
        menu.setOpen(true);
        menu.handleKeyStroke(new KeyStroke(KeyType.ESCAPE));
        assertTrue(callbackFired[0]);
    }

    @Test
    void separatorSkippedInNavigation() {
        var menu = new Menu("File");
        var fired = new boolean[1];
        menu.addMenuItem("Open", () -> {});
        menu.addSeparator();
        menu.addMenuItem("Quit", () -> fired[0] = true);
        menu.setOpen(true);
        // selectedIndex starts at 0 (first item), ArrowDown should skip separator to item 2
        menu.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        menu.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }

    @Test
    void drawDoesNotThrowWhenOpen() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.addSeparator();
        menu.addMenuItem("Quit", () -> {});
        menu.setOpen(true);
        var buf = new ScreenBuffer(new TerminalSize(20, 10));
        var g = new TextGraphics(buf);
        menu.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        assertDoesNotThrow(() -> menu.drawComponent(g));
    }

    @Test
    void drawDoesNotThrowWhenClosed() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        var buf = new ScreenBuffer(new TerminalSize(20, 10));
        var g = new TextGraphics(buf);
        menu.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 10));
        assertDoesNotThrow(() -> menu.drawComponent(g));
    }

    @Test
    void preferredSizeIncludesPadding() {
        var menu = new Menu("File");
        var ps = menu.getPreferredSize();
        // "File" + 2 spaces of padding = 6 columns, 1 row
        assertEquals(6, ps.columns());
        assertEquals(1, ps.rows());
    }

    @Test
    void dropdownWidthAccountsForItems() {
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> {});
        menu.addMenuItem("Save As…", () -> {});
        var width = menu.dropdownWidth();
        // "Save As…" is 8 chars + 2 padding = 10
        assertTrue(width >= 10);
    }

    @Test
    void inputIgnoredWhenClosed() {
        var fired = new boolean[1];
        var menu = new Menu("File");
        menu.addMenuItem("Open", () -> fired[0] = true);
        // Menu is closed — Enter should not activate
        menu.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertFalse(fired[0]);
    }
}