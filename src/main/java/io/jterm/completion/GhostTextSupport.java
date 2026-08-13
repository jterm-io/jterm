package io.jterm.completion;

import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.TextCell;

/**
 * Shared rendering and acceptance logic for inline ghost-text completions
 * used by both {@link io.jterm.widget.TextBox} and {@link io.jterm.widget.TextArea}.
 *
 * <p>A widget attaches a {@link CompletionProvider} and delegates to this class
 * for two concerns:
 * <ul>
 *   <li><b>Suggestion management</b> — after each keystroke, the widget calls
 *       {@link #refresh(String, int)} with the current text and cursor position.
 *       If the provider returns a non-null suffix, it is stored as the active
 *       ghost text.</li>
 *   <li><b>Rendering</b> — during {@code drawComponent}, the widget calls
 *       {@link #drawGhostText(TextGraphics, int, int, int, Color)} to paint
 *       the suggestion suffix in dim color after the cursor position.</li>
 * </ul>
 *
 * <p>Key acceptance (Space or Tab inserts the suffix) is handled by the widget
 * delegating to {@link #tryAccept(char)} or {@link #tryAcceptTab()} before its
 * own keystroke processing.
 *
 * @since 0.1.0
 */
public class GhostTextSupport {

    private CompletionProvider provider;
    private String ghostText = null;

    /**
     * Creates a GhostTextSupport with no provider attached.
     */
    public GhostTextSupport() {}

    /**
     * Creates a GhostTextSupport with the given completion provider.
     *
     * @param provider the completion provider, or {@code null} for no completions
     */
    public GhostTextSupport(CompletionProvider provider) {
        this.provider = provider;
    }

    /**
     * Returns the currently attached completion provider, or {@code null} if none.
     *
     * @return the completion provider
     */
    public CompletionProvider getProvider() {
        return provider;
    }

    /**
     * Attaches or detaches a completion provider.
     *
     * @param provider the completion provider, or {@code null} to disable
     */
    public void setProvider(CompletionProvider provider) {
        this.provider = provider;
        clear();
    }

    /**
     * Returns the current ghost text suffix, or {@code null} if no suggestion is active.
     *
     * @return the active ghost text suffix
     */
    public String getGhostText() {
        return ghostText;
    }

    /**
     * Returns {@code true} if a ghost text suggestion is currently active.
     *
     * @return true if ghost text is showing
     */
    public boolean hasGhostText() {
        return ghostText != null && !ghostText.isEmpty();
    }

    /**
     * Clears the current ghost text suggestion without accepting it.
     */
    public void clear() {
        ghostText = null;
    }

    /**
     * Queries the completion provider for a suggestion and updates the ghost text.
     *
     * <p>Called by the widget after every keystroke that modifies text or moves
     * the cursor. If the provider returns {@code null} or an empty string, the
     * ghost text is cleared.
     *
     * @param text      the current text content visible to the provider
     * @param cursorPos the zero-based cursor position within the text
     */
    public void refresh(String text, int cursorPos) {
        if (provider == null) {
            ghostText = null;
            return;
        }
        String suggestion = provider.suggest(text, cursorPos);
        if (suggestion == null || suggestion.isEmpty()) {
            ghostText = null;
        } else {
            ghostText = suggestion;
        }
    }

    /**
     * Attempts to accept the ghost text with a typed character.
     *
     * <p>If the typed character is a space and ghost text is active, the suffix
     * is accepted (appended at the cursor). The space is consumed and not
     * inserted as a literal character. Any other character dismisses the ghost
     * text and is processed normally by the widget.
     *
     * @param ch the character that was typed
     * @return the suffix to insert if accepted, or {@code null} if not accepted
     */
    public String tryAccept(char ch) {
        if (hasGhostText() && ch == ' ') {
            String accepted = ghostText;
            ghostText = null;
            return accepted;
        }
        // Any other character dismisses the ghost text
        if (hasGhostText()) {
            ghostText = null;
        }
        return null;
    }

    /**
     * Attempts to accept the ghost text via the Tab key.
     *
     * @return the suffix to insert if accepted, or {@code null} if not accepted
     */
    public String tryAcceptTab() {
        if (hasGhostText()) {
            String accepted = ghostText;
            ghostText = null;
            return accepted;
        }
        return null;
    }

    /**
     * Draws the ghost text suffix at the given screen position in dim color.
     *
     * <p>This is used by single-line widgets (TextBox) where the ghost text
     * starts immediately after the cursor on the same row.
     *
     * @param graphics   the graphics target
     * @param startCol   the screen column where ghost text begins
     * @param row        the screen row for the ghost text
     * @param maxColumns the maximum number of columns available for ghost text
     * @param bg         the background color to use (should match the widget's bg)
     */
    public void drawGhostText(TextGraphics graphics, int startCol, int row, int maxColumns, Color bg) {
        if (!hasGhostText()) return;
        var ghostStyle = new TextCell(' ', AnsiColor.BRIGHT_BLACK, bg);
        int col = startCol;
        int charsToDraw = Math.min(ghostText.length(), maxColumns - startCol);
        for (int i = 0; i < charsToDraw && col < maxColumns; i++) {
            char c = ghostText.charAt(i);
            graphics.setCell(col, row, ghostStyle.withCharacter(c));
            col++;
        }
    }

    /**
     * Draws the ghost text suffix starting at the given screen position, with
     * support for multi-line wrapping.
     *
     * <p>This is used by multi-line widgets (TextArea) where the ghost text may
     * need to wrap across multiple rows if it extends beyond the end of the
     * current line's visible area.
     *
     * @param graphics    the graphics target
     * @param startCol    the screen column where ghost text begins
     * @param startRow    the screen row where ghost text begins
     * @param totalCols   the total number of columns in the widget
     * @param totalRows   the total number of rows in the widget
     * @param bg          the background color to use
     */
    public void drawGhostTextMultiLine(TextGraphics graphics, int startCol, int startRow,
                                        int totalCols, int totalRows, Color bg) {
        if (!hasGhostText()) return;
        var ghostStyle = new TextCell(' ', AnsiColor.BRIGHT_BLACK, bg);
        int col = startCol;
        int row = startRow;
        for (int i = 0; i < ghostText.length(); i++) {
            if (col >= totalCols) {
                col = 0;
                row++;
                if (row >= totalRows) break;
            }
            char c = ghostText.charAt(i);
            graphics.setCell(col, row, ghostStyle.withCharacter(c));
            col++;
        }
    }
}