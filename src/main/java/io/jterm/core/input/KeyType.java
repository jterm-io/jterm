package io.jterm.core.input;

/**
 * The kind of input event produced by the {@link InputDecoder}.
 *
 * <p>Every key press decodes to a {@code KeyType} plus, for
 * {@link #CHARACTER}, the actual character in the accompanying
 * {@link KeyStroke}.
 */
public enum KeyType {
    /** A printable or control character; the value is carried by the KeyStroke. */
    CHARACTER,

    /** Up arrow key. */
    ARROW_UP,

    /** Down arrow key. */
    ARROW_DOWN,

    /** Left arrow key. */
    ARROW_LEFT,

    /** Right arrow key. */
    ARROW_RIGHT,

    /** Enter/Return key. */
    ENTER,

    /** Escape key. */
    ESCAPE,

    /** Tab key. */
    TAB,

    /** Backspace key. */
    BACKSPACE,

    /** Delete/forward-delete key. */
    DELETE,

    /** Home key (or Ctrl-A on terminals without a dedicated key). */
    HOME,

    /** End key (or Ctrl-E on terminals without a dedicated key). */
    END,

    /** Page up key. */
    PAGE_UP,

    /** Page down key. */
    PAGE_DOWN,

    /** Insert key. */
    INSERT,

    /** Function key F1. */
    F1,

    /** Function key F2. */
    F2,

    /** Function key F3. */
    F3,

    /** Function key F4. */
    F4,

    /** Function key F5. */
    F5,

    /** Function key F6. */
    F6,

    /** Function key F7. */
    F7,

    /** Function key F8. */
    F8,

    /** Function key F9. */
    F9,

    /** Function key F10. */
    F10,

    /** Function key F11. */
    F11,

    /** Function key F12. */
    F12,

    /** End of input: the input stream was closed (e.g. Ctrl-D on EOF). */
    EOF,

    /** Input that could not be decoded into any known key type. */
    UNKNOWN
}