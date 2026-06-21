package io.jterm.animation;

import io.jterm.core.MockTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.window.DefaultTextGUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AnimationManagerTest {

    @Test
    @DisplayName("AnimationManager can be attached to DefaultTextGUI")
    void managerAttachesToGui() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var manager = new AnimationManager(gui);
        assertNotNull(manager);
    }

    @Test
    @DisplayName("AnimationManager accepts custom target FPS")
    void customTickRate() {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var manager = new AnimationManager(gui, 30);
        assertEquals(30, manager.getTargetFps());
    }

    @Test
    @DisplayName("AnimationManager clamping is consistent with background FPS")
    void clampingConsistent() {
        var manager = new AnimationManager(new DefaultTextGUI(new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)))), 1000);
        assertEquals(60, manager.getTargetFps());
    }

    @Test
    @DisplayName("AnimationManager tick is safe when no backgrounds registered")
    void tickNoBackgroundsSafe() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var manager = new AnimationManager(gui);
        assertDoesNotThrow(() -> {
            manager.tick(0);
            manager.tick(16_000_000L);
        });
    }

    @Test
    @DisplayName("AnimationManager stop prevents updates")
    void stopPreventsUpdates() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var manager = new AnimationManager(gui);
        var bg = new StarfieldBackground(new TerminalSize(80, 24));
        manager.register(bg);

        manager.tick(0);
        int frame1 = bg.getFrame();
        manager.stop();
        manager.tick(1_000_000_000L);
        assertEquals(frame1, bg.getFrame(), "no updates after stop");
    }

    @Test
    @DisplayName("AnimationManager unregister removes background")
    void unregisterRemovesBackground() throws IOException {
        var screen = new DefaultScreen(new MockTerminal(new TerminalSize(80, 24)));
        var gui = new DefaultTextGUI(screen);
        var manager = new AnimationManager(gui);
        var bg = new StarfieldBackground(new TerminalSize(80, 24));
        manager.register(bg);

        manager.tick(0);
        int frame1 = bg.getFrame();
        manager.unregister(bg);
        manager.tick(1_000_000_000L);
        assertEquals(frame1, bg.getFrame(), "unregistered background should not update");
    }
}
