package io.jterm.widget;

import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractContainerExtraTest {
    @Test
    void removeAllClearsChildren() {
        var panel = new Panel();
        var a = new Label("A");
        var b = new Label("B");
        panel.addComponent(a);
        panel.addComponent(b);
        panel.removeComponent(a);
        panel.removeComponent(b);
        assertTrue(panel.getChildren().isEmpty());
    }

    @Test
    void setLayoutManagerInvalidatesAndAssigns() {
        var panel = new Panel();
        var layout = new LinearLayout(LinearLayout.Direction.HORIZONTAL);
        panel.setLayoutManager(layout);
        assertSame(layout, panel.getLayoutManager());
    }

    @Test
    void addComponentWithLayoutDataStoresData() {
        var panel = new Panel(new LinearLayout(LinearLayout.Direction.HORIZONTAL));
        var label = new Label("A");
        var data = new LinearLayout.LinearLayoutData(LinearLayout.Alignment.FILL);
        panel.addComponent(label, data);
        assertSame(data, label.getLayoutData());
    }

    @Test
    void removeAllOnEmptyPanelIsNoOp() {
        var panel = new Panel();
        assertDoesNotThrow(() -> panel.removeComponent(new Label("A")));
    }
}
