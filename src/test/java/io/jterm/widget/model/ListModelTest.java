package io.jterm.widget.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListModelTest {

    @Test
    void emptyListModelHasSizeZero() {
        ListModel<String> model = new DefaultListModel<>();
        assertEquals(0, model.getSize());
    }

    @Test
    void singleElementModelReturnsElement() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("only");
        assertEquals(1, model.getSize());
        assertEquals("only", model.getElementAt(0));
    }

    @Test
    void multipleElementsInOrder() {
        DefaultListModel<Integer> model = new DefaultListModel<>();
        model.addElement(1);
        model.addElement(2);
        model.addElement(3);
        assertEquals(3, model.getSize());
        assertEquals(1, model.getElementAt(0));
        assertEquals(2, model.getElementAt(1));
        assertEquals(3, model.getElementAt(2));
    }

    @Test
    void addElementAtInsertsAtIndex() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("c");
        model.addElementAt(1, "b");
        assertEquals(3, model.getSize());
        assertEquals("a", model.getElementAt(0));
        assertEquals("b", model.getElementAt(1));
        assertEquals("c", model.getElementAt(2));
    }

    @Test
    void setElementAtReplacesElement() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("old");
        model.setElementAt(0, "new");
        assertEquals("new", model.getElementAt(0));
        assertEquals(1, model.getSize());
    }

    @Test
    void removeElementAtRemovesElement() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        model.removeElementAt(0);
        assertEquals(1, model.getSize());
        assertEquals("b", model.getElementAt(0));
    }

    @Test
    void clearRemovesAllElements() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("a");
        model.addElement("b");
        model.clear();
        assertEquals(0, model.getSize());
    }

    @Test
    void getElementAtOutOfBoundsThrows() {
        ListModel<String> model = new DefaultListModel<>();
        assertThrows(IndexOutOfBoundsException.class, () -> model.getElementAt(0));
    }
}
