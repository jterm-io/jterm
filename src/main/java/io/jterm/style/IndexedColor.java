package io.jterm.style;

/** 256-color xterm palette. Index 0-15 = system, 16-231 = 6×6×6 RGB cube, 232-255 = grayscale ramp. */
public record IndexedColor(int index) implements Color {
    public IndexedColor {
        if (index < 0 || index > 255)
            throw new IllegalArgumentException("Index must be 0-255, got: " + index);
    }

    public static IndexedColor fromRgb(int r, int g, int b) {
        // Try 6×6×6 cube first
        int[] levels = {0x00, 0x5f, 0x87, 0xaf, 0xd7, 0xff};
        int ri = nearestLevel(r, levels);
        int gi = nearestLevel(g, levels);
        int bi = nearestLevel(b, levels);
        int cubeIdx = 16 + 36 * ri + 6 * gi + bi;

        // Try grayscale ramp
        int avg = (r + g + b) / 3;
        int grayIdx = 232 + Math.min(23, Math.max(0, (int) (avg / 255.0 * 23.0)));

        // Pick closer match
        int cubeDist = colorDist(r, g, b, levels[ri], levels[gi], levels[bi]);
        int grayDist = colorDist(r, g, b, grayVal(grayIdx), grayVal(grayIdx), grayVal(grayIdx));
        return new IndexedColor(cubeDist <= grayDist ? cubeIdx : grayIdx);
    }

    private static int nearestLevel(int v, int[] levels) {
        int best = 0, minDist = Integer.MAX_VALUE;
        for (int i = 0; i < levels.length; i++) {
            int d = Math.abs(v - levels[i]);
            if (d < minDist) { minDist = d; best = i; }
        }
        return best;
    }

    private static int colorDist(int r1, int g1, int b1, int r2, int g2, int b2) {
        return (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2);
    }

    private static int grayVal(int idx) { return 0x08 + (idx - 232) * 0x0a; }

    @Override
    public byte[] fgSequence() { return ("38;5;" + index).getBytes(); }

    @Override
    public byte[] bgSequence() { return ("48;5;" + index).getBytes(); }
}
