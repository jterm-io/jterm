package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanelTest {

    @Test
    void addComponentStoresChild() {
        var panel = new Panel();
        var label = new Label("X");
        panel.addComponent(label);
        assertEquals(1, panel.getChildren().size());
        assertSame(label, panel.getChildren().get(0));
    }

    @Test
    void addComponentSetsParent() {
        var panel = new Panel();
        var label = new Label("X");
        panel.addComponent(label);
        assertSame(panel, label.getParent());
    }

    @Test
    void removeComponentClearsParent() {
        var panel = new Panel();
        var label = new Label("X");
        panel.addComponent(label);
        panel.removeComponent(label);
        assertTrue(panel.getChildren().isEmpty());
        assertNull(label.getParent());
    }

    @Test
    void layoutManagerIsUsed() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        var a = new EmptySpace(new TerminalSize(3, 1));
        var b = new EmptySpace(new TerminalSize(3, 1));
        a.setLayoutData(new io.jterm.layout.LinearLayout.LinearLayoutData(
            io.jterm.layout.LinearLayout.Alignment.BEGINNING));
        b.setLayoutData(new io.jterm.layout.LinearLayout.LinearLayoutData(
            io.jterm.layout.LinearLayout.Alignment.BEGINNING));
        panel.addComponent(a);
        panel.addComponent(b);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertEquals(new TerminalPosition(0, 0), a.getPosition());
        assertEquals(new TerminalSize(3, 1), a.getSize());
        assertEquals(new TerminalPosition(0, 1), b.getPosition());
        assertEquals(new TerminalSize(3, 1), b.getSize());
    }

    @Test
    void preferredSizeComesFromLayoutManager() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        panel.addComponent(new Label("AB"));
        panel.addComponent(new Label("CDE"));
        assertEquals(new TerminalSize(5, 1), panel.getPreferredSize());
    }

    @Test
    void changingLayoutManagerInvalidatesPreferredSize() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        panel.addComponent(new Label("A"));
        panel.getPreferredSize();
        panel.setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        assertEquals(new TerminalSize(1, 1), panel.getPreferredSize());
    }
}
