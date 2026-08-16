package io.jterm.completion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads the spellcheck dictionary at startup.
 *
 * <p>The primary source is the system dictionary at
 * {@code /usr/share/dict/words} (available on macOS and most Linux
 * distributions, typically containing 200K+ words). If the system
 * dictionary is not available, the loader falls back to the bundled
 * resource {@code /io/jterm/completion/english_words.txt} which contains
 * ~1000 common English words.
 *
 * <p>All words are lowercased and stored in a {@link HashSet} for O(1)
 * lookup. The returned {@link SpellcheckDictionary} indicates whether it
 * was loaded from the fallback resource via {@link SpellcheckDictionary#isFallback()}.
 *
 * @since 0.1.0
 */
public class DictionaryLoader {

    /** Path to the system dictionary (macOS/Linux). */
    private static final String SYSTEM_DICT_PATH = "/usr/share/dict/words";

    /** Path to the fallback bundled resource. */
    private static final String FALLBACK_RESOURCE = "/io/jterm/completion/english_words.txt";

    /**
     * Loads the spellcheck dictionary. Tries {@code /usr/share/dict/words}
     * first, falls back to the bundled {@code english_words.txt} resource.
     * All words are lowercased and stored in a {@link HashSet} for O(1)
     * lookup.
     *
     * @return a {@link SpellcheckDictionary} with the loaded words
     */
    public static SpellcheckDictionary load() {
        try {
            SpellcheckDictionary dict = loadFromFile(SYSTEM_DICT_PATH);
            if (dict.size() > 0) {
                return dict;
            }
        } catch (Exception e) {
            // System dict not available — fall through to resource
        }
        return loadFromResource(FALLBACK_RESOURCE);
    }

    /**
     * Loads the dictionary from a specific file path. Words are lowercased
     * and blank lines are skipped. If the file does not exist or cannot be
     * read, an empty dictionary is returned.
     *
     * @param path the file path to load from
     * @return a {@link SpellcheckDictionary} with the loaded words (not fallback)
     */
    public static SpellcheckDictionary loadFromFile(String path) {
        Set<String> words = new HashSet<>();
        Path file = Paths.get(path);
        if (!Files.exists(file)) {
            return new SpellcheckDictionary(words, false);
        }
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.filter(line -> !line.isBlank())
                 .map(String::trim)
                 .map(String::toLowerCase)
                 .forEach(words::add);
        } catch (IOException e) {
            // Return what we have (possibly empty)
        }
        return new SpellcheckDictionary(words, false);
    }

    /**
     * Loads the dictionary from a classpath resource. Words are lowercased
     * and blank lines are skipped. The resulting dictionary is marked as
     * fallback ({@code isFallback() == true}).
     *
     * @param resourcePath the classpath resource path (e.g.
     *                     {@code "/io/jterm/completion/english_words.txt"})
     * @return a {@link SpellcheckDictionary} with the loaded words (fallback)
     * @throws IllegalStateException if the resource cannot be found or read
     */
    public static SpellcheckDictionary loadFromResource(String resourcePath) {
        Set<String> words = new HashSet<>();
        try (var is = DictionaryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Dictionary resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
                 Stream<String> lines = reader.lines()) {
                lines.filter(line -> !line.isBlank())
                     .map(String::trim)
                     .map(String::toLowerCase)
                     .forEach(words::add);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dictionary resource: " + resourcePath, e);
        }
        return new SpellcheckDictionary(words, true);
    }
}