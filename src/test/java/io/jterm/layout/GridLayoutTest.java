package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.EmptySpace;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridLayoutTest {
    @Test
    void preferredSizeIsCellTimesDimensions() {
        var panel = new Panel(new GridLayout(2, 2));
        panel.addComponent(new EmptySpace(new TerminalSize(2, 2)));
        panel.addComponent(new EmptySpace(new TerminalSize(2, 2)));
        panel.addComponent(new EmptySpace(new TerminalSize(2, 2)));
        assertEquals(new TerminalSize(4, 4), panel.getPreferredSize());
    }

    @Test
    void preferredSizeIsCellTimesDimensions_min3Rows() {
        var panel = new Panel(new GridLayout(2, 2));
        panel.addComponent(new EmptySpace(new TerminalSize(2, 2)));
        panel.addComponent(new EmptySpace(new TerminalSize(2, 2)));
        assertEquals(new TerminalSize(4, 4), panel.getPreferredSize());
    }

    @Test
    void cellsAreEqualSized() {
        var panel = new Panel(new GridLayout(2, 2));
        var a = new EmptySpace(new TerminalSize(1, 1));
        var b = new EmptySpace(new TerminalSize(1, 1));
        var c = new EmptySpace(new TerminalSize(1, 1));
        var d = new EmptySpace(new TerminalSize(1, 1));
        panel.addComponent(a);
        panel.addComponent(b);
        panel.addComponent(c);
        panel.addComponent(d);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 4));
        assertEquals(new TerminalPosition(0, 0), a.getPosition());
        assertEquals(new TerminalPosition(2, 0), b.getPosition());
        assertEquals(new TerminalPosition(0, 2), c.getPosition());
        assertEquals(new TerminalPosition(2, 2), d.getPosition());
        assertEquals(new TerminalSize(2, 2), a.getSize());
    }

    @Test
    void extraChildrenIgnored() {
        var panel = new Panel(new GridLayout(1, 1));
        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(3, 3));
        var children = panel.getChildren();
        assertEquals(new TerminalSize(3, 3), children.get(0).getSize());
        assertEquals(TerminalPosition.TOP_LEFT, children.get(1).getPosition()); // not laid out
    }

    @Test
    void zeroColumnsGivesOneCell() {
        var layout = new GridLayout(0, 2);
        var panel = new Panel(layout);
        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(4, 4));
        var child = panel.getChildren().get(0);
        assertTrue(child.getSize().columns() >= 1);
        assertTrue(child.getSize().rows() >= 1);
    }
}
