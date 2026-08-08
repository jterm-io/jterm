package io.jterm.demo;

import io.jterm.bitmapfont.BitmapFont;
import io.jterm.bitmapfont.BitmapTextRenderer;
import io.jterm.core.AnsiTerminal;
import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.screen.DefaultScreen;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;

import java.io.IOException;

/**
 * Demo application showcasing the {@link BitmapFont} and
 * {@link BitmapTextRenderer} system. Renders stylized pixel-art text in three
 * different fonts (block16, block8, banner) with different color schemes and
 * dithering effects — inspired by 16-bit BBS game title screens.
 *
 * <p><b>Keyboard</b></p>
 * <ul>
 *   <li><b>q</b> — quit</li>
 * </ul>
 */
public class BitmapFontDemo {

    private BitmapFontDemo() {}

    /**
     * Entry point — creates a terminal, loads the bitmap fonts, renders the
     * demo text, and waits for the user to quit.
     *
     * @param args unused
     * @throws IOException if terminal I/O fails or fonts cannot be loaded
     */
    public static void main(String[] args) throws IOException {
        var font16 = BitmapFont.loadResource("bitmapfonts/block16.jfont");
        var font8 = BitmapFont.loadResource("bitmapfonts/block8.jfont");
        var banner = BitmapFont.loadResource("bitmapfonts/banner.jfont");

        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        screen.startScreen();

        try {
            renderDemo(screen.getBackBuffer(), font16, font8, banner);
            screen.refresh();

            // Wait for 'q' to quit
            while (true) {
                KeyStroke ks = terminal.pollInput().orElse(null);
                if (ks != null && ks.type() == KeyType.CHARACTER
                        && (ks.character() == 'q' || ks.character() == 'Q')) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            screen.stopScreen();
        }
    }

    /**
     * Render the full demo onto the given ScreenBuffer using the three fonts.
     *
     * @param buffer the target ScreenBuffer
     * @param font16 the block16 font (16x16, for the main title)
     * @param font8  the block8 font (8x8, for the subtitle)
     * @param banner the banner font (8x7, for the footer)
     * @throws IOException if the fonts cannot be read
     */
    static void renderDemo(ScreenBuffer buffer, BitmapFont font16, BitmapFont font8,
                           BitmapFont banner) throws IOException {
        var renderer = new BitmapTextRenderer(buffer);
        var size = buffer.size();
        int cols = size.columns();

        // Clear background
        buffer.fill(new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK));

        // Label for block16
        int labelRow = 2;
        drawLabel(buffer, "block16 - SALVAGE", 2, labelRow, AnsiColor.BRIGHT_CYAN);

        // Render "SALVAGE" in block16 with blue highlight, grey base, dark shadow, dither 0.5
        int salvageX = 2;
        int salvageY = labelRow + 2;
        renderer.draw("SALVAGE", salvageX, salvageY, font16)
                .withBase(AnsiColor.BRIGHT_BLACK)
                .withHighlight(AnsiColor.BLUE)
                .withShadow(AnsiColor.BLACK)
                .withDither(0.5)
                .withBackground(AnsiColor.BLACK)
                .render();

        // Label for block8
        int block8LabelRow = salvageY + font16.charHeight() + 2;
        drawLabel(buffer, "block8 - HELLO", 2, block8LabelRow, AnsiColor.BRIGHT_GREEN);

        // Render "HELLO" in block8 with green colors
        int helloY = block8LabelRow + 2;
        renderer.draw("HELLO", 2, helloY, font8)
                .withBase(AnsiColor.GREEN)
                .withHighlight(AnsiColor.BRIGHT_GREEN)
                .withShadow(AnsiColor.BLACK)
                .withDither(0.5)
                .withBackground(AnsiColor.BLACK)
                .render();

        // Label for banner
        int bannerLabelRow = helloY + font8.charHeight() + 2;
        drawLabel(buffer, "banner - JTERM", 2, bannerLabelRow, AnsiColor.BRIGHT_CYAN);

        // Render "JTERM" in banner font with cyan colors
        int jtermY = bannerLabelRow + 2;
        renderer.draw("JTERM", 2, jtermY, banner)
                .withBase(AnsiColor.CYAN)
                .withHighlight(AnsiColor.BRIGHT_CYAN)
                .withShadow(AnsiColor.BLACK)
                .withDither(0.0)
                .withBackground(AnsiColor.BLACK)
                .render();

        // Footer hint
        drawLabel(buffer, "[q] Quit", 2, size.rows() - 2, AnsiColor.BRIGHT_BLACK);
    }

    /**
     * Draw a simple text label (regular terminal text, not bitmap) onto the buffer.
     *
     * @param buffer the target ScreenBuffer
     * @param text    the label text
     * @param x       the starting column
     * @param y       the row
     * @param color   the text color
     */
    private static void drawLabel(ScreenBuffer buffer, String text, int x, int y,
                                   AnsiColor color) {
        for (int i = 0; i < text.length(); i++) {
            buffer.setCell(x + i, y, new TextCell(text.charAt(i), color, AnsiColor.BLACK));
        }
    }
}