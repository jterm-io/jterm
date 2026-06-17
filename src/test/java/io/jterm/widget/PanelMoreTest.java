package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LinearLayout;
import io.jterm.screen.ScreenBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanelMoreTest {
    @Test
    void defaultLayoutManagerNull() {
        var panel = new Panel();
        assertNull(panel.getLayoutManager());
    }

    @Test
    void constructorSetsLayoutManager() {
        var layout = new LinearLayout(LinearLayout.Direction.HORIZONTAL);
        var panel = new Panel(layout);
        assertEquals(layout, panel.getLayoutManager());
    }

    @Test
    void childManagementDelegatesToContainer() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var label = new Label("A");
        panel.addComponent(label);
        assertEquals(1, panel.getChildren().size());
        assertEquals(panel, label.getParent());
    }

    @Test
    void preferredSizeDelegatesToLayout() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        panel.addComponent(new Label("AB"));
        panel.addComponent(new Label("CD"));
        assertEquals(new TerminalSize(4, 1), panel.getPreferredSize());
    }

    @Test
    void layoutAppliedToChildren() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.VERTICAL));
        var a = new Label("A");
        var b = new Label("B");
        panel.addComponent(a);
        panel.addComponent(b);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertTrue(a.getPosition().column() >= 0);
        assertTrue(a.getPosition().row() >= 0);
        assertEquals(1, b.getPosition().row());
    }

    @Test
    void drawRendersChildren() {
        var buffer = new ScreenBuffer(new TerminalSize(10, 5));
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        panel.addComponent(new Label("A"));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        panel.draw(new TextGraphics(buffer));
        assertEquals('A', buffer.getCell(0, 0).character().charAt(0));
    }
}
