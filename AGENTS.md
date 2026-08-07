# AGENTS.md — Guide for AI Coding Agents Working on jterm

## What is jterm?

jterm is a pure-Java terminal UI toolkit (TTY/TUI framework). It provides a full widget system, screen buffers, layout managers, charts, animations, and themes for building rich terminal applications. It is NOT an ANSI library — ANSI escape codes are just the output transport, the same way Swing uses pixels.

## Build Commands

```bash
mvn compile          # Compile
mvn test             # Run all 2,187 tests
mvn install -DskipTests  # Install to local ~/.m2
mvn javadoc:javadoc  # Generate Javadoc HTML
```

Requires JDK 26+.

## Architecture

Three layers, each building on the one below:

1. **Terminal** (`io.jterm.core`) — Raw ANSI/VT100, stty raw mode, InputDecoder, AnsiTerminal (local), SocketTerminal (network), MockTerminal (testing)
2. **Screen** (`io.jterm.screen`) — Double-buffered, diff-based refresh, SgrStateTracker for batched SGR transitions, ScreenBuffer, AnsiArtRenderer
3. **GUI** (`io.jterm.widget`, `io.jterm.layout`, `io.jterm.window`) — Component/Container tree, LayoutManagers, TextGUI event loop, Window management, FocusManager

## Key Rules

1. **No native dependencies.** No JNI, no C libraries. Pure Java only.
2. **No code without a failing test first.** TDD is mandatory. Write the test, watch it fail, implement, watch it pass.
3. **No `AnsiColor.DEFAULT` for backgrounds.** Always use theme colors via `ThemeManager.active()`.
4. **No `RgbColor` in terminal output.** Use `AnsiColor.blendAnsi()` for color interpolation.
5. **No `SGR.REVERSE` for cursors.** Swap fg/bg directly in the TextCell — not all terminals implement REVERSE.
6. **No native terminal cursor.** Draw your own virtual cursor. The real cursor is hidden (`ESC[?25l`).
7. **Widgets query theme at draw time**, not in constructors. Theme can change at runtime.
8. **Event loop uses blocking input**, not sleep-poll. `getInput()` calls `pollInput(5ms)` which blocks on the terminal's `BlockingQueue` and returns immediately when input arrives. No `Thread.sleep(16)` — the loop calls `Thread.yield()` when idle to let background threads run.
9. **Call `gui.requestRefresh()` before `gui.updateScreen()`** if handling keys directly.
10. **`TextCell` is immutable.** Safe to share between buffers, no defensive copies needed.

## Testing Patterns

- Use `MockTerminal` for all widget tests — no real terminal needed
- Draw widgets to a `ScreenBuffer` and assert on cell contents
- For cursor tests: call `box.setFocused(true)` before drawing (cursor only renders when focused)
- For theme tests: verify every cell type uses theme bg colors (see `ChartThemeTest` as a template)
- Screen buffers are inspectable: `buf.getCell(col, row)` returns a `TextCell` with char, fg, bg, SGR

## Common Pitfalls

- **stty cleanup**: If a test crashes without restoring terminal state, run `stty sane` or `reset`
- **Double-width chars**: Always use `TerminalTextUtils.getColumnWidth()` for CJK/fullwidth
- **JAR version**: Built JAR is `jterm-0.1.0-SNAPSHOT.jar` — check `ls target/*.jar` before referencing
- **JaCoCo**: Use 0.8.15+ for Java 26 compatibility
- **ListBox.getItems() returns a defensive copy**: Use `clearItems()` to clear, not `getItems().clear()`
- **Label centers text**: Tests checking character positions must account for horizontal centering offset

## File Structure

```
src/main/java/io/jterm/
  core/          — Terminal, AnsiTerminal, SocketTerminal, MockTerminal, InputDecoder, KeyStroke, KeyType
  screen/        — Screen, ScreenBuffer, DefaultScreen, AnsiArtRenderer, RefreshType
  graphics/      — TextGraphics, ClippedTextGraphics, SubTextGraphics
  style/         — AnsiCodes, AnsiColor, RgbColor, TextCell, Theme, ThemeManager, SGR, Cp437
  layout/        — BorderLayout, GridLayout, LinearLayout, LayoutManager
  widget/        — Button, Label, TextBox, TextArea, Table, ListBox, Panel, Menu, Chart, etc.
    model/       — TableModel, ListModel, GridModel and their default implementations
    chart/       — Chart, ChartSeries, ChartType, ChartAxisConfig
    animation/   — TwinkleStarfield, WarpStarfield
  animation/     — 25+ animated backgrounds and transitions
  sprite/        — Sprite, SpriteSheet, AnimatedText, ParticleEffect
  window/        — TextGUI, DefaultTextGUI, Window, WindowManager, Dialog
  event/         — FocusManager, Listener
  gui/           — ScreenManager (push/pop navigation)
  util/          — Symbols (box-drawing chars), StringUtils, TerminalTextUtils
  demo/          — 8 demo applications
```

## License

Apache 2.0