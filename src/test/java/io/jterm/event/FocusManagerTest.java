package io.jterm.event;

import io.jterm.widget.Button;
import io.jterm.widget.Label;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FocusManagerTest {
    @Test
    void setFocusUpdatesComponent() {
        var fm = new FocusManager();
        var button = new Button("Click");
        fm.setFocusedComponent(button);
        assertEquals(button, fm.getFocusedComponent());
        assertTrue(button.isFocused());
    }

    @Test
    void focusChangeNotifiesListeners() {
        var fm = new FocusManager();
        var button = new Button("Click");
        var label = new Label("Text");
        AtomicReference<io.jterm.widget.Component> current = new AtomicReference<>();
        fm.addListener(current::set);

        fm.setFocusedComponent(button);
        assertEquals(button, current.get());

        fm.setFocusedComponent(label);
        assertEquals(label, current.get());
        assertFalse(button.isFocused());
        assertTrue(label.isFocused());
    }

    @Test
    void clearFocusRemovesFocus() {
        var fm = new FocusManager();
        var button = new Button("Click");
        fm.setFocusedComponent(button);
        fm.clearFocus();
        assertNull(fm.getFocusedComponent());
        assertFalse(button.isFocused());
    }

    @Test
    void duplicateFocusDoesNotReNotify() {
        var fm = new FocusManager();
        var button = new Button("Click");
        var count = new int[1];
        fm.addListener(c -> count[0]++);
        fm.setFocusedComponent(button);
        fm.setFocusedComponent(button);
        fm.setFocusedComponent(button);
        assertEquals(1, count[0]);
    }
}
