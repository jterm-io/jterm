package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractContainerTest {
    @Test
    void addComponentSetsParent() {
        var panel = new Panel();
        var label = new Label("A");
        panel.addComponent(label);
        assertEquals(panel, label.getParent());
    }

    @Test
    void removeComponentClearsParent() {
        var panel = new Panel();
        var label = new Label("A");
        panel.addComponent(label);
        panel.removeComponent(label);
        assertNull(label.getParent());
    }

    @Test
    void removeUnknownComponentIsNoOp() {
        var panel = new Panel();
        var label = new Label("A");
        panel.removeComponent(label);
        assertEquals(0, panel.getChildren().size());
    }

    @Test
    void addWithLayoutData() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var label = new Label("A");
        panel.addComponent(label, new LinearLayout.LinearLayoutData(LinearLayout.Alignment.FILL));
        assertNotNull(label.getLayoutData());
    }

    @Test
    void setLayoutManagerInvalidates() {
        var panel = new Panel();
        panel.setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        assertNotNull(panel.getLayoutManager());
    }

    @Test
    void getChildrenReturnsCopy() {
        var panel = new Panel();
        panel.addComponent(new Label("A"));
        var children = panel.getChildren();
        children.clear();
        assertEquals(1, panel.getChildren().size());
    }
}
