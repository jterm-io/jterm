package io.jterm.core.input;

/** Immutable keyboard event. */
public record KeyStroke(KeyType type, char character, boolean ctrl, boolean alt, boolean shift) {

    public KeyStroke(KeyType type) {
        this(type, '\0', false, false, false);
    }

    public static KeyStroke character(char c, boolean ctrl, boolean alt, boolean shift) {
        return new KeyStroke(KeyType.CHARACTER, c, ctrl, alt, shift);
    }

    public boolean isCharacter() { return type == KeyType.CHARACTER; }
}
