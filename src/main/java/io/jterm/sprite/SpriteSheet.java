package io.jterm.sprite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a single ANSI art file and splits it into a grid of sprite frames.
 * The grid is defined by {@code cellWidth} × {@code cellHeight} cells arranged
 * in {@code cols} columns and {@code rows} rows.
 *
 * <p>Frames are extracted left-to-right, top-to-bottom (row-major order).
 * For example, with {@code cols=3, rows=2}, frames are returned as:
 * <pre>
 *   [0][1][2]
 *   [3][4][5]
 * </pre>
 *
 * <p>SGR escape sequences are preserved within each extracted frame so that
 * colors and styles defined in the source art are retained. Each frame is a
 * standalone ANSI string suitable for direct use with
 * {@code AnsiArtRenderer} or {@link Sprite#addFrame(String)}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Load a 4x4 grid of 8x8-cell frames from a sprite sheet ANSI file
 * SpriteSheet sheet = SpriteSheet.fromFile(Path.of("character.ans"), 8, 8, 4, 4);
 * Sprite sprite = sheet.toSprite();
 * sprite.setFrameMs(80);
 * sprite.setLoopMode(Sprite.LoopMode.LOOP);
 * }</pre>
 */
public class SpriteSheet {

    private final String content;
    private final int cellWidth;
    private final int cellHeight;
    private final int cols;
    private final int rows;
    private final List<String> frames;

    /**
     * Construct a sprite sheet from raw ANSI content.
     *
     * @param content    raw ANSI art string
     * @param cellWidth   width of each cell in terminal cells (≥ 1)
     * @param cellHeight  height of each cell in terminal cells (≥ 1)
     * @param cols        number of columns of cells (≥ 1)
     * @param rows        number of rows of cells (≥ 1)
     */
    public SpriteSheet(String content, int cellWidth, int cellHeight, int cols, int rows) {
        if (cellWidth <= 0) throw new IllegalArgumentException("cellWidth must be > 0");
        if (cellHeight <= 0) throw new IllegalArgumentException("cellHeight must be > 0");
        if (cols <= 0) throw new IllegalArgumentException("cols must be > 0");
        if (rows <= 0) throw new IllegalArgumentException("rows must be > 0");
        this.content = content == null ? "" : content;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.cols = cols;
        this.rows = rows;
        this.frames = extractFrames();
    }

