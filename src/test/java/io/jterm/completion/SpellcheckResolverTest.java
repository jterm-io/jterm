package io.jterm.completion;

import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpellcheckResolver}.
 */
class SpellcheckResolverTest {

    private SpellcheckDictionary createDictionary() {
        return new SpellcheckDictionary(Set.of("hello", "world", "don", "a", "i"));
    }

    private TextCell defaultStyle() {
        return new TextCell(' ', AnsiColor.WHITE, AnsiColor.BLACK);
    }

    // ── Valid text: no misspellings ─────────────────────────────

    @Test
    void validWordsProduceNullForAllChars() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("hello world");
        TextCell def = defaultStyle();
        for (int i = 0; i < 11; i++) {
            assertNull(resolver.resolveStyle(i, "hello world".charAt(i), def),
                    "char at " + i + " should be null (valid word)");
        }
    }

    @Test
    void mixedCaseValidWordProducesNull() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("HeLlO");
        TextCell def = defaultStyle();
        for (int i = 0; i < 5; i++) {
            assertNull(resolver.resolveStyle(i, "HeLlO".charAt(i), def),
                    "char at " + i + " should be null (valid mixed-case word)");
        }
    }

    // ── Misspelled text ──────────────────────────────────────────

    @Test
    void misspelledWordsProduceYellowForWordChars() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("helo wrld");
        TextCell def = defaultStyle();
        // "helo" = chars 0-3, should be yellow
        for (int i = 0; i < 4; i++) {
            TextCell cell = resolver.resolveStyle(i, "helo wrld".charAt(i), def);
            assertNotNull(cell, "char at " + i + " should be non-null (misspelled)");
            assertEquals(AnsiColor.YELLOW, cell.fg(),
                    "char at " + i + " should be yellow");
        }
        // space at index 4 should be null
        assertNull(resolver.resolveStyle(4, ' ', def),
                "space at 4 should be null");
        // "wrld" = chars 5-8, should be yellow
        for (int i = 5; i < 9; i++) {
            TextCell cell = resolver.resolveStyle(i, "helo wrld".charAt(i), def);
            assertNotNull(cell, "char at " + i + " should be non-null (misspelled)");
            assertEquals(AnsiColor.YELLOW, cell.fg(),
                    "char at " + i + " should be yellow");
        }
    }

    @Test
    void misspelledRangesCorrectForHeloWrld() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("helo wrld");
        List<int[]> ranges = resolver.getMisspelledRanges();
        assertEquals(2, ranges.size());
        // [0, 4) for "helo"
        assertArrayEquals(new int[]{0, 4}, ranges.get(0));
        // [5, 9) for "wrld"
        assertArrayEquals(new int[]{5, 9}, ranges.get(1));
    }

    @Test
    void isMisspelledCorrectForValidAndInvalid() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("helo world");
        // "helo" is misspelled (chars 0-3)
        assertTrue(resolver.isMisspelled(0));
        assertTrue(resolver.isMisspelled(3));
        // space at 4 is not misspelled
        assertFalse(resolver.isMisspelled(4));
        // "world" is valid (chars 5-9)
        assertFalse(resolver.isMisspelled(5));
        assertFalse(resolver.isMisspelled(9));
    }

    // ── Disabled state ───────────────────────────────────────────

    @Test
    void disabledReturnsNullForAllChars() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("helo wrld");
        resolver.setEnabled(false);
        assertFalse(resolver.isEnabled());
        TextCell def = defaultStyle();
        for (int i = 0; i < 9; i++) {
            assertNull(resolver.resolveStyle(i, "helo wrld".charAt(i), def),
                    "char at " + i + " should be null when disabled");
        }
    }

    @Test
    void enabledByDefault() {
        var resolver = new SpellcheckResolver(createDictionary());
        assertTrue(resolver.isEnabled());
    }

    // ── Edge cases ───────────────────────────────────────────────

    @Test
    void apostropheSplitsWordIntoValidAndSingleChar() {
        var resolver = new SpellcheckResolver(createDictionary());
        // "don" is valid, "t" is a single char — single-char words are always valid
        resolver.setText("don't");
        TextCell def = defaultStyle();
        for (int i = 0; i < 5; i++) {
            assertNull(resolver.resolveStyle(i, "don't".charAt(i), def),
                    "char at " + i + " should be null (don is valid, t is single char)");
        }
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void singleCharacterWordsAreAlwaysValid() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("a b c");
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void emptyTextProducesNoMisspelledRanges() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("");
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void nullTextProducesNoMisspelledRanges() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText(null);
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void digitsBreakWords() {
        var resolver = new SpellcheckResolver(createDictionary());
        // "hello" valid, "123" is digits (not letters, no word), "world" valid
        resolver.setText("hello123world");
        // "hello" chars 0-4 valid, "123" not letters, "world" chars 8-12 valid
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void hyphenBreaksWords() {
        var resolver = new SpellcheckResolver(createDictionary());
        // "hello" valid, "world" valid — hyphen breaks the word
        resolver.setText("hello-world");
        assertTrue(resolver.getMisspelledRanges().isEmpty());
    }

    @Test
    void onlyMisspelledWordsHighlighted() {
        var resolver = new SpellcheckResolver(createDictionary());
        resolver.setText("hello xyzqwf world");
        List<int[]> ranges = resolver.getMisspelledRanges();
        assertEquals(1, ranges.size());
        // "xyzqwf" starts at index 6, ends at 12
        assertArrayEquals(new int[]{6, 12}, ranges.get(0));
    }
}