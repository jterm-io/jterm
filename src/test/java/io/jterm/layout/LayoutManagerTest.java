package io.jterm.layout;

import io.jterm.core.TerminalSize;
import io.jterm.widget.Component;
import io.jterm.widget.EmptySpace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LayoutManagerTest {
    @Test
    void interfaceContractIsSatisfiedByLinearLayout() {
        LayoutManager manager = new LinearLayout(LinearLayout.Direction.HORIZONTAL);
        var children = new ArrayList<io.jterm.widget.Component>();
        children.add(new EmptySpace(new TerminalSize(2, 1)));
        children.add(new EmptySpace(new TerminalSize(3, 1)));
        assertEquals(new TerminalSize(5, 1), manager.getPreferredSize(children));
        manager.doLayout(new TerminalSize(10, 1), children);
        assertEquals(new TerminalSize(2, 1), children.get(0).getSize());
        assertEquals(new TerminalSize(3, 1), children.get(1).getSize());
    }

    @Test
    void interfaceContractIsSatisfiedByBorderLayout() {
        LayoutManager manager = new BorderLayout();
        var center = new EmptySpace(new TerminalSize(2, 1));
        var north = new EmptySpace(new TerminalSize(4, 1));
        center.setLayoutData(new BorderLayout.BorderLayoutData(BorderLayout.Region.CENTER));
        north.setLayoutData(new BorderLayout.BorderLayoutData(BorderLayout.Region.NORTH));
        List<Component> children = List.of(center, north);
        assertTrue(manager.getPreferredSize(children).columns() >= 4);
        manager.doLayout(new TerminalSize(8, 4), children);
        assertEquals(new TerminalSize(8, 3), center.getSize());
    }

    @Test
    void layoutDataIsMarkerInterface() {
        LayoutData data = new LinearLayout.LinearLayoutData();
        assertNotNull(data);
    }
}
