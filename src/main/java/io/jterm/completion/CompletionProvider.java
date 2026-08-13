package io.jterm.completion;

/**
 * Strategy interface for providing inline text completions (ghost text).
 *
 * <p>A completion provider examines the current text buffer and cursor position
 * and returns a suggested completion suffix to append at the cursor, or
 * {@code null} if no suggestion is available. The suffix is rendered as dim
 * "ghost text" after the cursor in a {@link io.jterm.widget.TextBox}. The user
 * can accept the suggestion by pressing Space or Tab, or dismiss it by typing
 * any other key.
 *
 * <p>Implementations should be lightweight and fast — the provider is queried
 * after every keystroke when attached to a text input widget.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface CompletionProvider {

    /**
     * Suggest a completion suffix for the given text and cursor position.
     *
     * @param text      the current text buffer content
     * @param cursorPos the zero-based cursor position within the text
     * @return the suffix to append at the cursor position, or {@code null}
     *         if no suggestion is available
     */
    String suggest(String text, int cursorPos);
}