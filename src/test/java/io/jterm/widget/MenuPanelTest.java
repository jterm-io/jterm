package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuPanelTest {

    @Test
    void preferredSizeForSingleItem() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        var ps = panel.getPreferredSize();
        // key width 1 + gap 3 + description 14 = 18 columns, 1 row
        assertEquals(18, ps.columns());
        assertEquals(1, ps.rows());
    }

    @Test
    void preferredSizeUsesLongestKeyAndDescription() {
        var panel = new MenuPanel();
        panel.addItem("A", "Short");
        panel.addItem("Enter", "A much longer description here");
        var ps = panel.getPreferredSize();
        int keyCol = 5; // "Enter"
        int gap = 3;
        int descWidth = 30; // "A much longer description here"
        assertEquals(keyCol + gap + descWidth, ps.columns());
        assertEquals(2, ps.rows());
    }

    @Test
    void emptyPanelHasMinimalSize() {
        var panel = new MenuPanel();
        var ps = panel.getPreferredSize();
        assertTrue(ps.columns() >= 2);
        assertEquals(0, ps.rows());
    }

    @Test
    void getItemsReturnsAddedEntries() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        assertEquals(2, panel.getItems().size());
        assertEquals("M", panel.getItems().get(0).key());
        assertEquals("Live Chat", panel.getItems().get(1).description());
    }

    @Test
    void clearItemsRemovesAll() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.clearItems();
        assertTrue(panel.getItems().isEmpty());
        assertEquals(0, panel.getPreferredSize().rows());
    }

    @Test
    void drawRightAlignsDescription() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(18, 1));
        var buf = new ScreenBuffer(new TerminalSize(18, 1));
        panel.draw(new TextGraphics(buf));
        // key "M" padded to keyCol=1, then gap 3 -> description starts at col 4
        // description width 14, right-aligned in remaining 14 cols (cols 4..17)
        assertEquals('M', buf.getCell(0, 0).character().charAt(0));
        assertEquals('M', buf.getCell(4, 0).character().charAt(0));
        assertEquals('s', buf.getCell(17, 0).character().charAt(0));
    }

    @Test
    void drawMultiCharKeyLeftPadded() {
        var panel = new MenuPanel();
        panel.addItem("Enter", "Submit");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(14, 1));
        var buf = new ScreenBuffer(new TerminalSize(14, 1));
        panel.draw(new TextGraphics(buf));
        // keyCol=5, gap=3, desc "Submit" (6) right-aligned in remaining 6 cols (cols 8..13)
        assertEquals('E', buf.getCell(0, 0).character().charAt(0));
        assertEquals('r', buf.getCell(4, 0).character().charAt(0));
        assertEquals('S', buf.getCell(8, 0).character().charAt(0));
        assertEquals('t', buf.getCell(13, 0).character().charAt(0));
    }

    @Test
    void drawHighlightsKey() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(20, 1));
        var buf = new ScreenBuffer(new TerminalSize(20, 1));
        panel.draw(new TextGraphics(buf));
        assertTrue(buf.getCell(0, 0).modifiers().contains(SGR.BOLD));
    }

    @Test
    void drawRendersMultipleRows() {
        var panel = new MenuPanel();
        panel.addItem("M", "Boards");
        panel.addItem("C", "Chat");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(14, 2));
        var buf = new ScreenBuffer(new TerminalSize(14, 2));
        panel.draw(new TextGraphics(buf));
        assertEquals('M', buf.getCell(0, 0).character().charAt(0));
        assertEquals('C', buf.getCell(0, 1).character().charAt(0));
        assertEquals('s', buf.getCell(13, 0).character().charAt(0));
        assertEquals('t', buf.getCell(13, 1).character().charAt(0));
    }

    @Test
    void drawWithBorderIntegration() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        var border = new Border(panel);
        // preferred size = panel + 2 border
        assertEquals(panel.getPreferredSize().columns() + 2, border.getPreferredSize().columns());
        assertEquals(panel.getPreferredSize().rows() + 2, border.getPreferredSize().rows());
    }

    @Test
    void emptyPanelDrawDoesNotThrow() {
        var panel = new MenuPanel();
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 2));
        var buf = new ScreenBuffer(new TerminalSize(4, 2));
        assertDoesNotThrow(() -> panel.draw(new TextGraphics(buf)));
    }

    @Test
    void descriptionIsRightAlignedEvenWhenPanelWider() {
        var panel = new MenuPanel();
        panel.addItem("Q", "Logout");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(22, 1));
        var buf = new ScreenBuffer(new TerminalSize(22, 1));
        panel.draw(new TextGraphics(buf));
        // "Logout" width 6, right-aligned in remaining 18 cols starts at col 4 + 12 = 16
        assertEquals('Q', buf.getCell(0, 0).character().charAt(0));
        assertEquals('L', buf.getCell(16, 0).character().charAt(0));
        assertEquals('t', buf.getCell(21, 0).character().charAt(0));
    }

    @Test
    void unicodeKeyWidthHandled() {
        var panel = new MenuPanel();
        panel.addItem("漢", "Chinese");
        panel.addItem("A", "English");
        // "漢" is double-width; keyCol should be 2
        var ps = panel.getPreferredSize();
        assertEquals(2 + 3 + 7, ps.columns()); // key 2 + gap 3 + "English" 7
    }
}
