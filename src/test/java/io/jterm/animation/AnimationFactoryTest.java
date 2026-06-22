package io.jterm.animation;

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
    void randomProducesAllTypesOverManyPicks() {
        Set<Class<? extends AnimatedBackground>> seen = new HashSet<>();
        var rng = new Random(123);
        for (int i = 0; i < 500; i++) {
            seen.add(AnimationFactory.random(rng).getClass());
        }
        assertEquals(AnimationFactory.availableClasses(), seen,
                "expected all animation types over many picks, got: " + seen);
    }
}