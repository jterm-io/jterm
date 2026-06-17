package io.jterm.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MenuItemTest {

    @Test
    void labelAndActionStored() {
        var fired = new boolean[1];
        var item = new MenuItem("Open", () -> fired[0] = true);
        assertEquals("Open", item.getLabel());
        assertNotNull(item.getAction());
        item.activate();
        assertTrue(fired[0]);
    }

    @Test
    void explicitMnemonic() {
        var item = new MenuItem("Save", 's', () -> {});
        assertEquals('s', item.getMnemonic());
    }

    @Test
    void autoMnemonicFromFirstChar() {
        var item = new MenuItem("Quit", () -> {});
        assertEquals('q', item.getMnemonic());
    }

    @Test
    void nullActionDoesNotThrow() {
        var item = new MenuItem("Label", null);
        assertDoesNotThrow(item::activate);
    }

    @Test
    void emptyLabelHasNullMnemonic() {
        var item = new MenuItem("", () -> {});
        assertEquals('\0', item.getMnemonic());
    }
}