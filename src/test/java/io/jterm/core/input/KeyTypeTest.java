package io.jterm.core.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyTypeTest {
    @Test
    void allExpectedValuesExist() {
        assertTrue(KeyType.values().length >= 17);
        assertNotNull(KeyType.CHARACTER);
        assertNotNull(KeyType.ARROW_UP);
        assertNotNull(KeyType.ARROW_DOWN);
        assertNotNull(KeyType.ARROW_LEFT);
        assertNotNull(KeyType.ARROW_RIGHT);
        assertNotNull(KeyType.ENTER);
        assertNotNull(KeyType.ESCAPE);
        assertNotNull(KeyType.TAB);
        assertNotNull(KeyType.BACKSPACE);
        assertNotNull(KeyType.DELETE);
        assertNotNull(KeyType.HOME);
        assertNotNull(KeyType.END);
        assertNotNull(KeyType.PAGE_UP);
        assertNotNull(KeyType.PAGE_DOWN);
        assertNotNull(KeyType.INSERT);
        assertNotNull(KeyType.EOF);
        assertNotNull(KeyType.UNKNOWN);
    }

    @Test
    void functionKeysF1ToF12Exist() {
        assertNotNull(KeyType.F1);
        assertNotNull(KeyType.F12);
    }

    @Test
    void valueOfRoundTrip() {
        for (var keyType : KeyType.values()) {
            assertEquals(keyType, KeyType.valueOf(keyType.name()));
        }
    }
}
