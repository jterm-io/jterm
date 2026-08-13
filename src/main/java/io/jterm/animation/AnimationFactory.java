package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.widget.animation.TwinkleStarfield;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Factory that randomly selects one of the available {@link AnimatedBackground}
 * implementations. Use {@link #random()} for a fresh pick on each call, or
 * {@link #random(Random)} to supply your own random source.
 */
public final class AnimationFactory {

    private static final TerminalSize DEFAULT_SIZE = new TerminalSize(80, 24);

    private static final List<Supplier<AnimatedBackground>> FACTORIES = List.of(
            () -> new TwinkleStarfield(new Random(), true),
            () -> new PlasmaWash(DEFAULT_SIZE),
            () -> new StarfieldBackground(DEFAULT_SIZE),
            () -> new VoronoiCells(DEFAULT_SIZE),
            () -> new MoirePatterns(DEFAULT_SIZE),
            () -> new io.jterm.widget.animation.WarpStarfield(DEFAULT_SIZE)
    );

    private AnimationFactory() {}

    /** Returns a new {@link SparkleCornersEffect} for animated borders. */
    public static SparkleCornersEffect sparkleCorners() {
        return new SparkleCornersEffect();
    }

    /** Returns a new {@link MarchingAntsEffect} with default dash=3, gap=2. */
    public static MarchingAntsEffect marchingAnts() {
        return new MarchingAntsEffect();
    }

    /** Returns a new {@link RotatingDashCornersEffect} for animated borders. */
    public static RotatingDashCornersEffect rotatingDashCorners() {
        return new RotatingDashCornersEffect();
    }

    /** Returns a new {@link ColorPulseEffect} with default 10 frames per color. */
    public static ColorPulseEffect colorPulse() {
        return new ColorPulseEffect(10);
    }

    /** Returns a new {@link ScanningLineEffect} with default highlight char (■) and speed 1. */
    public static ScanningLineEffect scanningLine() {
        return new ScanningLineEffect();
    }

    /** Returns a randomly chosen animation using a fresh {@link Random}. */
    public static AnimatedBackground random() {
        return random(new Random());
    }

    /** Returns a randomly chosen animation using the supplied {@link Random}. */
    public static AnimatedBackground random(Random random) {
        var factory = FACTORIES.get(random.nextInt(FACTORIES.size()));
        return factory.get();
    }

    /** Visible for tests: returns the count of registered animation factories. */
    public static int factoryCount() {
        return FACTORIES.size();
    }

    /** Visible for tests: returns the distinct classes produced by all factories. */
    public static Set<Class<? extends AnimatedBackground>> availableClasses() {
        return FACTORIES.stream()
                .map(Supplier::get)
                .map(AnimatedBackground::getClass)
                .collect(Collectors.toSet());
    }
}
