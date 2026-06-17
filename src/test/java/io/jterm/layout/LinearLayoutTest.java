package io.jterm.layout;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.widget.EmptySpace;
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

    @Test
    void spacingIsAppliedBetweenComponents() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL, 2));
        panel.addComponent(new Label("A"));
        panel.addComponent(new Label("B"));
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        var children = panel.getChildren();
        assertEquals(0, children.get(0).getPosition().column());
        assertEquals(3, children.get(1).getPosition().column()); // 1 + 2 spacing
    }

    @Test
    void hiddenComponentsAreSkipped() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var a = new Label("A");
        var b = new Label("B");
        b.setVisible(false);
        panel.addComponent(a);
        panel.addComponent(b);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 5));
        assertEquals(0, a.getPosition().column());
        assertEquals(TerminalSize.ZERO, b.getSize()); // not laid out
    }

    @Test
    void fillAlignmentExpandsCrossAxis() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var a = new EmptySpace(new TerminalSize(2, 1));
        a.setLayoutData(new LinearLayout.LinearLayoutData(LinearLayout.Alignment.FILL));
        panel.addComponent(a);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(8, 5));
        assertEquals(new TerminalSize(2, 5), a.getSize());
    }

    @Test
    void canGrowDistributesExtraSpace() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var a = new EmptySpace(new TerminalSize(2, 1));
        var b = new EmptySpace(new TerminalSize(2, 1));
        b.setLayoutData(new LinearLayout.LinearLayoutData(LinearLayout.GrowPolicy.CAN_GROW));
        panel.addComponent(a);
        panel.addComponent(b);
        panel.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(10, 1));
        assertEquals(2, a.getSize().columns());
        assertEquals(8, b.getSize().columns()); // 10 - 2
    }

    @Test
    void preferredSizeAccountsForSpacing() {
        var layout = new LinearLayout(LinearLayout.Direction.HORIZONTAL, 1);
        var panel = new Panel(layout);
        panel.addComponent(new Label("AB"));
        panel.addComponent(new Label("CD"));
        assertEquals(new TerminalSize(5, 1), panel.getPreferredSize()); // 2 + 1 + 2
    }
}
