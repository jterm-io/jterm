package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
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

    public TextBox() {}
    public TextBox(int columns) { this.preferredColumns = columns; }

    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = value;
        cursorPosition = Math.min(cursorPosition, value.length());
        invalidate();
    }

    /** Returns true if characters are drawn as {@code '*'} instead of real text. */
    public boolean isMasked() { return masked; }

    /** Enables or disables password masking. Invalidates the component. */
    public void setMasked(boolean masked) {
        this.masked = masked;
        invalidate();
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(preferredColumns, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var theme = ThemeManager.active();
        var style = new TextCell(' ', theme.foreground(), theme.background());
        graphics.fillRectangle(0, 0, size.columns(), 1, style);
        var visible = value.substring(viewportOffset, Math.min(value.length(), viewportOffset + size.columns()));
        var display = masked ? "*".repeat(visible.length()) : visible;
        graphics.drawString(0, 0, display, style);
        // Only draw cursor when this TextBox has focus
        if (isFocused()) {
            int cursorCol = cursorPosition - viewportOffset;
            if (cursorCol >= 0 && cursorCol < size.columns()) {
                char c = cursorPosition < value.length() ? (masked ? '*' : value.charAt(cursorPosition)) : ' ';
                graphics.setCell(cursorCol, 0, new TextCell(c, theme.selectionFg(), theme.selectionBg()));
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
                default -> { return; }  // Ignore other Ctrl+letter combos
            }
        }
        switch (keyStroke.type()) {
            case CHARACTER -> {
                value = value.substring(0, cursorPosition) + keyStroke.character() + value.substring(cursorPosition);
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
            case DELETE -> {
                if (cursorPosition < value.length()) {
                    value = value.substring(0, cursorPosition) + value.substring(cursorPosition + 1);
                    invalidate();
                }
            }
            case ARROW_LEFT -> cursorPosition = Math.max(0, cursorPosition - 1);
            case ARROW_RIGHT -> cursorPosition = Math.min(value.length(), cursorPosition + 1);
            case HOME -> cursorPosition = 0;
            case END -> cursorPosition = value.length();
            default -> {}
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
