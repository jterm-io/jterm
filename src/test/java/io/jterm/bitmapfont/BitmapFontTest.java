package io.jterm.bitmapfont;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * Tests for {@link BitmapFont} — font loading and grid retrieval.
 */
class BitmapFontTest {

    /**
     * Test that a font definition is loaded from a classpath resource and
     * its metadata (name, width, height) is correct.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont loads font definition from resource")
    void loadsFontFromResource() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        assertEquals("test", font.name());
        assertEquals(3, font.charWidth());
        assertEquals(3, font.charHeight());
    }

    /**
     * Test that the grid for a known character is returned correctly.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont returns correct grid for character")
    void returnsCorrectGrid() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var grid = font.gridOf('A');
        assertNotNull(grid, "Grid for 'A' should not be null");
        assertEquals(3, grid.length, "Grid should have 3 rows");
        assertEquals(3, grid[0].length, "Grid row should have 3 columns");
        assertArrayEquals(new int[][]{{1, 1, 1}, {1, 2, 1}, {1, 1, 3}}, grid);
    }

    /**
     * Test that a different character returns a different grid.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont returns different grids for different characters")
    void returnsDifferentGrids() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var gridA = font.gridOf('A');
        var gridB = font.gridOf('B');
        assertArrayEquals(new int[][]{{0, 1, 0}, {1, 1, 1}, {0, 1, 0}}, gridB);
        assertFalse(java.util.Arrays.deepEquals(gridA, gridB), "Grids for A and B should differ");
    }

    /**
     * Test that a space character has an all-zero (transparent) grid.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont returns all-zero grid for space")
    void spaceIsTransparent() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        var grid = font.gridOf(' ');
        for (int[] row : grid) {
            for (int val : row) {
                assertEquals(0, val, "Space cell should be transparent (0)");
            }
        }
    }

    /**
     * Test that a missing character returns null.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont returns null for undefined character")
    void returnsNullForUndefined() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/test.jfont");
        assertNull(font.gridOf('Z'), "Grid for undefined char 'Z' should be null");
    }

    /**
     * Test that loading the real block8 font works and has expected dimensions.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont loads block8 font with correct dimensions")
    void loadsBlock8Font() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/block8.jfont");
        assertEquals("block8", font.name());
        assertEquals(8, font.charWidth());
        assertEquals(8, font.charHeight());
        assertNotNull(font.gridOf('A'), "block8 should have letter A");
        assertNotNull(font.gridOf('Z'), "block8 should have letter Z");
        assertNotNull(font.gridOf('0'), "block8 should have digit 0");
    }

    /**
     * Test that loading the real block16 font works and has expected dimensions.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont loads block16 font with correct dimensions")
    void loadsBlock16Font() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/block16.jfont");
        assertEquals("block16", font.name());
        assertEquals(16, font.charWidth());
        assertEquals(16, font.charHeight());
        assertNotNull(font.gridOf('S'), "block16 should have letter S");
    }

    /**
     * Test that loading the real banner font works and has expected dimensions.
     *
     * @throws IOException if the resource cannot be loaded
     */
    @Test
    @DisplayName("BitmapFont loads banner font with correct dimensions")
    void loadsBannerFont() throws IOException {
        var font = BitmapFont.loadResource("bitmapfonts/banner.jfont");
        assertEquals("banner", font.name());
        assertEquals(8, font.charWidth());
        assertEquals(7, font.charHeight());
        assertNotNull(font.gridOf('J'), "banner should have letter J");
        assertNotNull(font.gridOf('T'), "banner should have letter T");
    }
}