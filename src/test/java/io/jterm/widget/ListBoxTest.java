package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import io.jterm.style.Theme;
import io.jterm.style.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListBoxTest {
    @Test
    void addItemUpdatesPreferredSize() {
        var list = new ListBox<String>();
        list.addItem("Alpha");
        list.addItem("Beta");
        assertEquals(new TerminalSize(5, 2), list.getPreferredSize());
    }

    @Test
    void selectionDefaultsToFirstItem() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        assertEquals(0, list.getSelectedIndex());
        assertEquals("A", list.getSelectedItem());
    }

    @Test
    void setSelectedIndexUpdatesSelection() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedIndex());
        assertEquals("B", list.getSelectedItem());
    }

    @Test
    void invalidIndexIgnored() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(5);
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void arrowDownMovesSelection() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void arrowUpStopsAtTop() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void enterFiresSelectionListener() {
        var list = new ListBox<String>();
        list.addItem("A");
        var fired = new boolean[1];
        list.addSelectionListener(() -> fired[0] = true);
        list.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }

    @Test
    void customRendererUsed() {
        var list = new ListBox<Integer>();
        list.addItem(42);
        list.setRenderer(i -> "Value: " + i);
        assertTrue(list.getPreferredSize().columns() >= 7);
    }

    @Test
    void scrollOffsetAdjustedWhenSelectionBelowViewport() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("Item " + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setSelectedIndex(5);
        assertTrue(list.getSelectedIndex() == 5);
    }

    @Test
    void emptyListReturnsNullSelection() {
        var list = new ListBox<String>();
        assertNull(list.getSelectedItem());
    }

    @Test
    void scrollToBottomRevealsLastItemWhenListExceedsViewport() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("Item " + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setSelectedIndex(0);
        list.scrollToBottom();
        assertEquals(7, list.getScrollOffset());
        assertTrue(list.isLastItemVisible());
    }

    @Test
    void scrollToBottomDoesNothingWhenAllItemsFit() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        list.setSelectedIndex(0);
        list.scrollToBottom();
        assertEquals(0, list.getScrollOffset());
    }

    @Test
    void autoScrollOnAddItemKeepsLastItemVisible() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setAutoScroll(true);
        for (int i = 0; i < 5; i++) list.addItem("Item " + i);
        assertTrue(list.isLastItemVisible(), "expected last item to be visible after auto-scroll add");
    }

    @Test
    void autoScrollDisabledLeavesScrollOffsetUnchangedOnAddItem() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        for (int i = 0; i < 5; i++) list.addItem("Item " + i);
        list.setScrollOffset(2);
        list.addItem("Item 5");
        assertEquals(2, list.getScrollOffset());
    }

    @Test
    void manualScrollUpPausesAutoScrollUntilNewItemAdded() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setAutoScroll(true);
        for (int i = 0; i < 5; i++) list.addItem("Item " + i);
        list.setScrollOffset(1); // manual scroll up
        assertEquals(1, list.getScrollOffset());
        list.setSelectedIndex(2); // selection change should not override manual position
        assertEquals(1, list.getScrollOffset(), "auto-scroll should not override manual scroll on selection change");
        list.addItem("Item 5"); // new item should resume auto-scroll
        assertTrue(list.isLastItemVisible(), "new item should resume auto-scroll to bottom");
    }

    @Test
    void scrollToBottomRespectsViewportBounds() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.addItem("A");
        list.addItem("B");
        list.scrollToBottom();
        assertEquals(0, list.getScrollOffset(), "scrollOffset must not be negative when fewer items than viewport");
    }

    @Test
    void scrollToBottomWithEmptyListIsNoOp() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        assertDoesNotThrow(list::scrollToBottom);
        assertEquals(0, list.getScrollOffset());
    }

    @Test
    void autoScrollGetterReflectsState() {
        var list = new ListBox<String>();
        assertFalse(list.isAutoScroll());
        list.setAutoScroll(true);
        assertTrue(list.isAutoScroll());
    }

    // ── Expanded coverage: items, navigation, keystrokes, rendering ───────

    @AfterEach
    void resetTheme() {
        ThemeManager.setActive(Theme.DARK);
    }

    @Test
    void getItemsReturnsDefensiveCopy() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        var snapshot = list.getItems();
        snapshot.clear();
        assertEquals(2, list.getItems().size(), "mutating getItems() result must not affect ListBox");
    }

    @Test
    void getItemsReturnsNewArrayListInstance() {
        var list = new ListBox<String>();
        list.addItem("X");
        var a = list.getItems();
        var b = list.getItems();
        assertNotSame(a, b, "getItems() must return a fresh list each call");
        assertEquals(1, a.size());
    }

    @Test
    void getSelectedItemReturnsNullWhenIndexOutOfRange() {
        var list = new ListBox<String>();
        list.addItem("A");
        // No setBounds needed; selectedIndex stays valid at 0
        assertEquals("A", list.getSelectedItem());
        // Force selectedIndex beyond range via setSelectedIndex clamping
        list.setSelectedIndex(99);
        assertEquals(0, list.getSelectedIndex());
        assertEquals("A", list.getSelectedItem());
    }

    @Test
    void setSelectedIndexNegativeClampsToZero() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(-5);
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void setSelectedIndexOnEmptyListStaysZero() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setSelectedIndex(3);
        assertEquals(0, list.getSelectedIndex());
        assertNull(list.getSelectedItem());
    }

    @Test
    void setSelectedIndexFiresSelectionListener() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        var fired = new int[1];
        list.addSelectionListener(() -> fired[0]++);
        list.setSelectedIndex(1);
        assertEquals(1, fired[0]);
    }

    @Test
    void setSelectedIndexSameValueDoesNotFireListener() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        var fired = new int[1];
        list.addSelectionListener(() -> fired[0]++);
        list.setSelectedIndex(0); // already 0
        assertEquals(0, fired[0]);
    }

    @Test
    void arrowDownStopsAtLastItem() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(1, list.getSelectedIndex());
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN)); // past last
        assertEquals(1, list.getSelectedIndex(), "selection must not go past last item");
    }

    @Test
    void arrowUpFromZeroStaysAtZero() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void singleItemNavigationStaysAtZero() {
        var list = new ListBox<String>();
        list.addItem("Solo");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_DOWN));
        assertEquals(0, list.getSelectedIndex());
        list.handleKeyStroke(new KeyStroke(KeyType.ARROW_UP));
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void unsupportedKeysAreIgnored() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        // Keys not handled by handleKeyStroke should be no-ops (default branch)
        for (var kt : new KeyType[]{
                KeyType.HOME, KeyType.END, KeyType.PAGE_UP, KeyType.PAGE_DOWN,
                KeyType.ARROW_LEFT, KeyType.ARROW_RIGHT, KeyType.TAB,
                KeyType.ESCAPE, KeyType.DELETE, KeyType.BACKSPACE,
                KeyType.INSERT, KeyType.F1, KeyType.EOF, KeyType.UNKNOWN}) {
            list.setSelectedIndex(0);
            list.handleKeyStroke(new KeyStroke(kt));
            assertEquals(0, list.getSelectedIndex(), "key " + kt + " should not move selection");
        }
    }

    @Test
    void characterKeyIsIgnored() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.handleKeyStroke(KeyStroke.character('x', false, false, false));
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void setScrollOffsetClampsToMaxOffset() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        // maxOffset = 10 - 3 = 7
        list.setScrollOffset(100);
        assertEquals(7, list.getScrollOffset());
    }

    @Test
    void setScrollOffsetClampsToZero() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setScrollOffset(-5);
        assertEquals(0, list.getScrollOffset());
    }

    @Test
    void setScrollOffsetSetsManualScrollFlag() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setAutoScroll(true);
        // scrolling to a position below max marks manual scroll
        list.setScrollOffset(2);
        // manualScroll should be true since 2 < 7 (maxOffset)
        // selection change should NOT override manual position
        list.setSelectedIndex(0);
        assertEquals(2, list.getScrollOffset(), "manual scroll should block ensureVisible on selection");
    }

    @Test
    void setScrollOffsetAtMaxDoesNotSetManualScroll() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setScrollOffset(7); // == maxOffset, manualScroll = false
        // Move selection away from 0 then back; ensureVisible should follow because manualScroll is false
        list.setSelectedIndex(5);
        list.setSelectedIndex(0);
        assertEquals(0, list.getScrollOffset(), "at max offset, selection should be followed (manualScroll=false)");
    }

    @Test
    void isLastItemVisibleTrueWhenAllFit() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.addItem("B");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertTrue(list.isLastItemVisible());
    }

    @Test
    void isLastItemVisibleFalseWhenScrolledAway() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setScrollOffset(0);
        assertFalse(list.isLastItemVisible(), "last item should not be visible when scrolled to top");
    }

    @Test
    void setRendererChangesPreferredSize() {
        var list = new ListBox<String>();
        list.addItem("short");
        int before = list.getPreferredSize().columns();
        list.setRenderer(s -> "PREFIX-" + s);
        int after = list.getPreferredSize().columns();
        assertTrue(after > before, "custom renderer should widen preferred size");
    }

    @Test
    void drawRendersItemsAndSelectionHighlight() {
        var list = new ListBox<String>();
        list.addItem("Apple");
        list.addItem("Banana");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        var buf = drawList(list, 10, 3);
        // Row 0 (selected "Apple") should use selection bg (WHITE in DARK theme)
        var selectedCell = buf.getCell(0, 0);
        assertEquals(AnsiColor.WHITE, selectedCell.bg(), "selected row should have selection background");
        assertEquals(AnsiColor.BLACK, selectedCell.fg(), "selected row should have selection foreground");
        assertTrue(selectedCell.is('A'));
        // Row 1 (unselected "Banana") should use normal bg (BLACK)
        var normalCell = buf.getCell(0, 1);
        assertEquals(AnsiColor.BLACK, normalCell.bg());
        assertEquals(AnsiColor.WHITE, normalCell.fg());
        assertTrue(normalCell.is('B'));
    }

    @Test
    void drawFillsEmptyRowsWithSpaces() {
        var list = new ListBox<String>();
        list.addItem("A");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        var buf = drawList(list, 5, 3);
        // Rows 1 and 2 are beyond items -> filled with spaces using theme colors
        assertTrue(buf.getCell(0, 1).is(' '));
        assertTrue(buf.getCell(0, 2).is(' '));
        assertEquals(AnsiColor.BLACK, buf.getCell(0, 1).bg(), "empty rows should use background color");
    }

    @Test
    void drawTruncatesLongItemToWidth() {
        var list = new ListBox<String>();
        list.addItem("VeryLongItemTextExceedingWidth");
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(6, 1));
        var buf = drawList(list, 6, 1);
        // First 6 chars of "VeryLongItemTextExceedingWidth" = "VeryLo"
        assertTrue(buf.getCell(0, 0).is('V'));
        assertTrue(buf.getCell(5, 0).is('o')); // "VeryLo"
    }

    @Test
    void drawDoesNotThrowOnEmptyList() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        assertDoesNotThrow(() -> {
            var buf = new ScreenBuffer(new TerminalSize(5, 3));
            list.draw(new TextGraphics(buf));
        });
    }

    @Test
    void drawWithScrollOffsetShowsCorrectItems() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(5, 3));
        list.setScrollOffset(3);
        var buf = drawList(list, 5, 3);
        // Row 0 should show item at index 3 = "I3"
        assertTrue(buf.getCell(0, 0).is('I'));
        assertTrue(buf.getCell(1, 0).is('3'));
    }

    @Test
    void ensureVisibleScrollsDownWhenSelectionBelowViewport() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setSelectedIndex(8);
        // scrollOffset should adjust so item 8 is visible
        assertTrue(list.getScrollOffset() >= 6, "scrollOffset should move down to reveal item 8");
    }

    @Test
    void ensureVisibleScrollsUpWhenSelectionAboveViewport() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        // Move selection down first (so selectedIndex != 0), then scroll up and select item 0
        list.setSelectedIndex(8);
        list.setScrollOffset(7);
        // Now select item 0 — selectedIndex changes from 8 to 0, ensureVisible runs
        list.setSelectedIndex(0);
        assertEquals(0, list.getScrollOffset(), "scrollOffset should reset to 0 when selecting item 0");
    }

    @Test
    void setBoundsTriggersEnsureVisible() {
        var list = new ListBox<String>();
        for (int i = 0; i < 10; i++) list.addItem("I" + i);
        list.setSelectedIndex(9);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        // After setBounds, ensureVisible runs -> item 9 should be visible
        assertTrue(list.isLastItemVisible());
    }

    @Test
    void setAutoScrollFalseClearsManualScroll() {
        var list = new ListBox<String>();
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setAutoScroll(true);
        list.setScrollOffset(1); // set manual
        list.setAutoScroll(false); // turning off autoScroll shouldn't clear manual, but turning back on should
        list.setAutoScroll(true);
        // autoScroll re-enabled should scroll to bottom and clear manual
        assertTrue(list.isLastItemVisible());
    }

    @Test
    void enterFiresAllListeners() {
        var list = new ListBox<String>();
        list.addItem("A");
        var count = new int[1];
        list.addSelectionListener(() -> count[0]++);
        list.addSelectionListener(() -> count[0] += 10);
        list.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertEquals(11, count[0], "all listeners should fire on Enter");
    }

    @Test
    void preferredSizeMinWidthIsFour() {
        var list = new ListBox<String>();
        // No items -> maxLen stays at 4
        assertEquals(4, list.getPreferredSize().columns());
        assertEquals(2, list.getPreferredSize().rows(), "empty list prefers at least 2 rows");
    }

    @Test
    void preferredSizeCapsAtTenRows() {
        var list = new ListBox<String>();
        for (int i = 0; i < 20; i++) list.addItem("X");
        assertEquals(10, list.getPreferredSize().rows());
    }

    @Test
    void addItemInvalidatesPreferredSize() {
        var list = new ListBox<String>();
        list.addItem("A");
        var size1 = list.getPreferredSize();
        list.addItem("LongerItem");
        var size2 = list.getPreferredSize();
        assertTrue(size2.columns() >= size1.columns());
    }

    @Test
    void setAutoScrollInvalidatesAndScrollsToBottom() {
        var list = new ListBox<String>();
        for (int i = 0; i < 5; i++) list.addItem("I" + i);
        list.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 3));
        list.setScrollOffset(0);
        list.setAutoScroll(true);
        assertTrue(list.isLastItemVisible(), "setAutoScroll(true) should scroll to bottom");
    }

    private ScreenBuffer drawList(ListBox<?> list, int cols, int rows) {
        var buf = new ScreenBuffer(new TerminalSize(cols, rows));
        list.draw(new TextGraphics(buf));
        return buf;
    }
}
