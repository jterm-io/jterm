package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.Label;
import io.jterm.widget.Panel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinearLayoutTest {
    @Test
    void verticalStacksComponents() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        panel.addComponent(new Label("A"));
        panel.addComponent(new Label("B"));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var children = panel.getChildren();
        assertEquals(0, children.get(0).getPosition().row());
        assertEquals(1, children.get(1).getPosition().row());
    }

    @Test
    void horizontalStacksComponents() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        panel.addComponent(new Label("A"));
        panel.addComponent(new Label("B"));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var children = panel.getChildren();
        assertEquals(0, children.get(0).getPosition().column());
        assertTrue(children.get(1).getPosition().column() > 0);
    }
}
