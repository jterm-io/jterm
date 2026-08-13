package io.jterm.completion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A trie (prefix tree) data structure for efficient prefix-based lookups.
 *
 * <p>Supports O(k) prefix lookups, where k is the length of the prefix,
 * regardless of the number of words stored. This makes it suitable for
 * word lists that may grow to 50k+ entries.
 *
 * <p>All operations are case-insensitive — words are normalized to
 * lowercase on insertion and lookup. Returned words are always lowercase.
 *
 * <p>The trie is designed to be reusable across multiple completion sources
 * (English words, usernames, board names, etc.). Each source can maintain
 * its own {@code Trie} instance.
 *
 * @since 0.1.0
 */
public class Trie {

    /** Root node of the trie. */
    private final Node root;
    /** Number of unique words stored. */
    private int size;

    /**
     * Creates an empty trie.
     */
    public Trie() {
        this.root = new Node();
        this.size = 0;
    }

    /**
     * Inserts a word into the trie. The word is normalized to lowercase.
     * Duplicate insertions are silently ignored.
     *
     * @param word the word to insert
     */
    public void insert(String word) {
        if (word == null) {
            return;
        }
        String normalized = word.toLowerCase();
        Node current = root;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            current = current.children.computeIfAbsent(c, k -> new Node());
        }
        if (!current.terminal) {
            current.terminal = true;
            size++;
        }
    }

    /**
     * Returns {@code true} if the word has been inserted into the trie.
     * Lookup is case-insensitive.
     *
     * @param word the word to check
     * @return true if the word exists in the trie
     */
    public boolean contains(String word) {
        if (word == null) {
            return false;
        }
        Node node = findNode(word.toLowerCase());
        return node != null && node.terminal;
    }

    /**
     * Returns {@code true} if any inserted word starts with the given prefix.
     * Lookup is case-insensitive.
     *
     * @param prefix the prefix to check
     * @return true if the prefix exists in the trie (i.e., some word starts with it)
     */
    public boolean isValidPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        if (prefix.isEmpty()) {
            return size > 0;
        }
        return findNode(prefix.toLowerCase()) != null;
    }

    /**
     * Returns the shortest word in the trie that starts with the given prefix.
     * The returned word is lowercase. If the prefix itself is a terminal word,
     * that word is returned (since it's the shortest possible match).
     *
     * <p>Lookup is case-insensitive.
     *
     * @param prefix the prefix to complete
     * @return the shortest matching word, or {@code null} if no word starts
     *         with the given prefix
     */
    public String shortestCompletion(String prefix) {
        if (prefix == null) {
            return null;
        }
        String normalized = prefix.toLowerCase();
        Node start = findNode(normalized);
        if (start == null) {
            return null;
        }
        // BFS to find the shortest terminal descendant (including start itself)
        // Use a simple level-order approach: process nodes at increasing depth
        List<NodeWithPrefix> currentLevel = new ArrayList<>();
        currentLevel.add(new NodeWithPrefix(start, normalized));
        while (!currentLevel.isEmpty()) {
            List<NodeWithPrefix> nextLevel = new ArrayList<>();
            for (NodeWithPrefix entry : currentLevel) {
                if (entry.node.terminal) {
                    return entry.prefix;
                }
                for (Map.Entry<Character, Node> child : entry.node.children.entrySet()) {
                    nextLevel.add(new NodeWithPrefix(child.getValue(),
                            entry.prefix + child.getKey()));
                }
            }
            currentLevel = nextLevel;
        }
        return null;
    }

    /**
     * Returns all words in the trie that start with the given prefix.
     * All returned words are lowercase. Lookup is case-insensitive.
     *
     * @param prefix the prefix to search for; empty string returns all words
     * @return a list of all matching words (never {@code null}, may be empty)
     */
    public List<String> getAllWithPrefix(String prefix) {
        if (prefix == null) {
            return new ArrayList<>();
        }
        String normalized = prefix.toLowerCase();
        Node start = findNode(normalized);
        if (start == null) {
            return new ArrayList<>();
        }
        List<String> results = new ArrayList<>();
        collectWords(start, normalized, results);
        return results;
    }

    /**
     * Returns the number of unique words stored in the trie.
     *
     * @return the word count
     */
    public int size() {
        return size;
    }

    // ── Internal helpers ───────────────────────────────────────────

    /**
     * Walks the trie to find the node corresponding to the given (lowercase)
     * string, or returns {@code null} if the path doesn't exist.
     *
     * @param key the lowercase string to walk
     * @return the node at the end of the path, or {@code null}
     */
    private Node findNode(String key) {
        Node current = root;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            current = current.children.get(c);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Recursively collects all terminal words descending from the given node.
     *
     * @param node    the starting node
     * @param prefix  the string built so far to reach this node
     * @param results the list to append found words to
     */
    private void collectWords(Node node, String prefix, List<String> results) {
        if (node.terminal) {
            results.add(prefix);
        }
        for (Map.Entry<Character, Node> child : node.children.entrySet()) {
            collectWords(child.getValue(), prefix + child.getKey(), results);
        }
    }

    // ── Inner classes ──────────────────────────────────────────────

    /**
     * A node in the trie.
     */
    private static class Node {
        /** Child nodes keyed by character. */
        final Map<Character, Node> children = new HashMap<>();
        /** Whether this node marks the end of a valid word. */
        boolean terminal = false;
    }

    /**
     * Helper for BFS shortest-completion: pairs a node with the prefix string
     * built so far to reach it.
     */
    private record NodeWithPrefix(Node node, String prefix) {}
}