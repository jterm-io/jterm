package io.jterm.completion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DictionaryLoader}.
 */
class DictionaryLoaderTest {

    @Test
    void loadFromFileLoadsWords(@TempDir Path tempDir) throws IOException {
        Path dictFile = tempDir.resolve("test_dict.txt");
        Files.writeString(dictFile, "hello\nworld\nfoo\nbar\n");
        SpellcheckDictionary dict = DictionaryLoader.loadFromFile(dictFile.toString());
        assertNotNull(dict);
        assertEquals(4, dict.size());
        assertTrue(dict.isValid("hello"));
        assertTrue(dict.isValid("world"));
        assertTrue(dict.isValid("HELLO")); // case-insensitive
        assertFalse(dict.isValid("xyzqwf"));
    }

    @Test
    void loadFromFileSkipsBlankLines(@TempDir Path tempDir) throws IOException {
        Path dictFile = tempDir.resolve("blanks.txt");
        Files.writeString(dictFile, "hello\n\nworld\n\n");
        SpellcheckDictionary dict = DictionaryLoader.loadFromFile(dictFile.toString());
        assertEquals(2, dict.size());
    }

    @Test
    void loadFromFileIsNotFallback(@TempDir Path tempDir) throws IOException {
        Path dictFile = tempDir.resolve("test_dict.txt");
        Files.writeString(dictFile, "hello\nworld\n");
        SpellcheckDictionary dict = DictionaryLoader.loadFromFile(dictFile.toString());
        assertFalse(dict.isFallback());
    }

    @Test
    void loadFromResourceLoadsBundledWords() {
        SpellcheckDictionary dict = DictionaryLoader.loadFromResource("/io/jterm/completion/english_words.txt");
        assertNotNull(dict);
        assertTrue(dict.size() > 100, "bundled word list should have > 100 words, got " + dict.size());
        assertTrue(dict.isValid("the"), "'the' should be in bundled dictionary");
        assertTrue(dict.isValid("world"), "'world' should be in bundled dictionary");
        assertFalse(dict.isValid("xyzqwf"));
    }

    @Test
    void loadFromResourceIsFallback() {
        SpellcheckDictionary dict = DictionaryLoader.loadFromResource("/io/jterm/completion/english_words.txt");
        assertTrue(dict.isFallback());
    }

    @Test
    void loadTriesSystemDictOrFallback() {
        // On macOS /usr/share/dict/words has ~236K words.
        // On other systems, it falls back to the bundled resource.
        SpellcheckDictionary dict = DictionaryLoader.load();
        assertNotNull(dict);
        assertTrue(dict.size() > 100, "load() should produce a usable dictionary, got " + dict.size());
        // "hello" should be valid in either the system dict or the bundled list
        assertTrue(dict.isValid("hello"));
    }

    @Test
    void loadFromNonExistentFileReturnsEmptyDictionary(@TempDir Path tempDir) {
        SpellcheckDictionary dict = DictionaryLoader.loadFromFile(tempDir.resolve("nonexistent.txt").toString());
        assertNotNull(dict);
        assertEquals(0, dict.size());
    }
}