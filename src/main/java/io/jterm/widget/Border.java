package io.jterm.widget;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.util.Symbols;

/** Decorative border wrapping a component. */
public class Border extends AbstractContainer {
    private final BorderStyle style;
    private String title;

    public Border(Component contents) { this(contents, BorderStyle.SINGLE_LINE); }

    public Border(Component contents, BorderStyle style) {
        this.style = style;
        addComponent(contents);
    }

    public void setTitle(String title) { this.title = title; }

    @Override
    protected TerminalSize calculatePreferredSize() {
        var ps = getChildren().get(0).getPreferredSize();
        return new TerminalSize(ps.columns() + 2, ps.rows() + 2);
    }

    @Override
    public void setBounds(io.jterm.core.TerminalPosition position, TerminalSize size) {
        super.setBounds(position, size);
        if (!getChildren().isEmpty()) {
            getChildren().get(0).setBounds(new io.jterm.core.TerminalPosition(1, 1), new TerminalSize(size.columns() - 2, size.rows() - 2));
        }
    }

    @Override
    protected void drawComponent(TextGraphics graphics) {
        var size = getSize();
        var borderCell = new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT);
        // top
        graphics.drawString(0, 0, style.tl + repeat(style.h, size.columns() - 2) + style.tr, borderCell);
        // sides
        for (int r = 1; r < size.rows() - 1; r++) {
            graphics.drawString(0, r, style.v, borderCell);
            graphics.drawString(size.columns() - 1, r, style.v, borderCell);
        }
        // bottom
        graphics.drawString(0, size.rows() - 1, style.bl + repeat(style.h, size.columns() - 2) + style.br, borderCell);
        if (title != null && !title.isEmpty() && size.columns() > title.length() + 4) {
            graphics.drawString(2, 0, " " + title + " ", new TextCell(' ', AnsiColor.DEFAULT, AnsiColor.DEFAULT));
        }
        super.drawComponent(graphics);
    }

    private String repeat(String s, int n) {
        return n <= 0 ? "" : s.repeat(n);
    }

    public enum BorderStyle {
        SINGLE_LINE(Symbols.TL_CORNER, Symbols.TR_CORNER, Symbols.BL_CORNER, Symbols.BR_CORNER, Symbols.H_LINE, Symbols.V_LINE),
        DOUBLE_LINE(Symbols.TL_DOUBLE, Symbols.TR_DOUBLE, Symbols.BL_DOUBLE, Symbols.BR_DOUBLE, Symbols.H_DOUBLE, Symbols.V_DOUBLE),
        ROUNDED(Symbols.TL_ROUNDED, Symbols.TR_ROUNDED, Symbols.BL_ROUNDED, Symbols.BR_ROUNDED, Symbols.H_LINE, Symbols.V_LINE);

        final String tl, tr, bl, br, h, v;
        BorderStyle(String tl, String tr, String bl, String br, String h, String v) {
            this.tl = tl; this.tr = tr; this.bl = bl; this.br = br; this.h = h; this.v = v;
        }
    }
}
