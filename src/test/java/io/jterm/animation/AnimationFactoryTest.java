package io.jterm.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.widget.AbstractComponent;
import io.jterm.widget.animation.TwinkleStarfield;
import io.jterm.widget.animation.WarpStarfield;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnimationFactoryTest {

    @Test
    void randomReturnsNonNullAnimation() {
        var bg = AnimationFactory.random();
        assertNotNull(bg, "random() should return a non-null animation");
        assertTrue(bg instanceof AnimatedBackground);
    }

    @Test
    @DisplayName("randomWithSeedReturnsConsistentResult")
    void randomWithSeedReturnsConsistentResult() {
        var rng1 = new Random(42);
        var rng2 = new Random(42);
        var bg1 = AnimationFactory.random(rng1);
        var bg2 = AnimationFactory.random(rng2);
        assertEquals(bg1.getClass(), bg2.getClass(),
                "same seed should produce same animation type");
    }

    @Test
    @DisplayName("factoryCount returns expected number of registered animations")
    void factoryCountMatches() {
        // 5 pre-existing (TwinkleStarfield, WarpStarfield, PlasmaWash,
        // CircuitBoard, StarfieldBackground) + 19 new = 24
        assertEquals(24, AnimationFactory.factoryCount(),
                "expected exactly 24 animation factories, got " + AnimationFactory.factoryCount());
    }

    @Test
    void randomProducesAllTypesOverManyPicks() {
        Set<Class<? extends AnimatedBackground>> seen = new HashSet<>();
        var rng = new Random(123);
        for (int i = 0; i < 500; i++) {
            seen.add(AnimationFactory.random(rng).getClass());
        }
        assertEquals(AnimationFactory.availableClasses(), seen,
                "expected all animation types over many picks, got: " + seen);
    }

    @Test
    @DisplayName("every registered animation renders non-blank output")
    void everyAnimationRendersNonBlank() {
        var size = new TerminalSize(80, 24);
        var blank = new ScreenBuffer(size);

        // Collect one instance of every registered animation class using the factory.
        var byClass = new java.util.HashMap<Class<? extends AnimatedBackground>, AnimatedBackground>();
        var rng = new Random(42);
        while (byClass.size() < AnimationFactory.factoryCount()) {
            var bg = AnimationFactory.random(rng);
            byClass.putIfAbsent(bg.getClass(), bg);
        }

        for (var entry : byClass.entrySet()) {
            var bg = entry.getValue();
            if (bg instanceof AbstractComponent component) {
                component.setBounds(TerminalPosition.TOP_LEFT, size);
            }

            var rendered = new ScreenBuffer(size);
            bg.tick(0);
            bg.renderFrame(new TextGraphics(rendered), size);

            boolean hasContent = false;
            for (int r = 0; r < size.rows() && !hasContent; r++) {
                for (int c = 0; c < size.columns(); c++) {
                    if (!rendered.getCell(c, r).equals(blank.getCell(c, r))) {
                        hasContent = true;
                        break;
                    }
                }
            }
            assertTrue(hasContent, entry.getKey().getSimpleName() + " should render non-blank output");
        }
    }
}