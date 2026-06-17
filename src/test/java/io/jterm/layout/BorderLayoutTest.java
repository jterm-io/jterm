package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
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
}
