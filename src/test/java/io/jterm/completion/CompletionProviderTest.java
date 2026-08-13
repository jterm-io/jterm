package io.jterm.completion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link CompletionProvider} strategy interface.
 */
class CompletionProviderTest {

    @Test
    void providerReturningNullMeansNoSuggestion() {
        CompletionProvider provider = (text, cursorPos) -> null;
        assertNull(provider.suggest("he", 2));
    }

    @Test
    void providerReturningSuffixMeansSuggestion() {
        CompletionProvider provider = (text, cursorPos) -> "llo";
        assertEquals("llo", provider.suggest("he", 2));
    }

    @Test
    void suggestReceivesTextAndCursorPosition() {
        // A provider that echoes the cursor position as a string for testing
        CompletionProvider provider = (text, cursorPos) -> String.valueOf(cursorPos);
        assertEquals("3", provider.suggest("abc", 3));
    }
}