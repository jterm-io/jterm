package io.jterm.bitmapfont;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A bitmap font definition where each character is represented as a grid of
 * integer layer indices. Layer values: 0 = transparent, 1 = base, 2 = highlight,
 * 3 = shadow. Font definitions are loaded from {@code .jfont} resource files.
 *
 * <p>Resource file format (plain text):
 * <pre>
 * font &lt;name&gt;
 * width &lt;n&gt;
 * height &lt;n&gt;
 * ---
 * char &lt;c&gt;
 * &lt;row1 digits&gt;
 * &lt;row2 digits&gt;
 * ...
 * ---
 * </pre>
 *
 * <p>Each character block contains {@code height} rows of exactly {@code width}
 * digits (0–3), one per cell.
 */
public class BitmapFont {

    private final String name;
    private final int charWidth;
    private final int charHeight;
    private final Map<Character, int[][]> glyphs;

    /**
     * Construct a BitmapFont from its components.
     *
     * @param name       the font name
     * @param charWidth  the width of each character in cells
     * @param charHeight the height of each character in cells
     * @param glyphs     the map of character → int[][] grid (row-major, top to bottom)
     */
    public BitmapFont(String name, int charWidth, int charHeight, Map<Character, int[][]> glyphs) {
        this.name = name;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
        this.glyphs = Map.copyOf(glyphs);
    }

    /**
     * Returns the font name.
     *
     * @return the font name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the width of each character in cells.
     *
     * @return the character width
     */
    public int charWidth() {
        return charWidth;
    }

    /**
     * Returns the height of each character in cells.
     *
     * @return the character height
     */
    public int charHeight() {
        return charHeight;
    }

    /**
     * Returns the grid for the given character, or {@code null} if the font
     * does not define that character.
     *
     * @param c the character to look up
     * @return the int[][] grid (row-major, top to bottom), or null if undefined
     */
    public int[][] gridOf(char c) {
        return glyphs.get(c);
    }

    /**
     * Returns whether this font defines a glyph for the given character.
     *
     * @param c the character to check
     * @return true if the character has a glyph, false otherwise
     */
    public boolean hasGlyph(char c) {
        return glyphs.containsKey(c);
    }

    /**
     * Loads a bitmap font from a classpath resource file.
     *
     * @param resourcePath the classpath resource path (e.g. "bitmapfonts/block8.jfont")
     * @return the loaded BitmapFont
     * @throws IOException          if the resource cannot be read or parsed
     * @throws NullPointerException if the resource is not found
     */
    public static BitmapFont loadResource(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "Resource path must not be null");
        InputStream stream = BitmapFont.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            // Also try test class loader
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            throw new IOException("Font resource not found: " + resourcePath);
        }
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }

    /**
     * Parses a font definition from a BufferedReader.
     *
     * @param reader the reader positioned at the start of the font file
     * @return the parsed BitmapFont
     * @throws IOException if the file is malformed
     */
    private static BitmapFont parse(BufferedReader reader) throws IOException {
        String fontName = null;
        int width = 0;
        int height = 0;
        Map<Character, int[][]> glyphMap = new HashMap<>();

        String line;
        Character currentChar = null;
        int currentRow = 0;
        int[][] currentGrid = null;
        boolean inCharBlock = false;

        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("font ")) {
                fontName = trimmed.substring(5).trim();
                continue;
            }
            if (trimmed.startsWith("width ")) {
                width = Integer.parseInt(trimmed.substring(6).trim());
                continue;
            }
            if (trimmed.startsWith("height ")) {
                height = Integer.parseInt(trimmed.substring(7).trim());
                continue;
            }
            if (trimmed.equals("---")) {
                // End of a character block (or header separator)
                if (inCharBlock && currentGrid != null) {
                    glyphMap.put(currentChar, currentGrid);
                }
                inCharBlock = false;
                currentGrid = null;
                currentChar = null;
                currentRow = 0;
                continue;
            }
            // Check "char " on the raw line (not trimmed) so "char  " (space char) works
            if (line.startsWith("char ") && !line.isBlank()) {
                // Don't trim the part after "char " — the character itself could be a space
                String charPart = line.substring(5);
                if (charPart.isEmpty()) {
                    throw new IOException("char directive has no character");
                }
                currentChar = charPart.charAt(0);
                currentGrid = new int[height][width];
                currentRow = 0;
                inCharBlock = true;
                continue;
            }
            // Data row
            if (inCharBlock && currentGrid != null && currentRow < height) {
                for (int c = 0; c < width && c < line.length(); c++) {
                    currentGrid[currentRow][c] = line.charAt(c) - '0';
                }
                currentRow++;
            }
        }

        if (fontName == null || width == 0 || height == 0) {
            throw new IOException("Font file missing required header (font, width, height)");
        }

        return new BitmapFont(fontName, width, height, glyphMap);
    }
}