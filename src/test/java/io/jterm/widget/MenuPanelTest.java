package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuPanelTest {

    // New format: "[M]" keys in brackets, left-aligned descriptions, vertical padding
    // keyColWidth = max key width + 2 (brackets), GAP = 5, 1 leading + 1 trailing space
    // rows = items + 2 (VERTICAL_PADDING * 2)

    @Test
    void preferredSizeForSingleItem() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        var ps = panel.getPreferredSize();
        // 1 leading + keyCol 3 ("[M]") + gap 5 + desc 14 + 1 trailing = 24 cols, 3 rows (1+2 padding)
        assertEquals(24, ps.columns());
        assertEquals(3, ps.rows());
    }

    @Test
    void preferredSizeUsesLongestKeyAndDescription() {
        var panel = new MenuPanel();
        panel.addItem("A", "Short");
        panel.addItem("Enter", "A much longer description here");
        var ps = panel.getPreferredSize();
        int keyCol = 7; // "[Enter]"
        int gap = 5;
        int descWidth = 30; // "A much longer description here"
        // 1 + keyCol + gap + descWidth + 1
        assertEquals(1 + keyCol + gap + descWidth + 1, ps.columns());
        assertEquals(4, ps.rows()); // 2 items + 2 padding
    }

    @Test
    void emptyPanelHasMinimalSize() {
        var panel = new MenuPanel();
        var ps = panel.getPreferredSize();
        assertTrue(ps.columns() >= 2);
        assertEquals(2, ps.rows()); // VERTICAL_PADDING * 2
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
        assertEquals(2, panel.getPreferredSize().rows()); // just padding
    }

    @Test
    void drawLeftAlignsDescription() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        // Preferred: 1 + 3 + 5 + 14 + 1 = 24 cols, 3 rows
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        var buf = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf));
        // Row 1 (first item, after 1 padding row)
        // col 1: '[', col 2: 'M', col 3: ']'
        assertEquals('[', buf.getCell(1, 1).character().charAt(0));
        assertEquals('M', buf.getCell(2, 1).character().charAt(0));
        assertEquals(']', buf.getCell(3, 1).character().charAt(0));
        // Description starts at col 1 + 3 + 5 = 9
        assertEquals('M', buf.getCell(9, 1).character().charAt(0));  // "Message"
        assertEquals('s', buf.getCell(22, 1).character().charAt(0)); // last char of "Boards"
    }

    @Test
    void drawMultiCharKeyLeftPadded() {
        var panel = new MenuPanel();
        panel.addItem("Enter", "Submit");
        // keyCol = 7 ("[Enter]"), preferred = 1 + 7 + 5 + 6 + 1 = 20
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        var buf = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf));
        // Row 1: col 1 = '[', col 2 = 'E', ..., col 7 = ']'
        assertEquals('[', buf.getCell(1, 1).character().charAt(0));
        assertEquals('E', buf.getCell(2, 1).character().charAt(0));
        assertEquals(']', buf.getCell(7, 1).character().charAt(0));
        // Description starts at col 1 + 7 + 5 = 13
        assertEquals('S', buf.getCell(13, 1).character().charAt(0));
    }

    @Test
    void drawHighlightsKey() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(24, 3));
        var buf = new ScreenBuffer(new TerminalSize(24, 3));
        panel.draw(new TextGraphics(buf));
        // Key is at row 1, col 1 (the '[' of "[M]")
        assertTrue(buf.getCell(1, 1).modifiers().contains(SGR.BOLD));
    }

    @Test
    void drawRendersMultipleRows() {
        var panel = new MenuPanel();
        panel.addItem("M", "Boards");
        panel.addItem("C", "Chat");
        // keyCol=3, preferred = 1 + 3 + 5 + 6 + 1 = 16 cols, 4 rows
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        var buf = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf));
        // Row 1 (first item): [M] at col 1, "Boards" at col 9
        assertEquals('[', buf.getCell(1, 1).character().charAt(0));
        assertEquals('M', buf.getCell(2, 1).character().charAt(0));
        assertEquals('s', buf.getCell(14, 1).character().charAt(0)); // last of "Boards"
        // Row 2 (second item): [C] at col 1, "Chat" at col 9
        assertEquals('[', buf.getCell(1, 2).character().charAt(0));
        assertEquals('C', buf.getCell(2, 2).character().charAt(0));
        assertEquals('t', buf.getCell(12, 2).character().charAt(0)); // last of "Chat"
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
    void descriptionIsLeftAlignedEvenWhenPanelWider() {
        var panel = new MenuPanel();
        panel.addItem("Q", "Logout");
        // Give extra width: 30 cols, 3 rows
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(30, 3));
        var buf = new ScreenBuffer(new TerminalSize(30, 3));
        panel.draw(new TextGraphics(buf));
        // "[Q]" at col 1, description starts at col 1 + 3 + 5 = 9
        assertEquals('[', buf.getCell(1, 1).character().charAt(0));
        assertEquals('Q', buf.getCell(2, 1).character().charAt(0));
        assertEquals('L', buf.getCell(9, 1).character().charAt(0)); // "Logout" left-aligned
        assertEquals('t', buf.getCell(14, 1).character().charAt(0)); // last of "Logout"
    }

    @Test
    void unicodeKeyWidthHandled() {
        var panel = new MenuPanel();
        panel.addItem("漢", "Chinese");
        panel.addItem("A", "English");
        // "漢" is double-width; "[漢]" = 4, "[A]" = 3, keyCol = 4
        var ps = panel.getPreferredSize();
        // 1 + 4 + 5 + 7 + 1 = 18
        assertEquals(1 + 4 + 5 + 7 + 1, ps.columns());
    }

    // ---- Highlight / blink tests ----

    @Test
    void indexOfKeyFindsMatchingItem() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        assertEquals(0, panel.indexOfKey("M"));
        assertEquals(1, panel.indexOfKey("C"));
        assertEquals(0, panel.indexOfKey("m")); // case-insensitive
        assertEquals(-1, panel.indexOfKey("Z"));
    }

    @Test
    void highlightedIndexDefaultsToNone() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        assertEquals(-1, panel.getHighlightedIndex());
    }

    @Test
    void setHighlightedIndexClampsToValidRange() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        panel.setHighlightedIndex(5); // out of range
        assertEquals(1, panel.getHighlightedIndex()); // clamped to last item
        panel.setHighlightedIndex(-1);
        assertEquals(-1, panel.getHighlightedIndex());
    }

    @Test
    void highlightedRowRendersInReverseVideo() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        panel.setHighlightedIndex(0);

        var buf = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf));

        var theme = ThemeManager.active();
        // Row 1 (first item, index 0) should have swapped fg/bg (inverted)
        var hlCell = buf.getCell(1, 1);
        assertEquals(theme.background(), hlCell.fg(),
                "highlighted row should have bg color as fg (inverted)");
        assertEquals(theme.foreground(), hlCell.bg(),
                "highlighted row should have fg color as bg (inverted)");
        // Row 2 (second item, index 1) should NOT be inverted
        var normCell = buf.getCell(1, 2);
        assertEquals(theme.foreground(), normCell.fg(),
                "non-highlighted row should have normal fg");
        assertEquals(theme.background(), normCell.bg(),
                "non-highlighted row should have normal bg");
    }

    @Test
    void clearHighlightRemovesReverseVideo() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        panel.setHighlightedIndex(0);

        var theme = ThemeManager.active();
        var buf1 = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf1));
        assertEquals(theme.background(), buf1.getCell(1, 1).fg(),
                "highlighted row should have inverted fg");

        panel.setHighlightedIndex(-1);
        var buf2 = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf2));
        assertEquals(theme.foreground(), buf2.getCell(1, 1).fg(),
                "cleared highlight should restore normal fg");
    }

    @Test
    void clearItemsResetsHighlightedIndex() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.setHighlightedIndex(0);
        panel.clearItems();
        assertEquals(-1, panel.getHighlightedIndex());
    }

    // ---- Multi-column tests ----

    @Test
    void multiColumnPreferredSizeIsWiderAndShorter() {
        var panel = new MenuPanel();
        panel.addItem("A", "Item A");
        panel.addItem("B", "Item B");
        panel.addItem("C", "Item C");
        panel.addItem("D", "Item D");

        // Single column: 4 items + 2 padding = 6 rows
        var singlePs = panel.getPreferredSize();
        assertEquals(6, singlePs.rows());

        // Two columns: 2 items per column + 2 padding = 4 rows, wider
        panel.setColumns(2);
        var multiPs = panel.getPreferredSize();
        assertEquals(4, multiPs.rows());
        assertTrue(multiPs.columns() > singlePs.columns(), "multi-column should be wider");
    }

    @Test
    void multiColumnDrawsItemsInColumns() {
        var panel = new MenuPanel();
        panel.addItem("A", "Item A");
        panel.addItem("B", "Item B");
        panel.addItem("C", "Item C");
        panel.addItem("D", "Item D");
        panel.setColumns(2);

        var ps = panel.getPreferredSize();
        panel.setBounds(TerminalPosition.TOP_LEFT, ps);
        var buf = new ScreenBuffer(ps);
        panel.draw(new TextGraphics(buf));

        int itemsPerCol = 2; // ceil(4/2) = 2
        int keyColWidth = 3; // "[A]"
        int gap = 5;
        int colWidth = 1 + keyColWidth + gap + 6 + 1; // 1 + 3 + 5 + 6 ("Item A") + 1 = 16
        int colGap = 2;

        // Column 0, row 1 (first item, after padding): [A] at col 1
        assertEquals('[', buf.getCell(1, 1).character().charAt(0));
        assertEquals('A', buf.getCell(2, 1).character().charAt(0));
        // Column 0, row 2 (second item): [B]
        assertEquals('[', buf.getCell(1, 2).character().charAt(0));
        assertEquals('B', buf.getCell(2, 2).character().charAt(0));
        // Column 1, row 1 (third item): [C] at col 1 + colWidth + colGap
        int col1Start = colWidth + colGap;
        assertEquals('[', buf.getCell(col1Start + 1, 1).character().charAt(0));
        assertEquals('C', buf.getCell(col1Start + 2, 1).character().charAt(0));
        // Column 1, row 2 (fourth item): [D]
        assertEquals('[', buf.getCell(col1Start + 1, 2).character().charAt(0));
        assertEquals('D', buf.getCell(col1Start + 2, 2).character().charAt(0));
    }

    @Test
    void multiColumnHighlightWorks() {
        var panel = new MenuPanel();
        panel.addItem("A", "Item A");
        panel.addItem("B", "Item B");
        panel.addItem("C", "Item C");
        panel.addItem("D", "Item D");
        panel.setColumns(2);
        panel.setBounds(TerminalPosition.TOP_LEFT, panel.getPreferredSize());
        panel.setHighlightedIndex(2); // item C, column 1, row 0

        var buf = new ScreenBuffer(panel.getPreferredSize());
        panel.draw(new TextGraphics(buf));

        var theme = ThemeManager.active();
        // Item C is in column 1, row 1 (after padding)
        // Check that the highlighted cell is inverted
        var keyColWidth = 3;
        int colWidth = 1 + keyColWidth + 5 + 6 + 1; // 16
        int col1X = colWidth + 2 + 1; // col1Start + 1
        var hlCell = buf.getCell(col1X, 1);
        assertEquals(theme.background(), hlCell.fg(), "highlighted item should have inverted colors");
    }

    @Test
    void singleColumnDefaultIsUnchanged() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        panel.addItem("C", "Live Chat");
        // Default columns = 1, should behave exactly as before
        var ps = panel.getPreferredSize();
        assertEquals(1 + 3 + 5 + 14 + 1, ps.columns()); // 24
        assertEquals(4, ps.rows()); // 2 items + 2 padding
    }

    @Test
    void setColumnsClampsToOne() {
        var panel = new MenuPanel();
        panel.setColumns(0);
        assertEquals(1, panel.getColumns());
        panel.setColumns(-5);
        assertEquals(1, panel.getColumns());
    }

    @Test
    void oddItemCountRoundsUp() {
        var panel = new MenuPanel();
        for (int i = 0; i < 5; i++) {
            panel.addItem(String.valueOf((char)('A' + i)), "Item " + (char)('A' + i));
        }
        panel.setColumns(2);
        var ps = panel.getPreferredSize();
        // ceil(5/2) = 3 items per column + 2 padding = 5 rows
        assertEquals(5, ps.rows());
    }
}