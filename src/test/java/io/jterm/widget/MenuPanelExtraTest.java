package io.jterm.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuPanelExtraTest {
    @Test
    void indexOfKeyFindsCaseInsensitive() {
        var panel = new MenuPanel();
        panel.addItem("M", "Message Boards");
        assertEquals(0, panel.indexOfKey("m"));
        assertEquals(0, panel.indexOfKey("M"));
        assertEquals(-1, panel.indexOfKey("z"));
        assertEquals(-1, panel.indexOfKey(null));
    }

    @Test
    void highlightedIndexClampsToRange() {
        var panel = new MenuPanel();
        panel.addItem("A", "A");
        panel.setHighlightedIndex(10);
        assertEquals(0, panel.getHighlightedIndex());
        panel.setHighlightedIndex(-1);
        assertEquals(-1, panel.getHighlightedIndex());
    }

    @Test
    void clearItemsResetsHighlight() {
        var panel = new MenuPanel();
        panel.addItem("A", "A");
        panel.setHighlightedIndex(0);
        panel.clearItems();
        assertTrue(panel.getItems().isEmpty());
        assertEquals(-1, panel.getHighlightedIndex());
    }

    @Test
    void emptyPreferredSizeIsSmall() {
        var panel = new MenuPanel();
        var ps = panel.getPreferredSize();
        assertTrue(ps.columns() > 0);
        assertTrue(ps.rows() > 0);
    }
}
