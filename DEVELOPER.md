# jterm — Developer Guide

A pure-Java terminal UI toolkit for building text-based user interfaces. No native dependencies, no JNI — just Java and ANSI.

## Requirements

- JDK 26+
- Maven 3.8+

## Build

```bash
mvn compile          # Compile
mvn test             # Run tests (2,187 tests)
mvn install -DskipTests  # Install to local ~/.m2
mvn javadoc:javadoc  # Generate Javadoc HTML (target/reports/apidocs/)
```

## Architecture (3 Layers)

1. **Terminal** (`io.jterm.core`) — Raw ANSI/VT100 escape sequences, stty raw mode, InputDecoder for key events. AnsiTerminal for local console, SocketTerminal for network connections (SSH/Telnet).
2. **Screen** (`io.jterm.screen`) — Double-buffered with diff-based refresh. SgrStateTracker batches SGR transitions so adjacent cells don't redundantly re-emit escape codes. RefreshType.DELTA only redraws changed cells.
3. **GUI** (`io.jterm.widget`, `io.jterm.layout`, `io.jterm.window`) — Component/Container tree, Panel with LayoutManagers, Border factory, widgets, TextGUI event loop with window management and focus traversal.

## Key Design Decisions

- `TextCell` is an immutable record — safe to share between front/back buffers, no defensive copies
- Zero native dependencies — just System.out/System.in + stty on Unix
- MockTerminal for testing — injects escape sequences without a real terminal
- Screen buffers are inspectable — widgets render to off-screen buffers you can assert against in tests

## Running Demos

```bash
mvn install -DskipTests
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.HelloWorld
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.DashboardDemo
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.BordersDemo
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.MenuDemo
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.ChartDemo
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.TextEditorDemo
java -cp target/jterm-0.1.0-SNAPSHOT.jar io.jterm.demo.ThemeDemo
```

- **HelloWorld** — styled label, counter button, quit button
- **DashboardDemo** — ListBox, Table, ProgressBar, BorderLayout, focus navigation, background progress thread
- **BordersDemo** — all 5 border styles (singleLine, doubleLine, rounded, bevel, empty) + titled border
- **MenuDemo** — MenuBar with File/Edit/Help menus, separators, keyboard navigation (Ctrl/Alt+mnemonic)
- **ChartDemo** — interactive stock data viewer: line/bar/scatter/multi-series charts, press 1/2/3/4 to switch views, r to regenerate, q to quit
- **TextEditorDemo** — multi-line text editor with scrolling, emacs bindings, Ctrl+S to save
- **ThemeDemo** — color theme switcher: 4 built-in themes, press t to cycle or 1-4 to select

## Widget System

### Core Widgets

| Widget | Description |
|--------|-------------|
| `Label` | Text display, supports multi-line, centering, theme colors |
| `Button` | Clickable button with click listener |
| `TextBox` | Single-line input with emacs key bindings, password masking, uppercase mode |
| `TextArea` | Multi-line text editor with scrolling and emacs key bindings |
| `CheckBox` | Toggle button |
| `RadioButton` / `RadioGroup` | Mutually exclusive selection |
| `ProgressBar` | Indeterminate and determinate progress display |
| `ListBox` | Scrollable list with auto-scroll-to-bottom support |
| `Table` | Tabular data with TableModel, column headers, row selection |
| `DataGrid` | Grid data with GridModel, column configuration |
| `Panel` | Container with LayoutManager |
| `Border` / `Borders` | Box-drawing borders (single, double, rounded, bevel, empty) |
| `MenuBar` / `Menu` / `MenuItem` | Dropdown menu system with keyboard navigation |
| `Separator` | Horizontal or vertical divider |
| `EmptySpace` | Filler component for layout spacing |
| `Chart` | Terminal-based charts (line, bar, scatter) with multi-series support |

### Layout Managers

- **BorderLayout** — north/south/east/west/center positioning
- **LinearLayout** — horizontal or vertical stacking with alignment (BEGINNING, CENTER, END, FILL)
- **GridLayout** — rows × columns grid

### Data Models

- `ListModel<T>` / `DefaultListModel<T>` — list data with change events
- `TableModel` / `DefaultTableModel` — table data with row/column/structure change events
- `GridModel` / `DefaultGridModel` — grid data with change events
- `ResultSetGridModel` — bind JDBC ResultSet directly to a grid

### Theme System

`Theme` is a record with 12 semantic colors (foreground, background, selection, focus, border, title, accent, header). `ThemeManager` holds the active theme with runtime switching and listener notification.

4 built-in themes: `DARK` (default), `YELLOW_ON_BLUE`, `GREEN_ON_BLACK`, `WHITE_ON_GREEN`.

All widgets query `ThemeManager.active()` at draw time. Switch themes at runtime with `ThemeManager.setActive(theme)` or `ThemeManager.cycle()`.

### Animation System

25+ animated backgrounds and transition effects in `io.jterm.animation`:

- MatrixRain, Starfield, TwinkleStarfield, Fireworks, Snowfall
- LavaLamp, Aurora, OceanWaves, PlasmaWash
- CircuitBoard, PacketFlow, DNAHelix, LightningStorm
- MandelbrotZoom, Spirograph, VoronoiCells, MoirePatterns
- TickerTape, MarketDepth, PortfolioHeatmap, CandlestickField
- TerrainFlyover, Scanlines, PhosphorDecay, RainStorm, DvdLogo
- FadeTransition, WipeTransition, DissolveTransition, SlideTransition

