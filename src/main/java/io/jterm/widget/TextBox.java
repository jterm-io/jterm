package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

/** Single-line text input. */
public class TextBox extends AbstractComponent {
    private String value = "";
    private int cursorPosition = 0;
    private int viewportOffset = 0;
    private int preferredColumns = 20;

    public TextBox() {}
    public TextBox(int columns) { this.preferredColumns = columns; }

    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = value;
        cursorPosition = Math.min(cursorPosition, value.length());
        invalidate();
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        return new TerminalSize(preferredColumns, 1);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var style = new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
        graphics.fillRectangle(0, 0, size.columns(), 1, style);
        var visible = value.substring(viewportOffset, Math.min(value.length(), viewportOffset + size.columns()));
        graphics.drawString(0, 0, visible, style);
        int cursorCol = cursorPosition - viewportOffset;
        if (cursorCol >= 0 && cursorCol < size.columns()) {
            char c = cursorPosition < value.length() ? value.charAt(cursorPosition) : ' ';
            graphics.setCell(cursorCol, 0, new TextCell(c, AnsiColor.BLACK, AnsiColor.WHITE));
        }
    }

    @Override
    public void handleKeyStroke(KeyStroke keyStroke) {
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
