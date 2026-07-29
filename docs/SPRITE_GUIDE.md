# JTerm Sprite & Animation Library Guide

The `io.jterm.sprite` package provides a complete sprite and animation
system for terminal UIs. It is built on top of the existing
[`AnsiArtRenderer`](../src/main/java/io/jterm/screen/AnsiArtRenderer.java)
and `TextGraphics` infrastructure, so anything you can draw with ANSI art can
be animated as a sprite.

## Contents

- [API Overview](#api-overview)
  - [Sprite](#sprite)
  - [SpriteSheet](#spritesheet)
  - [AnimatedText](#animatedtext)
  - [ParticleEffect](#particleeffect)
  - [SpriteRenderer](#spriterenderer)
- [Code Examples](#code-examples)
  - [Loading a sprite from multiple .ans files](#loading-a-sprite-from-multiple-ans-files)
  - [Loading a sprite sheet (grid layout)](#loading-a-sprite-sheet-grid-layout)
  - [Looping, ping-pong, and one-shot animations](#looping-ping-pong-and-one-shot-animations)
  - [Scrolling marquee text](#scrolling-marquee-text)
  - [Typewriter intro](#typewriter-intro)
  - [Explosion effect on a hit](#explosion-effect-on-a-hit)
  - [Sparkle background](#sparkle-background)
  - [Running multiple animations concurrently](#running-multiple-animations-concurrently)
- [BBS Feature Ideas](#bbs-feature-ideas)
- [Creating ANSI Art Sprite Frames](#creating-ansi-art-sprite-frames)

---

## API Overview

### Sprite

`Sprite` is the core class. It holds an ordered list of ANSI-art frames (each
a raw string with embedded SGR color codes) and an animation state machine
that advances the frame index based on elapsed milliseconds.

```java
public class Sprite {
    public enum LoopMode { ONCE, LOOP, PING_PONG }

    public void addFrame(String ansiContent);
    public static Sprite fromFiles(List<Path> paths) throws IOException;
    public void advance(long elapsedMs);
    public void reset();
    public String getCurrentFrame();
    public int getCurrentFrameIndex();
    public int getFrameCount();
    public int getFrameMs();
    public void setFrameMs(int frameMs);
    public LoopMode getLoopMode();
    public void setLoopMode(LoopMode mode);
    public int getWidth();
    public int getHeight();
    public boolean isFinished();
    public void render(TextGraphics g, int col, int row);
}
```

- **Frame timing**: `setFrameMs(int)` controls milliseconds per frame.
  Default is 100ms (10 FPS), suitable for most BBS art. For smoother motion,
  lower to 50–80ms; for a "flicker" BBS aesthetic, raise to 200–500ms.
- **Loop modes**:
  - `ONCE` — play through all frames then stop at the last frame. `isFinished()` becomes true.
  - `LOOP` — wrap back to frame 0 after the last frame. Never finishes.
  - `PING_PONG` — forward to end, then reverse to start, repeating. Never finishes.
- **Size**: `getWidth()` / `getHeight()` return the maximum visible dimensions across all frames (SGR escapes don't count toward width).
- **Rendering**: `render(TextGraphics, col, row)` parses the current frame's ANSI string via `AnsiArtRenderer` and writes it into the buffer at the given offset.

### SpriteSheet

`SpriteSheet` loads a single ANSI art file and slices it into a grid of frames.
Use it when your art tool exports a tile sheet (multiple frames laid out in a
grid) rather than one file per frame.

```java
public class SpriteSheet {
    public SpriteSheet(String content, int cellWidth, int cellHeight, int cols, int rows);
    public static SpriteSheet fromFile(Path path, int cellWidth, int cellHeight, int cols, int rows)
            throws IOException;
    public int frameCount();
    public String frameAt(int index);
    public Sprite toSprite();
    public Sprite toSprite(int frameMs, Sprite.LoopMode loopMode);
}
```

- Frames are extracted in **row-major order**: `[0][1][2]` / `[3][4][5]`.
- SGR escape sequences are preserved within each cell. The sheet carries
  active SGR state from the start of each source line into the extracted
  cell, so color state continues correctly across cell boundaries.
- Lines shorter than `cellWidth * cols` are padded with spaces.
- `toSprite()` is a convenience that builds a playable `Sprite` with default
  timing and `LOOP` mode.

### AnimatedText

`AnimatedText` provides standalone text animations. Each instance has a
`Mode` (TYPEWRITER, SCROLL_LEFT, SCROLL_RIGHT, or BLINK) and a `stepMs`
parameter controlling the speed.

```java
public class AnimatedText {
    public enum Mode { TYPEWRITER, SCROLL_LEFT, SCROLL_RIGHT, BLINK }

    public static AnimatedText typewriter(String text, int stepMs);
    public static AnimatedText scrollLeft(String text, int viewportWidth, int stepMs);
    public static AnimatedText scrollRight(String text, int viewportWidth, int stepMs);
    public static AnimatedText blink(String text, List<Color> colors, int stepMs);
    public void tick(long elapsedMs);
    public String getText();
    public boolean isComplete();
    public void reset();
    public void render(TextGraphics g, int col, int row);
}
```

- **Typewriter**: reveals one character per `stepMs`. `isComplete()` is true once all chars are shown.
- **Scroll**: shifts the viewport across the text. `viewportWidth` is the number of terminal cells visible; text scrolls through it and wraps.
- **Blink**: text stays fixed but the foreground color cycles through the supplied `List<Color>` variants.

### ParticleEffect

`ParticleEffect` is a lightweight particle system for terminal visuals.
Particles are single characters with velocities, optional gravity/friction,
and a lifetime in milliseconds.

```java
public class ParticleEffect {
    public static ParticleEffect explosion(int x, int y, int count);
    public static ParticleEffect sparkle(int x, int y, int count);
    public void spawnExplosion(int x, int y, int count);
    public void spawnSparkle(int x, int y, int count);
    public void spawn(double x, double y, double vx, double vy,
                       char glyph, Color color, long lifetimeMs);
    public void tick(long deltaMs);
    public void render(TextGraphics g);
    public int particleCount();
    public boolean isAlive();
    public void reset();
    public void setGravity(double gravity);
    public void setFriction(double friction);
}
```

- **Explosion**: radial burst — each particle gets a random outward velocity
  and a color from a warm palette (red, yellow, cyan, white, magenta).
- **Sparkle**: random twinkling particles with a star/glyph palette and
  smaller velocities, suitable for ambient backgrounds.
- **Gravity**: positive values pull particles downward (rows increase).
  Typical value: `0.5`–`1.0`.
- **Friction**: `1.0` = no friction; values `< 1.0` slow particles over time
  (e.g. `0.5` = strong damping). Use `0.9` for a gentle slow-down.
- **Lifetime**: each particle has a `lifetimeMs` (default 300–700ms). Dead
  particles are removed on each `tick`.

### SpriteRenderer

`SpriteRenderer` drives sprite animations on virtual threads and renders
them to a `TextGraphics` buffer. Multiple animations can run concurrently;
each returns an `AnimationHandle` for stopping it individually.

```java
public class SpriteRenderer {
    public SpriteRenderer(TextGraphics graphics, TerminalSize size);
    public void render(Sprite sprite, int col, int row);
    public AnimationHandle playOnce(Sprite sprite, int col, int row, Runnable callback);
    public AnimationHandle playLoop(Sprite sprite, int col, int row);
    public void stopAll();
}

public static class AnimationHandle {
    public void stop();
    public boolean isRunning();
}
```

- `render(sprite, col, row)` draws the current frame at position. When the
  same sprite is rendered at a new position, the previous region is cleared
  to avoid stale artifacts.
- `playOnce` runs a sprite in `ONCE` mode on a virtual thread, firing the
  callback when the animation completes. Useful for "intro" animations that
  transition to another screen on completion.
- `playLoop` runs a sprite in `LOOP` (or `PING_PONG`) mode on a virtual
  thread, returning a handle that can stop the loop.
- `stopAll()` stops every running animation managed by this renderer.
- Thread-safe: the renderer uses a `CopyOnWriteArrayList` of handles and
  synchronizes on the graphics buffer during draws, so multiple animations
  can update concurrently without corrupting the screen.

---

## Code Examples

### Loading a sprite from multiple .ans files

```java
import java.nio.file.Path;
import java.util.List;

Sprite walkCycle = Sprite.fromFiles(List.of(
    Path.of("art/walk1.ans"),
    Path.of("art/walk2.ans"),
    Path.of("art/walk3.ans"),
    Path.of("art/walk4.ans")
));
walkCycle.setFrameMs(120);
walkCycle.setLoopMode(Sprite.LoopMode.LOOP);
```

### Loading a sprite sheet (grid layout)

```java
import java.nio.file.Path;

// 4 columns × 2 rows of 10×6-cell frames
SpriteSheet sheet = SpriteSheet.fromFile(
    Path.of("art/character_sheet.ans"),
    10, 6, 4, 2
);
Sprite sprite = sheet.toSprite(80, Sprite.LoopMode.LOOP);
```

### Looping, ping-pong, and one-shot animations

```java
// Loop forever (e.g. an idle animation)
sprite.setLoopMode(Sprite.LoopMode.LOOP);
AnimationHandle loopHandle = renderer.playLoop(sprite, 10, 5);

// Ping-pong (e.g. a breathing/pulsing effect)
sprite.setLoopMode(Sprite.LoopMode.PING_PONG);
AnimationHandle ppHandle = renderer.playLoop(sprite, 30, 5);

// One-shot with completion callback
sprite.setLoopMode(Sprite.LoopMode.ONCE);
renderer.playOnce(sprite, 50, 5, () -> {
    System.out.println("Animation complete — advance to next screen");
});
```

### Scrolling marquee text

```java
import io.jterm.style.AnsiColor;
import java.util.List;

AnimatedText marquee = AnimatedText.scrollLeft(
    "*** WELCOME TO LARCHNET BBS — DOWNLOADS, GAMES, AND MESSAGES ***",
    40,   // viewport width
    80    // ms per column shift
);

// In your render loop:
marquee.tick(deltaMs);
marquee.render(graphics, 0, 0);
```

### Typewriter intro

```java
AnimatedText intro = AnimatedText.typewriter(
    "LARCHNET BBS v2.1\nConnecting...\nAuthentication required.",
    40  // ms per char
);

// In your render loop:
intro.tick(deltaMs);
intro.render(graphics, 10, 5);
if (intro.isComplete()) {
    // proceed to login screen
}
```

### Explosion effect on a hit

```java
ParticleEffect boom = ParticleEffect.explosion(40, 12, 30);
boom.setGravity(0.8);  // particles fall

// In your render loop:
boom.tick(deltaMs);
boom.render(graphics);
if (!boom.isAlive()) {
    // explosion finished; remove from scene
}
```

### Sparkle background

```java
ParticleEffect sparkles = ParticleEffect.sparkle(40, 12, 20);

// In your render loop, refresh sparkles periodically:
if (!sparkles.isAlive()) {
    sparkles.spawnSparkle(40, 12, 20);
}
sparkles.tick(deltaMs);
sparkles.render(graphics);
```

### Running multiple animations concurrently

```java
SpriteRenderer renderer = new SpriteRenderer(graphics, terminalSize);

Sprite player = sheet.toSprite(80, Sprite.LoopMode.LOOP);
Sprite enemy  = enemySheet.toSprite(100, Sprite.LoopMode.LOOP);

AnimationHandle h1 = renderer.playLoop(player, 10, 10);
AnimationHandle h2 = renderer.playLoop(enemy,  60, 10);

// Later, when the player moves or the enemy dies:
h1.stop();
h2.stop();

// Or stop everything at once:
renderer.stopAll();
```

---

## BBS Feature Ideas

The sprite library is intentionally generic so it can power a wide variety of
BBS features. Here are some ideas:

### Door games
- **Character animation**: load a walk-cycle sprite sheet and play it in `LOOP` mode while the player moves. Switch to an attack sprite in `ONCE` mode when the player hits something.
- **Tile maps**: use `SpriteSheet` to slice a tile-map image into individual tiles. Render the visible portion of the map each frame.
- **Combat effects**: spawn `ParticleEffect.explosion(...)` at the impact point when a projectile hits. Add gravity for falling debris.
- **Health bars**: use `AnimatedText.blink(...)` with red/yellow/green colors to pulse a low-health warning.

### Welcome screen
- **Logo reveal**: use `AnimatedText.typewriter(...)` to type the BBS name character-by-character on connection, mimicking a 2400 baud handshake.
- **Scrolling marquee**: use `AnimatedText.scrollLeft(...)` to display news/announcements in a single-line ticker at the top or bottom of the screen.
- **Animated logo**: load a multi-frame ANSI art logo as a `Sprite` and play it in `PING_PONG` mode for a breathing/pulsing effect.

### Chat reactions
- **Emote bursts**: when a user sends `:)`, spawn a sparkle `ParticleEffect` at their cursor position.
- **Typing indicator**: use `AnimatedText.blink(...)` with three dots (`...`) cycling between dim and bright colors.
- **Message slide-in**: use `AnimatedText.scrollRight(...)` to slide new chat messages in from the left edge.

### Score celebrations
- **Confetti**: spawn multiple `ParticleEffect.explosion(...)` calls at random positions across the top of the screen, with `setGravity(0.5)` so the particles fall like confetti.
- **Score popup**: use `AnimatedText.typewriter(...)` to reveal a new high score, then `playOnce` a celebratory sprite animation.
- **Fireworks**: chain explosions with short delays for a fireworks finale.

### Screen transitions
- **Dissolve**: combined with the existing `DissolveTransition`, use a particle effect that scatters the outgoing screen content as particles.
- **Wipe**: use `AnimatedText.scrollLeft(...)` on the outgoing screen content to slide it off-screen.
- **Morph**: build a sprite with the old screen as frame 0 and the new screen as frame N, then play it in `ONCE` mode for a quick morph.

### Loader animations
- **Spinner**: build a 4-frame sprite with `|`, `/`, `-`, `\` and play it in `LOOP` mode next to a "Loading..." label.
- **Progress bar**: use `AnimatedText.blink(...)` on the incomplete portion of the bar to show it's active.
- **Fancy loader**: play a sprite-sheet animation (e.g. a rotating gear or pulsing logo) while waiting for a download.

### MUD entity rendering
- **NPC idle animation**: each NPC has an idle sprite (e.g. a guard breathing) played in `PING_PONG` mode.
- **Combat rounds**: when the player attacks, play a "swing" sprite in `ONCE` mode, then trigger an explosion particle effect at the target.
- **Spell effects**: use `ParticleEffect.sparkle(...)` for a magic-cast animation, then `explosion(...)` for the impact.
- **Movement trails**: as an entity moves, spawn a few sparkle particles at its previous position that fade out.

---

## Creating ANSI Art Sprite Frames

### Tools

- **Moebius** (https://blocktronics.org/) — modern cross-platform ANSI art editor with layers and animation support. Exports `.ans` files directly.
- **PablumDraw** — classic ANSI editor for Windows; works well under WINE.
- **ACiDDraw** — vintage ANSI editor; for authentic BBS-era art.
- **SyncTERM** — terminal emulator that renders ANSI art natively; useful for previewing frames.
- **textedit / vim** — for simple ASCII-art frames you can author by hand.

### Tips for animation frames

1. **Keep frame dimensions consistent.** All frames in a sprite should have
   the same width and height. The `Sprite` class tracks the max across all
   frames, but mismatched sizes will leave blank cells when a smaller frame
   replaces a larger one.

2. **Use SGR codes, not extended characters, for color.** The
   `AnsiArtRenderer` parses `ESC[38;5;Nm` (256-color) and `ESC[31m` (16-color)
   sequences. Stick to these for maximum compatibility.

3. **Frame rate considerations.** BBS terminals render at 5–15 FPS over a
   network. Use `setFrameMs(100)` (10 FPS) as a baseline. For "cinematic"
   intros, 200ms (5 FPS) feels authentic. For smooth game animations, try
   50–80ms (12–20 FPS), but be aware that remote terminals may not keep up.

4. **Sprite sheets vs. individual files.** If your art tool supports
   animation (e.g. Moebius), export a sprite sheet — a single `.ans` file
   with all frames laid out in a grid. Then use `SpriteSheet.fromFile(...)`
   to slice it. If your tool only exports one frame per file, use
   `Sprite.fromFiles(...)`.

5. **Test with `AnsiArtRenderer` first.** Before loading a frame into a
   sprite, render it with `AnsiArtRenderer.render(g, ansiContent)` to verify
   it looks right. This isolates art issues from animation issues.

6. **Keep cells small.** Terminal cells are coarse. A 10×6-cell sprite is
   already 60 characters of art per frame. Larger sprites are harder to
   author and slower to render. For characters, 8×4 to 16×8 is a sweet spot.

7. **Use CP437 block characters for smooth motion.** Characters like `░`,
   `▒`, `▓`, `█` let you fake anti-aliasing and create the illusion of
   sub-cell motion between frames.

8. **Author ping-pong sequences as half-frames.** If you intend to play a
   sprite in `PING_PONG` mode, author only the forward half (e.g. 4 frames:
   pose A, B, C, D). The renderer will play A→B→C→D→C→B→A→B→... so you get
   7 visual steps from 4 frames.