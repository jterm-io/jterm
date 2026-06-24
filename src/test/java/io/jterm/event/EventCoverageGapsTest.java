package io.jterm.event;

import io.jterm.widget.Button;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers public FocusManager methods with zero line coverage.
 */
class EventCoverageGapsTest {

    @Test
    void focusManagerRemoveListener() {
        var manager = new FocusManager();
        var button = new Button("x");
        AtomicInteger count = new AtomicInteger();
        Listener<io.jterm.widget.Component> listener = c -> count.incrementAndGet();
        manager.addListener(listener);
        manager.setFocusedComponent(button);
        assertEquals(1, count.get());
        manager.removeListener(listener);
        manager.clearFocus();
        assertEquals(1, count.get(), "removed listener should not be notified");
    }
}
