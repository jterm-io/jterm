package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.ThemeManager;
import io.jterm.util.TerminalTextUtils;

/** Displays styled text. */
public class Label extends AbstractComponent {
    private String text;
    private TextCell style;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;

    public enum HorizontalAlignment { LEFT, CENTER, RIGHT }

    /**
     * Creates a label with the given text using default colors.
     *
     * @param text the label text
     */
    public Label(String text) {
        this(text, AnsiColor.DEFAULT, AnsiColor.DEFAULT);
    }

    /**
     * Creates a label with the given text and foreground/background colors.
     *
     * @param text the label text
     * @param fg   the foreground color
     * @param bg   the background color
     */
    public Label(String text, io.jterm.style.Color fg, io.jterm.style.Color bg) {
        this.text = text;
        this.style = new TextCell(' ', fg, bg);
    }

    /**
     * Creates a label with the given text and cell style.
     *
     * @param text  the label text
     * @param style the cell style (foreground, background, character)
     */
    public Label(String text, TextCell style) {
        this.text = text;
        this.style = style;
    }

    /**
     * Sets the label text and invalidates the component.
     *
     * @param text the new text
     */
    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    /**
     * Returns the current label text.
     *
     * @return the text
     */
    public String getText() { return text; }

    /**
     * Sets the foreground color and invalidates the component.
     *
     * @param color the new foreground color
     */
    public void setForeground(io.jterm.style.Color color) {
        this.style = style.withForeground(color);
        invalidate();
    }

    /**
     * Sets the background color and invalidates the component.
     *
     * @param color the new background color
     */
    public void setBackground(io.jterm.style.Color color) {
        this.style = style.withBackground(color);
        invalidate();
    }

    /**
     * Sets the full cell style and invalidates the component.
     *
     * @param style the new cell style
     */
    public void setStyle(TextCell style) {
        this.style = style;
        invalidate();
    }

    /** Set horizontal alignment of text within the label's bounds. */
    public void setHorizontalAlignment(HorizontalAlignment alignment) {
        this.horizontalAlignment = alignment;
        invalidate();
    }

    /** Computes preferred size from the widest line and line count. */
    @Override
    protected TerminalSize calculatePreferredSize() {
        if (text == null || text.isEmpty()) return new TerminalSize(1, 1);
        int max = 0;
        for (var line : text.split("\n", -1)) {
            max = Math.max(max, TerminalTextUtils.getTrueWidth(line));
        }
        return new TerminalSize(max, text.split("\n", -1).length);
    }

    /** Renders each line of text with horizontal alignment, falling back to theme colors when defaults are used. */
    @Override
    protected void drawComponent(TextGraphics graphics) {
        var lines = text.split("\\n", -1);
        var size = getSize();
        // If label uses DEFAULT colors, fall back to theme fg/bg so the
        // label inherits the themed background instead of terminal default.
        var effectiveStyle = style;
        if (style.fg() == AnsiColor.DEFAULT && style.bg() == AnsiColor.DEFAULT) {
            var theme = ThemeManager.active();
            effectiveStyle = new TextCell(' ', theme.foreground(), theme.background());
        }
        for (int i = 0; i < lines.length && i < size.rows(); i++) {
            var line = lines[i];
            int width = TerminalTextUtils.getTrueWidth(line);
            int x = 0;
            if (width < size.columns()) {
                switch (horizontalAlignment) {
                    case CENTER -> x = (size.columns() - width) / 2;
                    case RIGHT  -> x = size.columns() - width;
                    case LEFT   -> x = 0;
                }
            }
            graphics.drawString(x, i, line, effectiveStyle);
        }
    }
}
