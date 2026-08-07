package io.jterm.demo;

import io.jterm.core.AnsiTerminal;
import io.jterm.core.TerminalSize;
import io.jterm.core.input.KeyStroke;
import io.jterm.core.input.KeyType;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.DefaultScreen;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.sprite.AnimatedText;
import io.jterm.sprite.ParticleEffect;
import io.jterm.sprite.Sprite;
import io.jterm.sprite.SpriteRenderer;
import io.jterm.sprite.SpriteSheet;
import io.jterm.window.AbstractWindow;
import io.jterm.window.DefaultTextGUI;
import io.jterm.window.WindowHint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sprite demo: a BBS-style welcome screen that demonstrates the full sprite
 * library API — {@link Sprite}, {@link SpriteSheet}, {@link AnimatedText},
 * {@link ParticleEffect}, and {@link SpriteRenderer}.
 *
 * <p>The demo renders:
 * <ul>
 *   <li>A typewriter intro that spells out "JTERM BBS" at the top.</li>
 *   <li>A pulsing logo sprite in {@link Sprite.LoopMode#PING_PONG} mode,
 *       built from procedurally generated ANSI frames using block chars.</li>
 *   <li>A spinner sprite sheet (4-frame) played in {@link Sprite.LoopMode#LOOP}.</li>
 *   <li>A scrolling marquee (news ticker) at the bottom.</li>
 *   <li>Ambient sparkle particles in the background.</li>
 *   <li>Explosion and sparkle particle effects triggered by keypress.</li>
 * </ul>
 *
 * <p><b>Keyboard</b></p>
 * <ul>
 *   <li><b>e</b> — trigger an explosion particle effect at a random position</li>
 *   <li><b>s</b> — trigger a sparkle burst at a random position</li>
 *   <li><b>q</b> — quit</li>
 * </ul>
 */
public class SpriteDemo {

    private static final Random rng = new Random(42);

    private SpriteDemo() {}

    /**
     * Entry point — creates the terminal, screen, GUI, and runs the event loop.
     *
     * @param args unused
     * @throws IOException if terminal I/O fails
     */
    public static void main(String[] args) throws IOException {
        var terminal = new AnsiTerminal();
        var screen = new DefaultScreen(terminal);
        var gui = new DefaultTextGUI(screen);
        gui.getScreen().startScreen();

        var window = createSpriteWindow(gui);
        gui.addWindow(window);
        gui.updateScreen();

        var running = true;
        try {
            while (running) {
                // Tick all animations
                tickAnimations(window, 16);

                // Poll input
                while (true) {
                    var ks = screen instanceof DefaultScreen ds
                            ? ds.getTerminal().pollInput().orElse(null)
                            : null;
                    if (ks == null) break;

                    if (ks.type() == KeyType.CHARACTER) {
                        if (handleKey(window, ks, gui)) {
                            running = false;
                            break;
                        }
                    }
                }

                // Force a refresh every frame so animations are visible
                gui.requestRefresh();
                gui.updateScreen();
                Thread.sleep(16);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            gui.close();
        }
    }

    // ── Window creation ──────────────────────────────────────────

    /**
     * Create the sprite demo window and all animation state, attached to the
     * given GUI. The window uses {@link WindowHint#FULLSCREEN} with no
     * decorations so sprites render edge-to-edge.
     *
     * @param gui the GUI to attach the window to
     * @return the created sprite window
     */
    static SpriteWindow createSpriteWindow(DefaultTextGUI gui) {
        var window = new SpriteWindow();
        window.setHints(List.of(WindowHint.FULLSCREEN, WindowHint.NO_DECORATIONS));
        return window;
    }

    /**
     * Custom window that holds all sprite animation state and renders it
     * directly to the graphics context by overriding {@link #draw(TextGraphics)}.
     */
    static class SpriteWindow extends AbstractWindow {
        final AnimatedText typewriter;
        final AnimatedText marquee;
        final Sprite logoSprite;
        final Sprite spinnerSprite;
        final SpriteRenderer renderer;
        final List<ParticleEffect> particleEffects;
        final ParticleEffect ambientSparkles;
        volatile long lastTickMs;
        volatile int lastCols = 80;
        volatile int lastRows = 24;

        SpriteWindow() {
            this.typewriter = buildTypewriterIntro();
            this.marquee = buildMarqueeText();
            this.logoSprite = buildLogoSprite();
            this.spinnerSprite = buildSpinnerSprite();
            this.particleEffects = new CopyOnWriteArrayList<>();
            this.ambientSparkles = ParticleEffect.sparkle(40, 12, 15);
            this.ambientSparkles.setGravity(0);
            this.ambientSparkles.setFriction(0.95);
            this.lastTickMs = System.currentTimeMillis();
            // SpriteRenderer will be initialized on first render when we have a graphics context
            this.renderer = null;
        }

        /**
         * Override draw to render all sprite/animation elements directly to
         * the graphics context, bypassing the normal widget-based rendering.
         */
        @Override
        public void draw(TextGraphics g) {
            renderFrame(this, g);
        }
    }

    // ── Key handling ──────────────────────────────────────────────

    /**
     * Handle a key stroke. Returns {@code true} if the demo should quit,
     * {@code false} otherwise.
     *
     * @param window the sprite window
     * @param ks     the key stroke
     * @param gui    the GUI
     * @return true if the demo should quit
     */
    static boolean handleKey(SpriteWindow window, KeyStroke ks, DefaultTextGUI gui) {
        if (ks.type() != KeyType.CHARACTER) return false;
        char ch = ks.character();
        int maxCol = Math.max(1, window.lastCols - 10);
        int maxRow = Math.max(1, window.lastRows - 5);
        switch (ch) {
            case 'e', 'E' -> {
                int x = 5 + rng.nextInt(maxCol);
                int y = 3 + rng.nextInt(maxRow);
                var explosion = ParticleEffect.explosion(x, y, 20);
                explosion.setGravity(0.3);
                explosion.setFriction(0.9);
                window.particleEffects.add(explosion);
                return false;
            }
            case 's', 'S' -> {
                int x = 5 + rng.nextInt(maxCol);
                int y = 3 + rng.nextInt(maxRow);
                var sparkle = ParticleEffect.sparkle(x, y, 15);
                sparkle.setGravity(0);
                sparkle.setFriction(0.95);
                window.particleEffects.add(sparkle);
                return false;
            }
            case 'q', 'Q' -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ── Animation tick ────────────────────────────────────────────

    /**
     * Advance all animations by the given elapsed time.
     *
     * @param window    the sprite window
     * @param deltaMs   elapsed milliseconds
     */
    static void tickAnimations(SpriteWindow window, long deltaMs) {
        window.typewriter.tick(deltaMs);
        window.marquee.tick(deltaMs);
        window.logoSprite.advance(deltaMs);
        window.spinnerSprite.advance(deltaMs);
        window.ambientSparkles.tick(deltaMs);

        // Tick and prune finished particle effects
        var iter = window.particleEffects.iterator();
        while (iter.hasNext()) {
            var fx = iter.next();
            fx.tick(deltaMs);
            if (!fx.isAlive()) {
                window.particleEffects.remove(fx);
            }
        }

        // Periodically spawn ambient sparkles to keep the background alive
        if (rng.nextInt(20) == 0) {
            int x = rng.nextInt(Math.max(1, window.lastCols));
            int y = rng.nextInt(Math.max(1, window.lastRows));
            window.ambientSparkles.spawnSparkle(x, y, 1);
        }
    }

    // ── Frame rendering ───────────────────────────────────────────

    /**
     * Render the complete sprite demo frame to the given graphics buffer.
     *
     * @param window the sprite window
     * @param g      the graphics context
     */
    static void renderFrame(SpriteWindow window, TextGraphics g) {
        var size = g.getSize();
        int cols = size.columns();
        int rows = size.rows();
        window.lastCols = cols;
        window.lastRows = rows;

        // Fill background with a dark blue
        var bgCell = new TextCell(' ', AnsiColor.BRIGHT_BLUE, AnsiColor.BLACK);
        g.fillRectangle(0, 0, cols, rows, bgCell);

        // Render ambient sparkles (background layer)
        window.ambientSparkles.render(g);

        // Render typewriter intro at the top (row 1)
        window.typewriter.render(g, 2, 1);

        // Render the pulsing logo sprite (centered area)
        int logoCol = (cols - window.logoSprite.getWidth()) / 2;
        if (logoCol < 0) logoCol = 0;
        int logoRow = 4;
        window.logoSprite.render(g, logoCol, logoRow);

        // Render the spinner sprite sheet (below the logo, left side)
        int spinnerCol = 8;
        int spinnerRow = 12;
        window.spinnerSprite.render(g, spinnerCol, spinnerRow);

        // Render a help text line
        var helpCell = new TextCell(' ', AnsiColor.BRIGHT_YELLOW, AnsiColor.BLACK);
        String help = " [e] Explosion   [s] Sparkle   [q] Quit ";
        g.drawString(Math.max(0, (cols - help.length()) / 2), 10, help, helpCell);

        // Render scrolling marquee at the bottom (row = rows - 2)
        window.marquee.render(g, 0, rows - 2);

        // Render active particle effects (top layer)
        for (var fx : window.particleEffects) {
            fx.render(g);
        }

        // Render a border frame around the demo
        var borderCell = new TextCell(' ', AnsiColor.BRIGHT_CYAN, AnsiColor.BLACK);
        g.drawRectangle(0, 0, cols, rows, borderCell);
    }

    // ── Particle effects accessor (for tests) ─────────────────────

    /**
     * Get the list of active particle effects from the window.
     *
     * @param window the sprite window
     * @return the list of particle effects
     */
    static List<ParticleEffect> getParticleEffects(SpriteWindow window) {
        return window.particleEffects;
    }

    // ── Procedural frame generation ───────────────────────────────

    /**
     * Build the pulsing logo frames — a series of ANSI frames using block
     * characters that create a pulsing/glowing effect. The frames cycle
     * through different brightness levels using SGR color codes.
     *
     * @return list of ANSI frame strings
     */
    static List<String> buildLogoFrames() {
        var frames = new ArrayList<String>();
        // 4 frames of pulsing: dim → normal → bright → normal (ping-pong)
        String[] colors = {
            "\u001B[94m",   // bright blue (dim)
            "\u001B[96m",   // bright cyan (normal)
            "\u001B[97m",   // bright white (bright)
            "\u001B[96m",   // bright cyan (normal)
        };
        String reset = "\u001B[0m";

        // Logo text rendered with block characters
        String[] logoLines = {
            "  ██  ██  █████  ██████  ██████   ██████",
            "  ██  ██  ██  ██    ██  ██  ██   ██  ██",
            "  ██████  █████     ██  ██       ██  ██",
            "  ██████  ██  ██    ██  ██  ██   ██  ██",
            "  ██  ██  █████  ██████  ██████   ██████",
        };

        for (String color : colors) {
            var sb = new StringBuilder();
            for (int i = 0; i < logoLines.length; i++) {
                sb.append(color);
                sb.append(logoLines[i]);
                sb.append(reset);
                if (i < logoLines.length - 1) sb.append('\n');
            }
            frames.add(sb.toString());
        }
        return frames;
    }

    /**
     * Build the logo sprite from {@link #buildLogoFrames()} in PING_PONG mode.
     *
     * @return a pulsing logo sprite
     */
    static Sprite buildLogoSprite() {
        var frames = buildLogoFrames();
        var sprite = new Sprite(frames);
        sprite.setFrameMs(300);
        sprite.setLoopMode(Sprite.LoopMode.PING_PONG);
        return sprite;
    }

    /**
     * Build a 4-frame spinner sprite sheet programmatically. Each frame is
     * a single-cell animation showing a rotating spinner using characters
     * |, /, -, \\.
     *
     * @return a sprite sheet with 4 spinner frames
     */
    static SpriteSheet buildSpinnerSheet() {
        // Create a 4-column, 1-row grid, each cell 1x1
        // Frame 0: |  Frame 1: /  Frame 2: -  Frame 3: \
        var sb = new StringBuilder();
        sb.append("\u001B[96m|\u001B[0m");
        sb.append("\u001B[96m/\u001B[0m");
        sb.append("\u001B[96m-\u001B[0m");
        sb.append("\u001B[96m\\\u001B[0m");

        return new SpriteSheet(sb.toString(), 1, 1, 4, 1);
    }

    /**
     * Build the spinner sprite from the sprite sheet in LOOP mode.
     *
     * @return a looping spinner sprite
     */
    static Sprite buildSpinnerSprite() {
        var sheet = buildSpinnerSheet();
        return sheet.toSprite(150, Sprite.LoopMode.LOOP);
    }

    /**
     * Build the typewriter intro animation that spells out "JTERM BBS".
     *
     * @return a typewriter animated text
     */
    static AnimatedText buildTypewriterIntro() {
        return AnimatedText.typewriter("JTERM BBS — Welcome to the Sprite Demo!", 80);
    }

    /**
     * Build the scrolling marquee (news ticker) at the bottom of the screen.
     *
     * @return a left-scrolling animated text
     */
    static AnimatedText buildMarqueeText() {
        String news = "*** JTERM BBS NEWS ***  Press E for explosion, S for sparkle, Q to quit  "
                + "*** Sprite Library Demo ***  Featuring Sprite, SpriteSheet, AnimatedText, "
                + "ParticleEffect, and SpriteRenderer  ***  ";
        // Use a wide viewport — renderFrame clips to actual terminal width
        return AnimatedText.scrollLeft(news, 120, 100);
    }
}