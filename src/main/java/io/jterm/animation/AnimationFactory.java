package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.widget.animation.TwinkleStarfield;
import io.jterm.widget.animation.WarpStarfield;
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
            () -> new WarpStarfield(DEFAULT_SIZE),
            () -> new MatrixRain(DEFAULT_SIZE),
            () -> new PlasmaWash(DEFAULT_SIZE),
            () -> new CircuitBoard(DEFAULT_SIZE),
            () -> new StarfieldBackground(DEFAULT_SIZE),
            () -> new TypewriterEffect(List.of(
                    "JTerm BBS",
                    "retro multi-user terminal community",
                    "please identify yourself")),
            () -> new LavaLamp(DEFAULT_SIZE),
            () -> new OceanWaves(DEFAULT_SIZE),
            () -> new TerrainFlyover(DEFAULT_SIZE),
            () -> new Aurora(DEFAULT_SIZE),
            () -> new RainStorm(DEFAULT_SIZE),
            () -> new Fireworks(DEFAULT_SIZE),
            () -> new DNAHelix(DEFAULT_SIZE),
            () -> new VoronoiCells(DEFAULT_SIZE)
    );

    private AnimationFactory() {}

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
