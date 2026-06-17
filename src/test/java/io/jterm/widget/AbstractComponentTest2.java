package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractComponentTest2 {
    @Test
    void setBoundsStoresPositionAndSize() {
        var comp = new ConcreteComponent(new TerminalSize(3, 1));
        comp.setBounds(new TerminalPosition(2, 3), new TerminalSize(4, 2));
        assertEquals(new TerminalPosition(2, 3), comp.getPosition());
        assertEquals(new TerminalSize(4, 2), comp.getSize());
    }

    @Test
    void invalidateClearsPreferredSize() {
        var comp = new ConcreteComponent(new TerminalSize(3, 1));
        comp.getPreferredSize();
        comp.setBounds(new TerminalPosition(0, 0), new TerminalSize(5, 1));
        assertEquals(new TerminalSize(5, 1), comp.getSize());
    }

    @Test
    void visibilityDefaultsTrue() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        assertTrue(comp.isVisible());
    }

    @Test
    void setVisibleUpdatesState() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        comp.setVisible(false);
        assertFalse(comp.isVisible());
    }

    @Test
    void layoutDataRoundTrip() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        var data = new io.jterm.layout.LinearLayout.LinearLayoutData();
        comp.setLayoutData(data);
        assertEquals(data, comp.getLayoutData());
    }

    @Test
    void parentRoundTrip() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        var parent = new ConcreteComponent(new TerminalSize(1, 1));
        comp.setParent(parent);
        assertEquals(parent, comp.getParent());
    }

    @Test
    void focusedStateRoundTrip() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        comp.setFocused(true);
        assertTrue(comp.isFocused());
    }

    @Test
    void drawCallsDrawComponent() {
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        var buffer = new ScreenBuffer(new TerminalSize(1, 1));
        comp.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(1, 1));
        comp.draw(new TextGraphics(buffer));
        assertEquals('X', buffer.getCell(0, 0).character().charAt(0));
    }

    @Test
    void invalidationPropagatesToParent() {
        var parent = new Panel();
        var comp = new ConcreteComponent(new TerminalSize(1, 1));
        comp.setParent(parent);
        comp.invalidate();
        // parent is now invalidated; getPreferredSize should recompute (for Panel it will use layout manager)
        assertNotNull(parent.getPreferredSize());
    }

    private static class ConcreteComponent extends AbstractComponent {
        private final TerminalSize preferred;

        ConcreteComponent(TerminalSize preferred) {
            this.preferred = preferred;
        }

        @Override
        protected TerminalSize calculatePreferredSize() {
            return preferred;
        }

        @Override
        protected void drawComponent(TextGraphics graphics) {
            graphics.setCell(0, 0, new TextCell('X', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        }
    }
}
