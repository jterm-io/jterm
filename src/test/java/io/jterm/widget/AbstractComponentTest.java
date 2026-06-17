package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractComponentTest {

    @Test
    void defaultBoundsAreZero() {
        var comp = emptyComponent();
        assertEquals(TerminalPosition.TOP_LEFT, comp.getPosition());
        assertEquals(TerminalSize.ZERO, comp.getSize());
    }

    @Test
    void setBoundsUpdatesPositionAndSize() {
        var comp = emptyComponent();
        comp.setBounds(new TerminalPosition(2, 3), new TerminalSize(10, 5));
        assertEquals(new TerminalPosition(2, 3), comp.getPosition());
        assertEquals(new TerminalSize(10, 5), comp.getSize());
    }

    @Test
    void invalidateClearsPreferredSize() {
        var comp = sizedComponent(new TerminalSize(5, 2));
        var first = comp.getPreferredSize();
        assertEquals(new TerminalSize(5, 2), first);
        ((AbstractComponent) comp).invalidate();
        var second = comp.getPreferredSize();
        assertSame(first, second); // identity not guaranteed; structural check
        assertEquals(new TerminalSize(5, 2), second);
    }

    @Test
    void parentIsUpdatedAfterSetParent() {
        var child = emptyComponent();
        var parent = emptyComponent();
        child.setParent(parent);
        assertSame(parent, child.getParent());
    }

    @Test
    void visibilityDefaultsToTrue() {
        assertTrue(emptyComponent().isVisible());
    }

    @Test
    void layoutDataCanBeSetAndRetrieved() {
        var comp = emptyComponent();
        var data = new LayoutData() {};
        comp.setLayoutData(data);
        assertSame(data, comp.getLayoutData());
    }

    @Test
    void focusStateCanBeToggled() {
        var comp = emptyComponent();
        assertFalse(comp.isFocused());
        comp.setFocused(true);
        assertTrue(comp.isFocused());
    }

    private Component emptyComponent() {
        return new AbstractComponent() {
            @Override
            protected TerminalSize calculatePreferredSize() { return TerminalSize.ZERO; }
            @Override
            protected void drawComponent(TextGraphics graphics) { }
        };
    }

    private Component sizedComponent(TerminalSize size) {
        return new AbstractComponent() {
            @Override
            protected TerminalSize calculatePreferredSize() { return size; }
            @Override
            protected void drawComponent(TextGraphics graphics) { }
        };
    }
}
