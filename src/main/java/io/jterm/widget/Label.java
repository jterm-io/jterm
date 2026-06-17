package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.TerminalTextUtils;

/** Displays styled text. */
public class Label extends AbstractComponent {
    private String text;
    private TextCell style;

    public Label(String text) {
        this(text, AnsiColor.DEFAULT, AnsiColor.DEFAULT);
    }

    public Label(String text, io.jterm.style.Color fg, io.jterm.style.Color bg) {
        this.text = text;
        this.style = new TextCell(' ', fg, bg);
    }

    public Label(String text, TextCell style) {
        this.text = text;
        this.style = style;
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public String getText() { return text; }

    public void setForeground(io.jterm.style.Color color) {
        this.style = style.withForeground(color);
        invalidate();
    }

    public void setBackground(io.jterm.style.Color color) {
        this.style = style.withBackground(color);
        invalidate();
    }

    public void setStyle(TextCell style) {
        this.style = style;
        invalidate();
    }

    @Override
    protected TerminalSize calculatePreferredSize() {
        if (text == null || text.isEmpty()) return new TerminalSize(1, 1);
        int max = 0;
        for (var line : text.split("\n", -1)) {
            max = Math.max(max, TerminalTextUtils.getTrueWidth(line));
        }
        return new TerminalSize(max, text.split("\n", -1).length);
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var lines = text.split("\n", -1);
        var size = getSize();
        for (int i = 0; i < lines.length && i < size.rows(); i++) {
            var line = lines[i];
            int width = TerminalTextUtils.getTrueWidth(line);
            int x = 0;
            if (width < size.columns()) x = (size.columns() - width) / 2;
            graphics.drawString(x, i, line, style);
        }
    }
}
