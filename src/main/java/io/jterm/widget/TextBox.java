package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;

/** Single-line text input. */
public class TextBox extends AbstractComponent {
    private volatile String value = "";
    private volatile int cursorPosition = 0;
    private volatile int viewportOffset = 0;
    private volatile int preferredColumns = 20;
    private volatile boolean masked = false;
    private volatile boolean forceUppercase = false;
    private Color backgroundColorOverride = null;
    private String placeholder = null;

    public TextBox() {}
    public TextBox(int columns) { this.preferredColumns = columns; }

    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = forceUppercase ? value.toUpperCase() : value;
        cursorPosition = Math.min(cursorPosition, value.length());
        viewportOffset = Math.min(viewportOffset, value.length());
        invalidate();
    }

    /** Returns true if characters are drawn as {@code '*'} instead of real text. */
    public boolean isMasked() { return masked; }

    /** Enables or disables password masking. Invalidates the component. */
    public void setMasked(boolean masked) {
        this.masked = masked;
        invalidate();
    }

    /** Returns true if typed characters are forced to uppercase. */
    public boolean isForceUppercase() { return forceUppercase; }

    /** When enabled, all typed characters and setValue input are forced to uppercase. */
    public void setForceUppercase(boolean force) {
        this.forceUppercase = force;
        if (force) value = value.toUpperCase();
        invalidate();
    }

    /** Returns the background color override, or null if none set (uses theme background). */
    public Color getBackgroundColorOverride() { return backgroundColorOverride; }

    /** Sets a background color override for this text box. Pass null to revert to theme background. */
    public void setBackgroundColorOverride(Color color) {
        this.backgroundColorOverride = color;
        invalidate();
    }

    /** Sets placeholder text shown in dim color when the field is empty. */
    public void setPlaceholder(String text) {
        this.placeholder = text;
        invalidate();
    }

    public String getPlaceholder() { return placeholder; }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(preferredColumns, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        Color bg = backgroundColorOverride != null ? backgroundColorOverride : theme.background();
        var style = new TextCell(' ', theme.foreground(), bg);
        graphics.fillRectangle(0, 0, size.columns(), 1, style);
        // Defensive: clamp viewportOffset in case it's stale from a prior longer value.
        int offset = Math.min(viewportOffset, value.length());
        var visible = value.substring(offset, Math.min(value.length(), offset + size.columns()));
        var display = masked ? "*".repeat(visible.length()) : visible;
        graphics.drawString(0, 0, display, style);
        // Show placeholder text in dim color when empty and not focused
        if (value.isEmpty() && placeholder != null && !placeholder.isEmpty()) {
            var phStyle = new TextCell(' ', AnsiColor.BRIGHT_BLACK, bg);
            int phLen = Math.min(placeholder.length(), size.columns());
            graphics.drawString(0, 0, placeholder.substring(0, phLen), phStyle);
        }
        // Only draw cursor when this TextBox has focus
        if (isFocused()) {
            int cursorCol = cursorPosition - viewportOffset;
            if (cursorCol >= 0 && cursorCol < size.columns()) {
                char c = cursorPosition < value.length() ? (masked ? '*' : value.charAt(cursorPosition)) : ' ';
                // Swap fg/bg directly instead of using SGR.REVERSE.
                // Some CP437 clients (MuffinTerm) don't implement ESC[7m (REVERSE),
                // so we invert the colors explicitly. Every terminal supports
                // explicit fg/bg color codes.
                graphics.setCell(cursorCol, 0, new TextCell(c, bg, theme.foreground()));
            }
        }
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
        // Emacs-style key bindings (Ctrl+letter arrives as CHARACTER with ctrl=true)
        if (keyStroke.type() == KeyType.CHARACTER && keyStroke.ctrl()) {
            switch (keyStroke.character()) {
                case 'A', 'a' -> { cursorPosition = 0; adjustViewport(); return; }
                case 'E', 'e' -> { cursorPosition = value.length(); adjustViewport(); return; }
                case 'K', 'k' -> { value = value.substring(0, cursorPosition); invalidate(); adjustViewport(); return; }
                case 'F', 'f' -> { cursorPosition = Math.min(value.length(), cursorPosition + 1); adjustViewport(); return; }
                case 'B', 'b' -> { cursorPosition = Math.max(0, cursorPosition - 1); adjustViewport(); return; }
                case 'P', 'p' -> { cursorPosition = Math.max(0, cursorPosition - 1); adjustViewport(); return; }
                case 'N', 'n' -> { cursorPosition = Math.min(value.length(), cursorPosition + 1); adjustViewport(); return; }
                case 'D', 'd' -> { deleteForwardTextBox(); return; }
                default -> { return; }  // Ignore other Ctrl+letter combos
            }
        }
        switch (keyStroke.type()) {
            case CHARACTER -> {
                char ch = keyStroke.character();
                if (forceUppercase) ch = Character.toUpperCase(ch);
                value = value.substring(0, cursorPosition) + ch + value.substring(cursorPosition);
                cursorPosition++;
                invalidate();
            }
            case BACKSPACE -> {
                if (cursorPosition > 0) {
                    value = value.substring(0, cursorPosition - 1) + value.substring(cursorPosition);
                    cursorPosition--;
                    invalidate();
                }
            }
            case DELETE -> deleteForwardTextBox();
            case ARROW_LEFT -> cursorPosition = Math.max(0, cursorPosition - 1);
            case ARROW_RIGHT -> cursorPosition = Math.min(value.length(), cursorPosition + 1);
            case HOME -> cursorPosition = 0;
            case END -> cursorPosition = value.length();
            default -> {}
        }
        adjustViewport();
    }

    /** Delete the character at the cursor position (forward delete, emacs Ctrl+D / Delete key). */
    private void deleteForwardTextBox() {
        if (cursorPosition < value.length()) {
            value = value.substring(0, cursorPosition) + value.substring(cursorPosition + 1);
            invalidate();
        }
        adjustViewport();
    }

    private void adjustViewport() {
        var size = getSize().columns();
        if (cursorPosition < viewportOffset) viewportOffset = cursorPosition;
        if (cursorPosition >= viewportOffset + size) viewportOffset = cursorPosition - size + 1;
        if (viewportOffset < 0) viewportOffset = 0;
    }
}
