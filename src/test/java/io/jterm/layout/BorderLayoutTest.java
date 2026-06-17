package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.EmptySpace;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BorderLayoutTest {

    @Test
    void centerGetsRemainingSpace() {
        var panel = new Panel(new BorderLayout());
        var north = new Label("N");
        var center = new Label("C");
        panel.addComponent(north, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(center, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        assertEquals(new TerminalSize(10, 1), north.getSize());
        assertEquals(new TerminalPosition(0, 1), center.getPosition());
        assertEquals(new TerminalSize(10, 9), center.getSize());
    }

    @Test
    void eastAndWestFlankCenter() {
        var panel = new Panel(new BorderLayout());
        var west = new EmptySpace(new TerminalSize(2, 1));
        var east = new EmptySpace(new TerminalSize(3, 1));
        var center = new EmptySpace(new TerminalSize(1, 1));
        panel.addComponent(west, new BorderLayout.BorderLayoutData(BorderLayout.Region.WEST));
        panel.addComponent(center, new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        panel.addComponent(east, new BorderLayout.BorderLayoutData(BorderLayout.Region.EAST));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(12, 5));

        assertEquals(new TerminalPosition(0, 0), west.getPosition());
        assertEquals(new TerminalSize(2, 5), west.getSize());
        assertEquals(new TerminalPosition(2, 0), center.getPosition());
        assertEquals(new TerminalSize(7, 5), center.getSize());
        assertEquals(new TerminalPosition(9, 0), east.getPosition());
        assertEquals(new TerminalSize(3, 5), east.getSize());
    }

    @Test
    void northAndSouthSpanFullWidth() {
        var panel = new Panel(new BorderLayout());
        var north = new EmptySpace(new TerminalSize(1, 1));
        var south = new EmptySpace(new TerminalSize(1, 1));
        panel.addComponent(north, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(south, new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(8, 6));

        assertEquals(new TerminalPosition(0, 0), north.getPosition());
        assertEquals(new TerminalSize(8, 1), north.getSize());
        assertEquals(new TerminalPosition(0, 5), south.getPosition());
        assertEquals(new TerminalSize(8, 1), south.getSize());
    }

    @Test
    void allRegionsPreferredSize() {
        var panel = new Panel(new BorderLayout());
        panel.addComponent(new Label("W"), new BorderLayout.BorderLayoutData(BorderLayout.Region.WEST));
        panel.addComponent(new Label("N"), new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.addComponent(new Label("E"), new BorderLayout.BorderLayoutData(BorderLayout.Region.EAST));
        panel.addComponent(new Label("S"), new BorderLayout.BorderLayoutData(BorderLayout.Region.SOUTH));
        panel.addComponent(new Label("C"), new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));

        assertEquals(new TerminalSize(3, 3), panel.getPreferredSize());
    }

    @Test
    void missingCenterLeavesRoom() {
        var panel = new Panel(new BorderLayout());
        var north = new Label("N");
        panel.addComponent(north, new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 10));
        assertEquals(new TerminalSize(10, 1), north.getSize());
    }
}
