package io.jterm.screen;

import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.IndexedColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Parser and renderer for ANSI art files (.ans / .asc) — the classic BBS art
 * format.
 * <p>
 * These files contain plain text with embedded ANSI escape sequences for
 * color and styling. The parser maintains a cursor position (col, row) that
 * advances as characters are consumed, and handles the standard SGR color
 * codes:
 * <ul>
 *   <li>{@code ESC[0m} — reset to default colors</li>
 *   <li>{@code ESC[1m} — bold</li>
 *   <li>{@code ESC[7m} — reverse video</li>
 *   <li>{@code ESC[38;5;Nm} — 256-color foreground</li>
 *   <li>{@code ESC[48;5;Nm} — 256-color background</li>
 *   <li>{@code ESC[3Nm} — basic 16-color foreground (0-7)</li>
 *   <li>{@code ESC[4Nm} — basic 16-color background (0-7)</li>
 *   <li>{@code ESC[9Nm} — bright foreground (8-15)</li>
 *   <li>{@code ESC[10Nm} — bright background (8-15)</li>
 *   <li>Combined: {@code ESC[1;31m}, {@code ESC[1;31;42m}, etc.</li>
 * </ul>
 */
public final class AnsiArtRenderer {

    private AnsiArtRenderer() {}

    /**
     * Render ANSI art content to the graphics buffer at position (0,0).
     *
     * @param g           the graphics buffer to render into
     * @param ansiContent the ANSI art string (with embedded escape sequences)
     */
    public static void render(TextGraphics g, String ansiContent) {
        render(g, ansiContent, 0, 0);
    }

    /**
     * Render ANSI art content to the graphics buffer at an offset position.
     *
     * @param g           the graphics buffer to render into
     * @param ansiContent the ANSI art string (with embedded escape sequences)
     * @param startCol    starting column offset
     * @param startRow    starting row offset
     */
    public static void render(TextGraphics g, String ansiContent, int startCol, int startRow) {
        if (ansiContent == null || ansiContent.isEmpty()) return;

        var size = g.getSize();
        int maxCol = size.columns();
        int maxRow = size.rows();

        // Cursor state
        int col = startCol;
        int row = startRow;

        // Current color / style state
        Color fg = AnsiColor.DEFAULT;
        Color bg = AnsiColor.DEFAULT;
        var mods = EnumSet.noneOf(SGR.class);

        int i = 0;
        int len = ansiContent.length();
        while (i < len) {
            char ch = ansiContent.charAt(i);

            // Check for ANSI escape sequence: ESC[ ... m
            if (ch == 0x1B && i + 1 < len && ansiContent.charAt(i + 1) == '[') {
                // Find the end of the escape sequence (terminated by 'm' or other final byte)
                int j = i + 2;
                while (j < len) {
                    char cj = ansiContent.charAt(j);
                    // CSI sequences end with a byte in 0x40-0x7E
                    if (cj >= 0x40 && cj <= 0x7E) break;
                    j++;
                }
                if (j < len && ansiContent.charAt(j) == 'm') {
                    // SGR sequence — parse parameters
                    String params = ansiContent.substring(i + 2, j);
                    var result = parseSgr(params, fg, bg, mods);
                    fg = result.fg();
                    bg = result.bg();
                    mods = result.mods();
                    i = j + 1;
                    continue;
                } else if (j < len) {
                    // Non-SGR escape sequence (e.g., cursor positioning) — skip it
                    // Handle common ones: ESC[H = home, ESC[2J = clear
                    String seq = ansiContent.substring(i + 2, j);
                    char finalByte = ansiContent.charAt(j);
                    if (finalByte == 'H' || finalByte == 'f') {
                        // Cursor position — reset to start
                        if (seq.isEmpty() || seq.equals(";")) {
                            col = startCol;
                            row = startRow;
                        } else {
                            String[] parts = seq.split(";");
                            int r = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0]) : 1;
                            int c = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 1;
                            row = startRow + r - 1;
                            col = startCol + c - 1;
                        }
                    } else if (finalByte == 'J') {
                        // Erase display — just reset cursor for simplicity
                        if (seq.equals("2")) {
                            col = startCol;
                            row = startRow;
                        }
                    } else if (finalByte == 'K') {
                        // Erase line — skip (we don't erase cells)
                    } else if (finalByte == 'A') {
                        row -= seq.isEmpty() ? 1 : Integer.parseInt(seq);
                    } else if (finalByte == 'B') {
                        row += seq.isEmpty() ? 1 : Integer.parseInt(seq);
                    } else if (finalByte == 'C') {
                        col += seq.isEmpty() ? 1 : Integer.parseInt(seq);
                    } else if (finalByte == 'D') {
                        col -= seq.isEmpty() ? 1 : Integer.parseInt(seq);
                    }
                    i = j + 1;
                    continue;
                } else {
                    // Incomplete escape at end of string
                    break;
                }
            }

            // Handle control characters
            if (ch == '\n') {
                row++;
                col = startCol;
                i++;
                continue;
            }
            if (ch == '\r') {
                col = startCol;
                i++;
                continue;
            }
            if (ch == '\t') {
                col = ((col - startCol) / 8 + 1) * 8 + startCol;
                i++;
                continue;
            }
            if (ch == 0x1B) {
                // Lone ESC without [ — skip
                i++;
                continue;
            }

            // Printable character
            if (col >= 0 && col < maxCol && row >= 0 && row < maxRow) {
                var cell = new TextCell(String.valueOf(ch), fg, bg, mods.isEmpty() ? EnumSet.noneOf(SGR.class) : EnumSet.copyOf(mods));
                g.setCell(col, row, cell);
            }
            col++;
            i++;
        }
    }

    /**
     * Parse SGR parameter string and return updated color/style state.
     */
    private static SgrResult parseSgr(String params, Color curFg, Color curBg, EnumSet<SGR> curMods) {
        var mods = EnumSet.copyOf(curMods);
        Color fg = curFg;
        Color bg = curBg;

        if (params.isEmpty()) {
            params = "0";
        }

        String[] parts = params.split(";");
        int idx = 0;
        while (idx < parts.length) {
            String partStr = parts[idx].trim();
            int code = partStr.isEmpty() ? 0 : Integer.parseInt(partStr);

            if (code == 0) {
                // Reset all
                fg = AnsiColor.DEFAULT;
                bg = AnsiColor.DEFAULT;
                mods.clear();
            } else if (code == 1) {
                mods.add(SGR.BOLD);
            } else if (code == 2) {
                mods.add(SGR.DIM);
            } else if (code == 3) {
                mods.add(SGR.ITALIC);
            } else if (code == 4) {
                mods.add(SGR.UNDERLINE);
            } else if (code == 5) {
                mods.add(SGR.BLINK);
            } else if (code == 7) {
                mods.add(SGR.REVERSE);
            } else if (code == 8) {
                mods.add(SGR.HIDDEN);
            } else if (code == 9) {
                mods.add(SGR.STRIKETHROUGH);
            } else if (code == 22) {
                mods.remove(SGR.BOLD);
                mods.remove(SGR.DIM);
            } else if (code == 23) {
                mods.remove(SGR.ITALIC);
            } else if (code == 24) {
                mods.remove(SGR.UNDERLINE);
            } else if (code == 25) {
                mods.remove(SGR.BLINK);
            } else if (code == 27) {
                mods.remove(SGR.REVERSE);
            } else if (code == 28) {
                mods.remove(SGR.HIDDEN);
            } else if (code == 29) {
                mods.remove(SGR.STRIKETHROUGH);
            } else if (code == 38) {
                // Extended foreground color
                if (idx + 1 < parts.length) {
                    int mode = Integer.parseInt(parts[idx + 1].trim());
                    if (mode == 5 && idx + 2 < parts.length) {
                        // 256-color: ESC[38;5;Nm
                        int n = Integer.parseInt(parts[idx + 2].trim());
                        fg = indexedToColor(n);
                        idx += 2;
                    } else if (mode == 2 && idx + 4 < parts.length) {
                        // True color: ESC[38;2;R;G;Bm
                        int r = Integer.parseInt(parts[idx + 2].trim());
                        int gg = Integer.parseInt(parts[idx + 3].trim());
                        int b = Integer.parseInt(parts[idx + 4].trim());
                        fg = new io.jterm.style.RgbColor(r, gg, b);
                        idx += 4;
                    }
                }
            } else if (code == 48) {
                // Extended background color
                if (idx + 1 < parts.length) {
                    int mode = Integer.parseInt(parts[idx + 1].trim());
                    if (mode == 5 && idx + 2 < parts.length) {
                        // 256-color: ESC[48;5;Nm
                        int n = Integer.parseInt(parts[idx + 2].trim());
                        bg = indexedToColor(n);
                        idx += 2;
                    } else if (mode == 2 && idx + 4 < parts.length) {
                        // True color: ESC[48;2;R;G;Bm
                        int r = Integer.parseInt(parts[idx + 2].trim());
                        int gg = Integer.parseInt(parts[idx + 3].trim());
                        int b = Integer.parseInt(parts[idx + 4].trim());
                        bg = new io.jterm.style.RgbColor(r, gg, b);
                        idx += 4;
                    }
                }
            } else if (code >= 30 && code <= 37) {
                // Basic foreground colors (30-37)
                fg = AnsiColor.values()[code - 30];
            } else if (code == 39) {
                fg = AnsiColor.DEFAULT;
            } else if (code >= 40 && code <= 47) {
                // Basic background colors (40-47)
                bg = AnsiColor.values()[code - 40];
            } else if (code == 49) {
                bg = AnsiColor.DEFAULT;
            } else if (code >= 90 && code <= 97) {
                // Bright foreground (90-97)
                fg = AnsiColor.values()[code - 90 + 8];
            } else if (code >= 100 && code <= 107) {
                // Bright background (100-107)
                bg = AnsiColor.values()[code - 100 + 8];
            }
            // Unknown codes are silently ignored
            idx++;
        }

        return new SgrResult(fg, bg, mods);
    }

    /**
     * Map a 256-color index to a Color object. Indices 0-15 map to AnsiColor;
     * 16-255 use IndexedColor.
     */
    private static Color indexedToColor(int n) {
        if (n >= 0 && n <= 15) {
            return AnsiColor.values()[n];
        }
        return new IndexedColor(n);
    }

    /**
     * Load an ANSI art file from disk.
     *
     * @param path path to the .ans/.asc file
     * @return the file content as a string (with escape sequences intact)
     * @throws IOException if the file cannot be read
     */
    public static String fromFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /** Internal record for SGR parsing results. */
    private record SgrResult(Color fg, Color bg, EnumSet<SGR> mods) {}
}