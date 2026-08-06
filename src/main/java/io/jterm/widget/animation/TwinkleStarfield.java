package io.jterm.widget.animation;

import io.jterm.animation.AnimatedBackground;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A calm twinkling starfield background. Stars have fixed normalized positions
 * and cycle through four brightness levels via a sine wave. Most stars are
 * white/gray, but about 10% carry a subtle color tint.
 *
 * <p>This implementation is independent of {@link io.jterm.animation.StarfieldBackground}
 * so it can use per-star phase/speed math and honor CP437-safe terminals.</p>
 */
public class TwinkleStarfield implements AnimatedBackground {

    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 3;
    private static final int BRIGHTNESS_LEVELS = MAX_BRIGHTNESS - MIN_BRIGHTNESS + 1;

    private static final char[] CP437_SAFE_GLYPHS = {'.', '`', '\'', '+', '*'};
    private static final char[] UNICODE_GLYPHS = {'.', '`', '\'', '\u2726', '\u2727', '\u2605', '\u2731', '+', '*'};

    private static final AnsiColor[] TINTS = {
            AnsiColor.BRIGHT_BLUE,
            AnsiColor.BRIGHT_CYAN,
            AnsiColor.BRIGHT_YELLOW,
            AnsiColor.BRIGHT_RED
    };

    private final Random random;
    private final List<Star> stars = new ArrayList<>();
    private final boolean cp437Safe;
    private volatile boolean running;
    private TerminalSize lastSize;

    /** Creates a starfield using a fresh {@link Random} and Unicode glyphs. */
    public TwinkleStarfield() {
        this(new Random(), false);
    }

    /** Creates a starfield using the supplied {@link Random} and Unicode glyphs. */
    public TwinkleStarfield(Random random) {
        this(random, false);
    }

    /**
     * Creates a starfield using the supplied random source and glyph mode.
     *
     * @param random    random source
     * @param cp437Safe if true, only CP437-encodable glyphs are chosen
     */
    public TwinkleStarfield(Random random, boolean cp437Safe) {
        this.random = random;
        this.cp437Safe = cp437Safe;
        generateStars(120);
    }

    /** Visible for tests: returns an unmodifiable view of the generated stars. */
    public List<Star> getStars() {
        return List.copyOf(stars);
    }

    private void generateStars(int count) {
        stars.clear();
        for (int i = 0; i < count; i++) {
            stars.add(new Star(
                    random.nextDouble(),
                    random.nextDouble(),
                    pickGlyph(),
                    random.nextInt(BRIGHTNESS_LEVELS),
                    0.5 + random.nextDouble() * 1.5, // amplitude 0.5..2.0
                    0.8 + random.nextDouble() * 2.0, // speed ~0.8..2.8 rad/s (2-8s cycle)
                    random.nextDouble() * 2.0 * Math.PI,
                    pickColor()
            ));
        }
    }

    private char pickGlyph() {
        char[] pool = cp437Safe ? CP437_SAFE_GLYPHS : UNICODE_GLYPHS;
        return pool[random.nextInt(pool.length)];
    }

    private AnsiColor pickColor() {
        if (random.nextDouble() < 0.10) {
            return TINTS[random.nextInt(TINTS.length)];
        }
        return AnsiColor.WHITE;
    }

    /**
     * Renders a single frame at the current wall-clock time.
     *
     * @param graphics the text-graphics target
     * @param size     the area to render into
     */
    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) return;

        graphics.fillRectangle(0, 0, size.columns(), size.rows(),
                new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK));

        renderAtTime(graphics, size, System.currentTimeMillis() / 1000.0);
    }

    /** Visible for tests to avoid wall-clock time. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double timeSeconds) {
        if (size.columns() <= 0 || size.rows() <= 0) return;

        for (var star : stars) {
            int x = (int) (star.xPercent * size.columns());
            int y = (int) (star.yPercent * size.rows());
            if (x < 0 || x >= size.columns() || y < 0 || y >= size.rows()) continue;

            int brightness = computeBrightness(star, timeSeconds);
            if (brightness <= MIN_BRIGHTNESS) continue; // too dim, skip

            AnsiColor fg = chooseBrightnessColor(star.color, brightness);
            TextCell cell = new TextCell(star.character, fg, AnsiColor.BLACK,
                    brightness >= MAX_BRIGHTNESS ? new SGR[]{SGR.BOLD} : new SGR[0]);
            graphics.setCell(x, y, cell);
        }
    }

    /** Visible for tests. */
    public int computeBrightness(Star star, double timeSeconds) {
        double raw = star.baseBrightness() + star.amplitude() * Math.sin(timeSeconds * star.speed() + star.phase());
        return Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, (int) Math.round(raw)));
    }

    private AnsiColor chooseBrightnessColor(AnsiColor base, int brightness) {
        if (brightness == 1) {
            return base == AnsiColor.WHITE ? AnsiColor.BRIGHT_BLACK : dim(base);
        }
        if (brightness == 2) {
            return base == AnsiColor.WHITE ? AnsiColor.WHITE : base;
        }
        // brightness == 3
        return base == AnsiColor.WHITE ? AnsiColor.BRIGHT_WHITE : base;
    }

    private AnsiColor dim(AnsiColor color) {
        return switch (color) {
            case BRIGHT_BLUE -> AnsiColor.BLUE;
            case BRIGHT_CYAN -> AnsiColor.CYAN;
            case BRIGHT_YELLOW -> AnsiColor.YELLOW;
            case BRIGHT_RED -> AnsiColor.RED;
            default -> AnsiColor.BRIGHT_BLACK;
        };
    }

    /** {@inheritDoc} — records the new size for later use. */
    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
    }

    /** Marks the animation as running. */
    @Override
    public void start() {
        running = true;
    }

    /** Marks the animation as stopped. */
    @Override
    public void stop() {
        running = false;
    }

    /** Returns whether the animation is currently running. */
    @Override
    public boolean isRunning() {
        return running;
    }

    /** Returns the fixed target frame rate of 10 fps. */
    @Override
    public int targetFps() {
        return 10;
    }

    /** Visible for tests: returns the last size passed to onResize. */
    public TerminalSize lastSize() {
        return lastSize;
    }

    /** Package-visible for tests. */
    public record Star(
            double xPercent,
            double yPercent,
            char character,
            int baseBrightness,
            double amplitude,
            double speed,
            double phase,
            AnsiColor color
    ) {
    }
}