Use `AnimationManager` to manage frame timing and lifecycle. Animated backgrounds render behind normal content via `AnimatedBackgroundWindow`.

### Sprite System

- `Sprite` — frame-based animation with loop modes
- `SpriteSheet` — build sprites from ANSI art or text grids
- `AnimatedText` — typewriter text effect
- `ParticleEffect` — particle burst effects

## Emacs Key Bindings (TextBox and TextArea)

| Key | TextBox | TextArea |
|-----|---------|----------|
| Ctrl+A | Beginning of line | Beginning of line |
| Ctrl+E | End of line | End of line |
| Ctrl+K | Kill to end of line | Kill to end of line |
| Ctrl+F | Forward (→) | Forward (→) |
| Ctrl+B | Backward (←) | Backward (←) |
| Ctrl+P | Up (↑) | Up (↑) |
| Ctrl+N | Down (↓) | Down (↓) |
| Ctrl+D | Delete char at cursor | Delete forward (joins lines at EOL) |

## Quick Start

```java
import io.jterm.window.TextGUI;
import io.jterm.widget.*;

public class Hello {
    public static void main(String[] args) {
        var app = new TextGUI();
        var panel = new Panel(new LinearLayout(LinearLayout.VERTICAL));
        panel.setBorder(Borders.singleLine("jterm"));
        panel.add(new Label("Hello, Terminal!"));
        panel.add(new Button("Quit", app::close));
        app.setRoot(panel);
        app.run();
    }
}
```

## Chart Example

```java
var chart = new Chart("AAPL — 30 Day Price");
chart.addSeries(new ChartSeries("Close", prices, ChartType.LINE, AnsiColor.BRIGHT_GREEN));
chart.addSeries(new ChartSeries("MA", movingAvg, ChartType.LINE, AnsiColor.BRIGHT_MAGENTA));
chart.setYAxisConfig(ChartAxisConfig.fixed(140, 160, "$%.0f"));
panel.add(chart, BorderLayout.CENTER);
```

Chart features: sub-cell half-block rendering (2x vertical resolution), auto-scaling or fixed Y-axis, grid lines, multi-series overlay, edge case handling (empty series, single point, flat line, negative values).

## Window Management

- `TextGUI` — event loop, input processing, screen refresh
- `Window` / `AbstractWindow` — window with title bar, borders, content panel
- `WindowManager` — manages window stacking and focus traversal
- `ScreenManager` — push/pop screen navigation stack for multi-screen apps
- `Dialog` / `YesNoDialog` — modal dialogs

## Key Pitfalls (For AI Agents and Developers)

1. **Always call `gui.requestRefresh()` before `gui.updateScreen()`** if handling keys directly (not via `gui.processInput()`). Otherwise `needsRefresh` stays false and the screen never repaints.

2. **stty raw mode**: AnsiTerminal saves/restores terminal state in a `finally` block. If the process crashes, run `stty sane` or `reset` to restore your terminal.

3. **Double-width characters**: `TerminalTextUtils.getColumnWidth()` must be called for all CJK/fullwidth chars. Forgetting this causes misaligned text rendering.

4. **TextBox cursor only renders when focused**: Wrap cursor rendering in `if (isFocused())`. Tests that check cursor position must call `box.setFocused(true)` before drawing.

5. **Button must query theme at draw time, not cache in constructor**: Query `ThemeManager.active()` in `drawComponent()`, not in the constructor. Cached theme colors don't update when themes change at runtime.

6. **Event loop must sleep when idle**: A tight `while(running) { processInput(); updateScreen(); }` loop spins at 100% CPU. When `processInput()` returns false (no input), `Thread.sleep(16)` before the next iteration.

7. **All widgets must use theme colors, not AnsiColor.DEFAULT**: No widget should use `AnsiColor.DEFAULT` for backgrounds — always use the theme. The only exception is `Label`'s default constructor, which falls back to theme colors when DEFAULT is detected.

8. **Never use RgbColor in terminal output**: BBS terminals only support 16-color ANSI. Use `AnsiColor.blendAnsi()` for color interpolation.

9. **SgrStateTracker must be persistent across delta refreshes**: Creating a fresh tracker per delta refresh causes "stuck background" bugs. The tracker must know the terminal's actual current SGR state.

10. **JAR filename**: The built JAR is `jterm-0.1.0-SNAPSHOT.jar`. Always check `ls target/*.jar` before referencing the path.

## Testing

jterm has 2,187 tests. Key testing patterns:

- **MockTerminal** — injects escape sequences without a real terminal
- **ScreenBuffer assertions** — draw widgets to a ScreenBuffer and assert on cell contents
- **Cursor position helper** — draws TextBox to ScreenBuffer, finds cursor by looking for swapped fg/bg cell
- **Theme compliance tests** — verify every cell type uses theme bg colors

```java
// Example: test that a Button renders its label
var terminal = new MockTerminal(20, 5);
var screen = new DefaultScreen(terminal);
var gui = new DefaultTextGUI(screen);
var button = new Button("Click Me");
button.setBounds(0, 0, 10, 3);
button.draw(gui.getScreen().newTextGraphics());
// Assert on screen buffer contents
```

## License

Apache License 2.0. See [LICENSE](LICENSE) file for details.