package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListDataEventTest {

    @Test
    void eventCarriesTypeAndRange() {
        var event = new ListDataEvent(ListDataEventType.INTERVAL_ADDED, 2, 5);
        assertEquals(ListDataEventType.INTERVAL_ADDED, event.type());
        assertEquals(2, event.index0());
        assertEquals(5, event.index1());
    }

    @Test
    void singleIndexEventHasEqualBounds() {
        var event = new ListDataEvent(ListDataEventType.CONTENTS_CHANGED, 3, 3);
        assertEquals(3, event.index0());
        assertEquals(3, event.index1());
    }

    @Test
    void negativeBoundsAreAllowed() {
        var event = new ListDataEvent(ListDataEventType.INTERVAL_REMOVED, -1, -1);
        assertEquals(-1, event.index0());
        assertEquals(-1, event.index1());
    }

    @Test
    void eventTypesAreDistinct() {
        assertNotEquals(ListDataEventType.CONTENTS_CHANGED, ListDataEventType.INTERVAL_ADDED);
        assertNotEquals(ListDataEventType.INTERVAL_ADDED, ListDataEventType.INTERVAL_REMOVED);
    }
}
