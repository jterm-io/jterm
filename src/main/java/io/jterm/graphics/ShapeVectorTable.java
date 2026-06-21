package io.jterm.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Precomputes 6D shape vectors for printable ASCII characters and provides
 * nearest-neighbor lookup to find the best character for a given coverage vector.
 *
 * <p>The shape vector approach is inspired by Alex Harri's ASCII rendering
 * technique (https://alexharri.com/blog/ascii-rendering). Each character is
 * rendered into a small bitmap using a monospace font, then sampled in 6
 * sub-regions arranged as a 2×3 grid (2 columns, 3 rows). The fraction of
 * filled pixels in each sub-region forms a 6-dimensional shape vector.
 *
 * <p>At draw time, we compute how a line (or other primitive) occupies those
 * same 6 sub-regions of a cell, producing a sampling vector. We then find the
 * character whose shape vector is closest (by squared Euclidean distance) to
 * the sampling vector.
 *
 * <p>The 6 sub-regions are indexed as:
 * <pre>
 *  ┌─────┬─────┐
 *  │  0  │  1  │   top row
 *  ├─────┼─────┤
 *  │  2  │  3  │   middle row
 *  ├─────┼─────┤
 *  │  4  │  5  │   bottom row
 *  └─────┴─────┘
 * </pre>
 *
 * <p>Shape vectors are normalized so that the maximum component value across
 * all characters for each dimension is 1.0. This ensures that sampling vectors
 * (which can reach 1.0 coverage) map well to the character space.
 */
public final class ShapeVectorTable {

    /** Number of sub-regions per cell (2 columns × 3 rows). */
    public static final int DIMENSIONS = 6;

    // Rendering parameters for the character bitmap
    private static final int CELL_W = 40;   // sub-pixel width per cell
    private static final int CELL_H = 60;   // sub-pixel height per cell (taller than wide, like monospace)
    private static final int SUPER_W = CELL_W / 2;   // sub-region width
    private static final int SUPER_H = CELL_H / 3;   // sub-region height

    private final List<char[]> chars;
    private final double[][] vectors;        // normalized shape vectors
    private final double[] maxPerDim;        // max value per dimension (for normalization)

    private static ShapeVectorTable instance;

    /** Returns the singleton instance, lazily initialized. */
    public static synchronized ShapeVectorTable instance() {
        if (instance == null) {
            instance = new ShapeVectorTable();
        }
        return instance;
    }

    private ShapeVectorTable() {
        // Printable ASCII characters from '!' to '~' (33–126), plus space
        String charset = buildCharset();
        chars = new ArrayList<>();
        List<double[]> rawVectors = new ArrayList<>();

        BufferedImage img = new BufferedImage(CELL_W, CELL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.setBackground(Color.WHITE);

        // Use a monospace font; fall back to a logical font if unavailable
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 48);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < charset.length(); i++) {
            char c = charset.charAt(i);
            double[] vec = renderAndSample(g, fm, img, c);
            rawVectors.add(vec);
            chars.add(new char[]{c});
        }

        g.dispose();

        // Shape vectors are distributions (fraction of total ink per region),
        // same space as sampling vectors (fraction of line per region).
        // No per-dimension normalization needed — both already sum to ~1.0.
        vectors = new double[rawVectors.size()][DIMENSIONS];
        for (int i = 0; i < rawVectors.size(); i++) {
            for (int d = 0; d < DIMENSIONS; d++) {
                vectors[i][d] = rawVectors.get(i)[d];
            }
        }
        // maxPerDim not used but kept for compatibility
        maxPerDim = new double[DIMENSIONS];
    }

    private String buildCharset() {
        StringBuilder sb = new StringBuilder();
        // Space first (lowest density)
        sb.append(' ');
        // Printable ASCII from '!' to '~'
        for (char c = '!'; c <= '~'; c++) {
            sb.append(c);
        }
        // Also include some useful Unicode block characters
        sb.append('░'); // light shade
        sb.append('▒'); // medium shade
        sb.append('▓'); // dark shade
        sb.append('█'); // full block
        sb.append('▀'); // upper half block
        sb.append('▄'); // lower half block
        sb.append('▌'); // left half block
        sb.append('▐'); // right half block
        return sb.toString();
    }

    /**
     * Renders a character into the bitmap and samples 6 sub-regions.
     * Returns a 6D vector of coverage values (0.0 = empty, 1.0 = fully filled).
     */
    private double[] renderAndSample(Graphics2D g, FontMetrics fm, BufferedImage img, char c) {
        // Clear to white (background)
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, CELL_W, CELL_H);

        // Draw character in black
        g.setColor(Color.BLACK);
        int charW = fm.charWidth(c);
        int charH = fm.getAscent();
        // Center the character in the cell
        int x = (CELL_W - charW) / 2;
        int y = fm.getAscent() + (CELL_H - charH) / 2 - fm.getDescent() / 2;
        // Adjust for characters that render above/below baseline
        y = Math.max(fm.getAscent(), Math.min(y, CELL_H - fm.getDescent()));
        g.drawString(String.valueOf(c), x, y);

        // Sample 6 sub-regions: count dark pixels per region
        int[] darkCounts = new int[DIMENSIONS];
        int idx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int x0 = col * SUPER_W;
                int y0 = row * SUPER_H;
                darkCounts[idx++] = countDarkPixels(img, x0, y0, SUPER_W, SUPER_H);
            }
        }

        // Convert to distribution: fraction of total ink in each region.
        // This makes shape vectors comparable to sampling vectors (which
        // also represent distribution of the line across regions).
        int totalDark = 0;
        for (int d = 0; d < DIMENSIONS; d++) totalDark += darkCounts[d];
        double[] vec = new double[DIMENSIONS];
        if (totalDark > 0) {
            for (int d = 0; d < DIMENSIONS; d++) {
                vec[d] = (double) darkCounts[d] / totalDark;
            }
        }
        return vec;
    }

    /**
     * Counts dark pixels in the given region.
     */
    private int countDarkPixels(BufferedImage img, int x0, int y0, int w, int h) {
        int dark = 0;
        for (int y = y0; y < y0 + h && y < img.getHeight(); y++) {
            for (int x = x0; x < x0 + w && x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r + g + b < 384) dark++;
            }
        }
        return dark;
    }

    /**
     * Finds the best-matching character for the given sampling vector.
     *
     * @param samplingVector a 6D vector with values in [0, 1] representing
     *                       how much the line/primitive occupies each sub-region
     * @return the best-matching character
     */
    public char findBestChar(double[] samplingVector) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < vectors.length; i++) {
            double dist = 0;
            for (int d = 0; d < DIMENSIONS; d++) {
                double diff = vectors[i][d] - samplingVector[d];
                dist += diff * diff;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return chars.get(best)[0];
    }

    /**
     * Finds the best-matching character, but excludes characters that are
     * space or very low density. Useful for line drawing where we always want
     * a visible character.
     *
     * @param samplingVector a 6D vector with values in [0, 1]
     * @param minDensity     minimum total density (sum of vector components / 6)
     *                       required for a character to be considered
     * @return the best-matching character
     */
    public char findBestChar(double[] samplingVector, double minDensity) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < vectors.length; i++) {
            double density = 0;
            for (int d = 0; d < DIMENSIONS; d++) density += vectors[i][d];
            density /= DIMENSIONS;
            if (density < minDensity) continue;
            double dist = 0;
            for (int d = 0; d < DIMENSIONS; d++) {
                double diff = vectors[i][d] - samplingVector[d];
                dist += diff * diff;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        if (best < 0) return '*';
        return chars.get(best)[0];
    }

    /** Returns the number of characters in the table. */
    public int size() { return chars.size(); }

    /** Returns the shape vector for the character at index i (for testing). */
    double[] vector(int i) { return vectors[i]; }

    /** Returns the character at index i (for testing). */
    char charAt(int i) { return chars.get(i)[0]; }
}