    /**
     * Load a sprite sheet from a file on disk.
     *
     * @param path        path to the .ans file
     * @param cellWidth   width of each cell in terminal cells
     * @param cellHeight  height of each cell in terminal cells
     * @param cols        number of columns of cells
     * @param rows        number of rows of cells
     * @return a SpriteSheet
     * @throws IOException if the file cannot be read
     */
    public static SpriteSheet fromFile(Path path, int cellWidth, int cellHeight, int cols, int rows)
            throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return new SpriteSheet(content, cellWidth, cellHeight, cols, rows);
    }

    private List<String> extractFrames() {
        // Split content into logical lines (preserving SGR codes intact within
        // each line). We tokenize into a list of "raw lines", where a raw line
        // is the content between newlines. SGR codes inside the line remain.
        List<String> rawLines = splitLines(content);

        List<String> out = new ArrayList<>(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                out.add(extractCell(rawLines, c, r));
            }
        }
        return out;
    }

    /**
     * Split content into raw lines preserving SGR sequences.
     */
    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\n') {
                lines.add(cur.toString());
                cur.setLength(0);
            } else if (ch == '\r') {
                // skip; handled by \n
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }

    /**
     * Extract the cell at column {@code col} and row {@code r}.
     * For each logical row of the cell, slice the visible portion (columns
     * [col*cellWidth, col*cellWidth+cellWidth)) from the source line. SGR
     * state is preserved: we carry any active SGR sequence from the start of
     * the line into the cell so that color state is correctly initialized.
     */
    private String extractCell(List<String> rawLines, int col, int r) {
        int startLine = r * cellHeight;
        StringBuilder out = new StringBuilder();
        for (int h = 0; h < cellHeight; h++) {
            int lineIdx = startLine + h;
            String line = lineIdx < rawLines.size() ? rawLines.get(lineIdx) : "";
            String slice = sliceWithSgr(line, col * cellWidth, cellWidth);
            if (h > 0) out.append('\n');
            out.append(slice);
        }
        return out.toString();
    }

    /**
     * Slice the visible portion of a line between columns [startCol, startCol+width).
     * SGR escape sequences are preserved and copied as encountered. Carries
     * the active SGR state at the start of the slice so the resulting fragment
     * renders with the correct color.
     */
    private static String sliceWithSgr(String line, int startCol, int width) {
        StringBuilder out = new StringBuilder();
        StringBuilder pendingSgr = new StringBuilder(); // active SGR at start
        int visibleCol = 0;
        int i = 0;
        int len = line.length();
        boolean inSgr = false;

        // First pass: capture active SGR state at startCol
        int col = 0;
        int idx = 0;
        while (idx < len) {
            char ch = line.charAt(idx);
            if (ch == 0x1B && idx + 1 < len && line.charAt(idx + 1) == '[') {
                int j = idx + 2;
                while (j < len) {
                    char cj = line.charAt(j);
                    if (cj >= 0x40 && cj <= 0x7E) break;
                    j++;
                }
                if (j < len) {
                    String esc = line.substring(idx, j + 1);
                    if (col <= startCol) {
                        // SGR before/at start; carry forward
                        if (line.charAt(j) == 'm') {
                            pendingSgr.append(esc);
                        }
                    } else {
                        // SGR after start; include in output when within window
                        // handled in second pass
                    }
                    idx = j + 1;
                    continue;
                }
            }
            if (col >= startCol && col < startCol + width) {
                // visible char
                break;
            }
            col++;
            idx++;
        }

        // Second pass: build output, prefixing with carried SGR state
        if (pendingSgr.length() > 0) {
            out.append(pendingSgr);
        }

        col = 0;
        i = 0;
        while (i < len) {
            char ch = line.charAt(i);
            if (ch == 0x1B && i + 1 < len && line.charAt(i + 1) == '[') {
                int j = i + 2;
                while (j < len) {
                    char cj = line.charAt(j);
                    if (cj >= 0x40 && cj <= 0x7E) break;
                    j++;
                }
                if (j < len) {
                    String esc = line.substring(i, j + 1);
                    if (col >= startCol && col < startCol + width && line.charAt(j) == 'm') {
                        out.append(esc);
                    }
                    i = j + 1;
                    continue;
                }
            }
            if (col >= startCol && col < startCol + width) {
                out.append(ch);
            }
            col++;
            if (col >= startCol + width) break;
            i++;
        }

        // Pad with spaces if line was shorter than expected
        int visible = 0;
        for (int k = 0; k < out.length(); k++) {
            char ch = out.charAt(k);
            if (ch != 0x1B) visible++;
        }
        while (visible < width) {
            out.append(' ');
            visible++;
        }

        return out.toString();
    }

    /**
     * @return number of frames in the sheet (cols × rows)
     */
    public int frameCount() {
        return frames.size();
    }

    /**
     * Get the raw ANSI string for the frame at index {@code index} (row-major).
     *
     * @param index frame index (0..frameCount()-1)
     * @return the raw ANSI string for that frame
     */
    public String frameAt(int index) {
        return frames.get(index);
    }

    /**
     * Build a playable {@link Sprite} from this sheet, using default timing
     * (100ms per frame) and {@link Sprite.LoopMode#LOOP}.
     *
     * @return a new Sprite
     */
    public Sprite toSprite() {
        var sprite = new Sprite();
        for (String f : frames) {
            sprite.addFrame(f);
        }
        return sprite;
    }

    /**
     * Build a playable {@link Sprite} with custom frame timing and loop mode.
     *
     * @param frameMs  per-frame duration in ms
     * @param loopMode  loop behaviour
     * @return a new Sprite
     */
    public Sprite toSprite(int frameMs, Sprite.LoopMode loopMode) {
        var sprite = toSprite();
        sprite.setFrameMs(frameMs);
        sprite.setLoopMode(loopMode);
        return sprite;
    }

    /** @return cell width in terminal cells */
    public int getCellWidth() { return cellWidth; }
    /** @return cell height in terminal cells */
    public int getCellHeight() { return cellHeight; }
    /** @return number of columns */
    public int getCols() { return cols; }
    /** @return number of rows */
    public int getRows() { return rows; }
}