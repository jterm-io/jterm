package io.jterm.animation;

import io.jterm.style.AnsiColor;

/**
 * Animated border effect that cycles the border foreground color
 * through ANSI colors, creating a gentle hue shift around the border
 * perimeter.
 *
 * <p>On each color step, all border cells (corners + edges) share
 * the same foreground color. The color changes every
 * {@code framesPerColor} frames (default 10 ≈ 1 second at 10fps),
 * not every frame, for a smoother visual transition.</p>
 *
 * <p>The color sequence is: RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
 * then wraps back to RED.</p>
 */
public class ColorPulseEffect implements AnimatedBorderEffect {

    /** The ANSI color cycle for 16-color terminals. */
    private static final AnsiColor[] COLORS = {
            AnsiColor.RED, AnsiColor.GREEN, AnsiColor.YELLOW,
            AnsiColor.BLUE, AnsiColor.MAGENTA, AnsiColor.CYAN, AnsiColor.WHITE
    };

    private final int framesPerColor;

    /** Creates a ColorPulseEffect with the default 10 frames per color. */
    public ColorPulseEffect() {
        this(10);
    }

    /** Creates a ColorPulseEffect with the given frames-per-color speed.
     *
     * @param framesPerColor how many frames each color is held before transitioning
     */
    public ColorPulseEffect(int framesPerColor) {
        if (framesPerColor < 1) throw new IllegalArgumentException("framesPerColor must be >= 1");
        this.framesPerColor = framesPerColor;
    }

    @Override
    public void update(long frame, BorderContext ctx) {
        int colorIndex = (int) ((frame / framesPerColor) % COLORS.length);
        ctx.setBorderColor(COLORS[colorIndex]);
    }

    @Override
    public String name() {
        return "color-pulse";
    }
}