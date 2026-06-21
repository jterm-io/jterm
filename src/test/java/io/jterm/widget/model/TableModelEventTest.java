package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableModelEventTest {

    @Test
    void eventCarriesTypeRowsAndColumn() {
        var event = new TableModelEvent(TableModelEventType.ROWS_ADDED, 0, 4, -1);
        assertEquals(TableModelEventType.ROWS_ADDED, event.type());
        assertEquals(0, event.firstRow());
        assertEquals(4, event.lastRow());
        assertEquals(-1, event.column());
    }

    @Test
    void cellsChangedEventCarriesColumn() {
        var event = new TableModelEvent(TableModelEventType.CELLS_CHANGED, 1, 3, 2);
        assertEquals(TableModelEventType.CELLS_CHANGED, event.type());
        assertEquals(2, event.column());
    }

    @Test
    void structureChangedUsesWildcardIndices() {
        var event = new TableModelEvent(TableModelEventType.STRUCTURE_CHANGED, -1, -1, -1);
        assertEquals(-1, event.firstRow());
        assertEquals(-1, event.lastRow());
        assertEquals(-1, event.column());
    }

    @Test
    void eventTypesAreDistinct() {
        assertNotEquals(TableModelEventType.ROWS_ADDED, TableModelEventType.ROWS_REMOVED);
        assertNotEquals(TableModelEventType.ROWS_CHANGED, TableModelEventType.CELLS_CHANGED);
        assertNotEquals(TableModelEventType.CELLS_CHANGED, TableModelEventType.STRUCTURE_CHANGED);
    }
}
