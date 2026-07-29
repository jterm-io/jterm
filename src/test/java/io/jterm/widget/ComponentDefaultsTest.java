package io.jterm.widget;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.layout.LayoutData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the default methods on {@link Component} that are not overridden by
 * abstract helper classes. A minimal anonymous implementation is used.
 */
class ComponentDefaultsTest {

    private Component newComponent() {
        return new Component() {
            private TerminalSize size = new TerminalSize(1, 1);
            private TerminalPosition position = new TerminalPosition(0, 0);
            private Component parent;
            private boolean visible = true;
            private boolean focused;
            private LayoutData data;

            @Override public TerminalSize getSize() { return size; }
            @Override public TerminalPosition getPosition() { return position; }
            @Override public TerminalSize getPreferredSize() { return size; }
            @Override public void setBounds(TerminalPosition p, TerminalSize s) { this.position = p; this.size = s; }
            @Override public void draw(TextGraphics graphics) { }
            @Override public void invalidate() { }
            @Override public boolean isVisible() { return visible; }
            @Override public void setVisible(boolean v) { this.visible = v; }
            @Override public LayoutData getLayoutData() { return data; }
            @Override public void setLayoutData(LayoutData d) { this.data = d; }
            @Override public Component getParent() { return parent; }
            @Override public void setParent(Component p) { this.parent = p; }
            @Override public boolean handleKeyStroke(io.jterm.core.input.KeyStroke keyStroke) { return false; }
            @Override public boolean isFocused() { return focused; }
            @Override public void setFocused(boolean f) { this.focused = f; }
        };
    }

    @Test
    void defaultFocusableIsTrue() {
        assertTrue(newComponent().isFocusable());
    }

    @Test
    void defaultSetFocusableIsNoOp() {
        var c = newComponent();
        assertDoesNotThrow(() -> c.setFocusable(false));
        assertTrue(c.isFocusable());
    }
}
