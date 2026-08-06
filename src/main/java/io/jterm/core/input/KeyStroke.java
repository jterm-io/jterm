package io.jterm.core.input;

/** Immutable keyboard event. */
public record KeyStroke(KeyType type, char character, boolean ctrl, boolean alt, boolean shift) {

    /** Creates a keystroke with the given type and no character or modifiers. @param type the key type */
    public KeyStroke(KeyType type) {
        this(type, '\0', false, false, false);
    }

    /** Creates a CHARACTER keystroke with the given modifiers. @param c the character @param ctrl true if Ctrl held @param alt true if Alt held @param shift true if Shift held @return the keystroke */
    public static KeyStroke character(char c, boolean ctrl, boolean alt, boolean shift) {
        return new KeyStroke(KeyType.CHARACTER, c, ctrl, alt, shift);
    }

    /** Returns true if this keystroke is a CHARACTER type. @return true if a character */
    public boolean isCharacter() { return type == KeyType.CHARACTER; }
}
