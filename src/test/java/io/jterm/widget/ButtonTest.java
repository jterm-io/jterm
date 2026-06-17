package io.jterm.widget;

import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ButtonTest {
    @Test
    void clickFiresListener() {
        var button = new Button("OK");
        var fired = new boolean[1];
        button.addListener(() -> fired[0] = true);
        button.click();
        assertTrue(fired[0]);
    }

    @Test
    void enterKeyFiresClick() {
        var button = new Button("OK");
        var fired = new boolean[1];
        button.addListener(() -> fired[0] = true);
        button.handleKeyStroke(new KeyStroke(KeyType.ENTER));
        assertTrue(fired[0]);
    }
}
