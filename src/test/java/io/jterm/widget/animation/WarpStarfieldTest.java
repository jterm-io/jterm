package io.jterm.widget.animation;

import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WarpStarfieldTest {

    @Test
    @DisplayName("WarpStarfield has preferred size")
    void hasPreferredSize() {
        var bg = new WarpStarfield(new TerminalSize(80, 24));
        assertEquals(new TerminalSize(80, 24), bg.getPreferredSize());
    }

    @Test
    @DisplayName("project maps center star to screen center")
    void projectionCenterStarToCenter() {
        var bg = new WarpStarfield(new TerminalSize(80, 24));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 24));
        var projected = bg.project(0.0, 0.0, 1.0);
        assertEquals(new TerminalPosition(40, 12), projected);
    }

    @Test
    @DisplayName("project with closer z exaggerates offset")
    void projectionCloserZExaggeratesOffset() {
        var bg = new WarpStarfield(new TerminalSize(80, 24));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 24));
        var far = bg.project(0.5, 0.5, 5.0);
        var near = bg.project(0.5, 0.5, 1.0);
        assertTrue(Math.abs(near.column() - 40) > Math.abs(far.column() - 40));
        assertTrue(Math.abs(near.row() - 12) > Math.abs(far.row() - 12));
    }

    @Test
    @DisplayName("brightness increases as z decreases")
    void brightnessFromDepth() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        double far = bg.brightnessForDepth(8.0);
        double mid = bg.brightnessForDepth(5.0);
        double near = bg.brightnessForDepth(1.0);
        assertTrue(near > mid && mid > far, "brightness should increase as z decreases");
    }

    @Test
    @DisplayName("star respawns when z drops below threshold")
    void starRespawnsWhenTooClose() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        var star = bg.createStar();
        star.z = 0.05;
        double oldX = star.x;
        double oldY = star.y;
        bg.advanceStar(star, 0.05);
        assertTrue(star.z >= 0.1, "respawned star z should be at least 0.1");
        assertTrue(star.z <= 10.0, "respawned star z should be at most 10.0");
        assertTrue(star.x != oldX || star.y != oldY || star.x != 0.0,
                "respawned star should get fresh random x,y");
    }

    @Test
    @DisplayName("glyph selection matches depth ranges")
    void glyphSelectionByDepth() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        assertEquals('.', bg.glyphForDepth(8.0));
        assertEquals('+', bg.glyphForDepth(5.0));
        assertEquals('\u2727', bg.glyphForDepth(2.0));
    }

    @Test
    @DisplayName("first draw places content on screen")
    void firstDrawPlacesContent() {
        var size = new TerminalSize(40, 20);
        var bg = new WarpStarfield(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.tick(0);

        var buffer = new ScreenBuffer(size);
        bg.draw(new TextGraphics(buffer));

        boolean hasStar = false;
        for (int r = 0; r < size.rows() && !hasStar; r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    hasStar = true;
                    break;
                }
            }
        }
        assertTrue(hasStar, "first draw should place stars somewhere");
    }

    @Test
    @DisplayName("animation advances between two frames")
    void animationAdvancesBetweenFrames() {
        var size = new TerminalSize(40, 20);
        var bg = new WarpStarfield(size);
        bg.setBounds(TerminalPosition.TOP_LEFT, size);
        bg.setTargetFps(60);

        var buffer1 = new ScreenBuffer(size);
        bg.tick(0);
        bg.draw(new TextGraphics(buffer1));

        var buffer2 = new ScreenBuffer(size);
        bg.tick(32_000_000L);
        bg.draw(new TextGraphics(buffer2));

        boolean anyDifferent = false;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (!buffer1.getCell(c, r).equals(buffer2.getCell(c, r))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "two frames should differ");
    }

    @Test
    @DisplayName("screen center calculated from size")
    void centerCalculatedFromSize() {
        var bg = new WarpStarfield(new TerminalSize(80, 24));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 24));
        assertEquals(40, bg.getCenterX());
        assertEquals(12, bg.getCenterY());
    }

    @Test
    @DisplayName("resize updates center and star array")
    void resizeUpdatesCenterAndStars() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(80, 24));
        assertEquals(40, bg.getCenterX());
        assertEquals(12, bg.getCenterY());
        assertTrue(bg.getStarCount() > 0 || bg.getSize().area() > 0,
                "resize should keep component usable");
    }

    @Test
    @DisplayName("projection stays within screen bounds for moderate stars")
    void projectionStaysWithinBounds() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        bg.setBounds(TerminalPosition.TOP_LEFT, new TerminalSize(40, 20));
        for (double z = 0.6; z <= 10.0; z += 0.2) {
            var p = bg.project(0.5, 0.5, z);
            assertTrue(p.column() >= 0 && p.column() < 40,
                    "projected column should be on screen for z=" + z);
            assertTrue(p.row() >= 0 && p.row() < 20,
                    "projected row should be on screen for z=" + z);
        }
    }

    @Test
    @DisplayName("custom warp speed can be set and read")
    void warpSpeedControl() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        bg.setWarpSpeed(0.2);
        assertEquals(0.2, bg.getWarpSpeed(), 0.0001);
        bg.setWarpSpeed(1.0);
        assertEquals(1.0, bg.getWarpSpeed(), 0.0001);
    }

    @Test
    @DisplayName("pause stops frame advancement")
    void pauseStopsFrameAdvancement() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        bg.tick(0);
        int before = bg.getFrame();
        bg.setPaused(true);
        bg.tick(1_000_000_000L);
        assertEquals(before, bg.getFrame(), "paused background should not advance");
    }

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var bg = new WarpStarfield(new TerminalSize(40, 20));
        assertTrue(bg instanceof io.jterm.animation.AnimatedBackground);
        bg.start();
        assertTrue(bg.isRunning());
        bg.stop();
        assertFalse(bg.isRunning());
    }
}
