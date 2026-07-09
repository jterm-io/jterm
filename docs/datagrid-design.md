# DataGrid Widget Design

## Problem

The existing `Table` widget works for simple string-based tables but has limitations:

- **No column alignment control** — everything is left-aligned, so numeric columns look ragged
- **No per-column formatting** — callers manually format numbers as strings before adding rows
- **No per-cell styling** — no way to color-code cells (e.g., negative values in red, high opportunity scores in green)
- **No column types** — everything is `String`, so sorting and formatting must be done manually
- **No horizontal scrolling** — columns are squeezed to fit the viewport width

## Goal

A general-purpose `DataGrid<T>` widget that:

1. Displays tabular data with a **frozen header row** that stays on screen while scrolling
2. Supports **per-column alignment** (LEFT, CENTER, RIGHT) with automatic numeric right-alignment
3. Supports **per-column format strings** (e.g. `"$%,.2f"`, `"%.3f"`, `"%+.1f%%"`) applied to all cells in the column
4. Supports **per-cell styling** via a callback (color cells based on value — red for negatives, green for high scores, etc.)
5. Works with **any row type** (records, POJOs, Maps) via column accessors — not just `String[]`
6. Scrolls **both vertically and horizontally** when data exceeds the viewport
7. Supports **column-specific sizing** — auto-size to content, fixed width, or percentage of available width

## Architecture

```
io.jterm.widget.DataGrid<T>              — the widget (extends AbstractComponent)
io.jterm.widget.model.GridColumn<T>      — column definition (header, accessor, format, alignment, width)
io.jterm.widget.model.GridModel<T>       — interface for row data (list-backed default impl)
io.jterm.widget.model.DefaultGridModel<T> — CopyOnWriteArrayList-backed GridModel
io.jterm.widget.model.GridListener       — listener for model changes
io.jterm.style.CellStyle                  — fg/bg color + SGR modifiers for a cell
```

## Key Classes

### `CellStyle` — Per-cell Visual Style (immutable record)

```java
public record CellStyle(Color fg, Color bg, SGR... modifiers) {
    public static final CellStyle DEFAULT = new CellStyle(null, null);  // use theme colors
    public static final CellStyle GREEN = new CellStyle(AnsiColor.BRIGHT_GREEN, null);
    public static final CellStyle RED = new CellStyle(AnsiColor.BRIGHT_RED, null);
    public static final CellStyle BOLD = new CellStyle(null, null, SGR.BOLD);
}
```

Intentionally simple — just fg, bg, and modifiers. `null` fg/bg means "use theme default". Does not conflict with the existing `TextCell` but is lighter weight.

### `GridColumn<T>` — Column Definition (immutable record)

```java
public record GridColumn<T>(
    String header,                        // "Price", "Opp Score", "Ticker"
    Function<T, Object> accessor,          // row -> row.price(), row -> row.ticker()
    Alignment alignment,                  // LEFT, CENTER, RIGHT, AUTO
    String format,                        // printf format string, e.g. "$%,.2f", "%.3f", "%s"
                                          // null = Object.toString()
    int minWidth,                          // minimum width in terminal columns (0 = auto)
    int maxWidth,                          // maximum width (0 = unlimited)
    Function<Object, CellStyle> styler    // null = use default theme colors
) {
    public enum Alignment { LEFT, CENTER, RIGHT, AUTO }

    // Convenience factory methods:
    public static <T> GridColumn<T> text(String header, Function<T, String> accessor) { ... }
    public static <T> GridColumn<T> intCol(String header, Function<T, Integer> accessor) { ... }
    public static <T> GridColumn<T> doubleCol(String header, String format, Function<T, Double> accessor) { ... }
    public static <T> GridColumn<T> column(String header, Function<T, Object> accessor) { ... }

    // Builder-style copy methods:
    public GridColumn<T> withAlignment(Alignment a) { ... }
    public GridColumn<T> withStyler(Function<Object, CellStyle> s) { ... }
    public GridColumn<T> withMinWidth(int w) { ... }
    public GridColumn<T> withMaxWidth(int w) { ... }
}
```

**Alignment.AUTO** (default) inspects the accessor's return type at draw time — `Number` subclasses get RIGHT, everything else gets LEFT. This means numeric columns auto-align without any configuration.

**Format** uses `String.format()` for consistency. If the accessor returns a `Double` and format is `"%,.2f"`, the cell shows `1,234.56`. If the value is null, the cell shows empty string. If format is null, falls back to `Object.toString()`.

**Styler** is optional — a `Function<Object, CellStyle>` that receives the cell's raw value and returns colors/modifiers. Example:

```java
GridColumn.<Row>doubleCol("Deviation", "%+.2fσ", r -> r.deviationSigma())
    .withStyler(v -> ((Double) v) < -1.5 ? CellStyle.GREEN : CellStyle.DEFAULT)
```

### `GridModel<T>` — Row Data Interface

```java
public interface GridModel<T> {
    int getRowCount();
    T getRow(int index);
    void addGridListener(GridListener listener);
    void removeGridListener(GridListener listener);
}
```

**`DefaultGridModel<T>`** — a simple `CopyOnWriteArrayList`-backed implementation:

```java
public class DefaultGridModel<T> implements GridModel<T> {
    private final List<T> rows = new CopyOnWriteArrayList<>();

    public void addRow(T row) { ... }
    public void addRows(Collection<T> rows) { ... }
    public void setRows(Collection<T> rows) { ... }  // bulk replace, single fire
    public void clear() { ... }
}
```

This model is **type-safe** — rows are `T`, not `String[]`. The widget accesses cell values via `GridColumn.accessor().apply(row)`.

### `GridListener` — Model Change Listener

```java
@FunctionalInterface
public interface GridListener {
    void gridChanged();
}
```

Simple single-method listener. `DefaultGridModel` fires `gridChanged()` on any mutation (add, remove, clear, setRows). The widget listens and calls `invalidate()` + `ensureVisible()`.

### `DataGrid<T>` — The Widget

```java
public class DataGrid<T> extends AbstractComponent {
    private GridModel<T> model;
    private List<GridColumn<T>> columns;
    private volatile int selectedRow = 0;
    private volatile int scrollOffsetY = 0;
    private volatile int scrollOffsetX = 0;
    private volatile int[] columnWidths;  // computed during draw

    public DataGrid(List<GridColumn<T>> columns) { ... }
    public DataGrid(List<GridColumn<T>> columns, GridModel<T> model) { ... }

    public void setModel(GridModel<T> model) { ... }
    public void setColumns(List<GridColumn<T>> columns) { ... }

    // Selection
    public int getSelectedRow() { ... }
    public void setSelectedRow(int index) { ... }
    public T getSelectedItem() { ... }
    public void addSelectionListener(Runnable listener) { ... }

    // Scrolling
    public void pageDown() { ... }
    public void pageUp() { ... }
}
```

## Rendering

The `drawComponent` method:

1. **Compute column widths** — scan visible rows (not all rows, for performance) and headers to find max content width per column. Respect `minWidth` and `maxWidth`. If total width exceeds viewport, enable horizontal scrolling.

2. **Draw header row** (frozen, always at y=0) — uses `theme.headerFg()`/`theme.headerBg()` with `SGR.BOLD`. Columns are drawn with their alignment, separated by `│` (box-drawing vertical). Header text is truncated if column is too narrow.

3. **Draw data rows** (y=1 onward) — for each visible row (from `scrollOffsetY`):
   - For each column: apply `accessor(row)` → raw value → format with `column.format()` → align within column width
   - Apply styler if present (overrides theme colors), otherwise use theme fg/bg
   - Selected row uses `theme.selectionFg()`/`theme.selectionBg()`
   - Truncate text that exceeds column width

4. **Horizontal scrolling** — if total column width > viewport width, only show columns starting from `scrollOffsetX`. Left/right arrow keys (or Shift+arrows) scroll horizontally.

5. **Clear remaining space** below the last visible row with background color.

## Alignment Logic

```
LEFT:   |Text     |
CENTER: |  Text   |
RIGHT:  |   12.50 |
```

For numbers (Alignment.AUTO detects `Number` return type): right-align by default.
For strings: left-align by default.
Override with explicit `Alignment.RIGHT` / `Alignment.LEFT` / `Alignment.CENTER`.

## Format Application

```java
// column format = "$%,.2f", accessor returns 1234.5
String formatted = String.format("$%,.2f", 1234.5);  // "$1,234.50"

// column format = "%.3f", accessor returns 0.947
String formatted = String.format("%.3f", 0.947);     // "0.947"

// column format = null, accessor returns "AAPL"
String formatted = String.valueOf("AAPL");            // "AAPL"

// value is null
String formatted = "";                                // empty string
```

## Key Bindings

| Key | Action |
|-----|--------|
| ↑ / Ctrl+P | Move selection up |
| ↓ / Ctrl+N | Move selection down |
| PgUp / Ctrl+V | Page up |
| PgDown | Page down |
| Home | First row |
| End | Last row |
| ← / Shift+← | Scroll left (when columns exceed viewport) |
| → / Shift+→ | Scroll right |
| Enter | Fire selection listener |

## Usage Example (Trend Channel Scanner)

```java
var columns = List.of(
    GridColumn.text("Tk", r -> r.ticker()),
    GridColumn.doubleCol("Price", "$%,.2f", r -> r.price()),
    GridColumn.doubleCol("Trend", "$%,.2f", r -> r.trend()),
    GridColumn.doubleCol("vsTr%", "%+.1f%%", r -> ((r.price() - r.trend()) / r.trend()) * 100)
        .withStyler(v -> ((Double) v) < 0 ? CellStyle.RED : CellStyle.GREEN),
    GridColumn.doubleCol("Buy", "$%,.2f", r -> r.buyLine()),
    GridColumn.doubleCol("Devσ", "%+.2fσ", r -> r.deviationSigma())
        .withStyler(v -> ((Double) v) < -1.5 ? CellStyle.GREEN : CellStyle.DEFAULT),
    GridColumn.doubleCol("Bot%", "%+.1f%%", r -> r.pctFromBottom()),
    GridColumn.doubleCol("Opp", "%.2f", r -> r.oppScore())
        .withStyler(v -> ((Double) v) > 0.7 ? CellStyle.GREEN : CellStyle.DEFAULT),
    GridColumn.doubleCol("R²", "%.3f", r -> r.rSquared()),
    GridColumn.doubleCol("CAGR", "%.1f%%", r -> r.cagr())
);

var model = new DefaultGridModel<TrendChannelRow>();
model.setRows(screenerDao.trendChannelScan());

var grid = new DataGrid<>(columns, model);
```

This replaces the current `StyledLine` + `StyledListBox` approach with a cleaner, more structured table that handles alignment and formatting automatically.

## Migration Path

The existing `Table` widget stays as-is (backward compatible). `DataGrid` is a new, more capable widget. The BBS screens that currently use `StyledListBox` with manually-formatted `StyledLine` objects (trend channel, screener, portfolio, etc.) can migrate to `DataGrid` incrementally.

## Implementation Plan

### Milestone 1: CellStyle + GridColumn

- `CellStyle` record with constants (DEFAULT, GREEN, RED, BOLD)
- `GridColumn<T>` record with factory methods (text, intCol, doubleCol, column) and builder-style copy methods (withAlignment, withStyler, withMinWidth, withMaxWidth)
- Tests: factory methods produce correct defaults, AUTO alignment picks RIGHT for numbers, format strings applied correctly, copy methods return new instances

### Milestone 2: GridModel + DefaultGridModel

- `GridListener` functional interface
- `GridModel<T>` interface (getRowCount, getRow, add/removeGridListener)
- `DefaultGridModel<T>` with CopyOnWriteArrayList backing, addRow, addRows, setRows, clear, all firing gridChanged
- Tests: add/remove listeners fire correctly, setRows replaces bulk, clear empties, thread safety (concurrent add + read)

### Milestone 3: DataGrid Widget — Core Rendering

- `DataGrid<T>` extends AbstractComponent
- Column width computation (scan visible rows + headers, respect min/max width)
- Frozen header row rendering with theme colors + SGR.BOLD
- Data row rendering with alignment (LEFT, CENTER, RIGHT, AUTO)
- Format string application per column
- Per-cell styler application
- Selected row highlighting with theme selection colors
- Clear remaining rows with background color
- Tests: rendering with small data set, header is frozen, alignment is correct, format applied, styler overrides colors, empty model renders header only

### Milestone 4: DataGrid Widget — Scrolling + Selection

- Vertical scrolling (arrow keys, PgUp/PgDn, Home/End, Ctrl+P/Ctrl+N/Ctrl+V)
- Horizontal scrolling (Shift+←/→) when columns exceed viewport
- Selection management (setSelectedRow, getSelectedItem, ensureVisible)
- Selection listeners
- Page up/down
- Tests: scrolling moves viewport, selection clamps to valid range, ensureVisible works, horizontal scroll shows different columns, page up/down

### Milestone 5: DataGrid Widget — Thread Safety + Edge Cases

- Concurrent add + draw (matching existing ThreadSafetyTest patterns)
- Empty model (0 rows, 0 columns)
- Single row
- Very wide content (truncation)
- Very narrow viewport (minimum column widths)
- Null values in cells
- Model swap at runtime
- Tests: concurrent operations don't throw CME, edge cases render without exceptions

### Milestone 6: Demo + Integration

- Add DataGrid demo to BasicWidgetsDemo
- Add to widget coverage tests
- Verify full test suite passes

## Future Extensions (Not in Initial Build)

### Column Sorting

Click header (or press a key like `s` then column letter) to sort ascending/descending. Sorting is done via a `Comparator<T>` on the column's accessor value. Visual indicator (▲/▼) in the header. Multi-column sort with Shift+click.

### Column Resizing

Interactive column width adjustment with arrow keys. Press a key (e.g. `r` then column index) to enter resize mode, use ←/→ to adjust width, Enter to confirm. Respected alongside auto-sizing.

### Column Hiding/Showing

Toggle column visibility with a key (e.g. `h` then column index, or a menu). Hidden columns are skipped during rendering but retain their position in the column list for re-showing.

### Row Selection Modes

- **Single** (default) — one row selected at a time
- **Multi** — Shift+↑/↓ extends selection, Ctrl+Space toggles
- **None** — no selection, pure data display
- **Cell selection** — individual cell focus (for copy/paste)

### Cell Editing

For spreadsheet-like applications:
- Double-click or press Enter on a cell to enter edit mode
- Text input with cursor
- Validation via column type (numeric columns reject non-numeric input)
- Commit on Enter, cancel on Escape
- Fire edit event to model for persistence

### Frozen Columns

Keep the first N columns fixed while horizontal scrolling the rest. Configurable via `setFrozenColumns(int n)`. Frozen columns render in a separate pass after the scrollable columns, ensuring they stay visible.

### Row Striping

Alternating row background colors for readability. Configurable via `setRowStriping(boolean)` and uses a subtle theme-aware alternate background color.

### Column Footers

Optional footer row below the data rows for aggregates (sum, average, count). Configurable per column via `GridColumn.withFooter(Function<List<Object>, String> aggregator)`.

### Export

Export visible data as CSV or TSV to clipboard or file. Triggered via a key binding (e.g. `e` for CSV export).

### Filtering

Per-column text filter — type to filter rows matching the query. Filter bar appears below the header when activated (e.g. `/` key). Supports simple substring matching initially, regex later.

### Responsive Column Priority

When viewport is narrow, columns with lower priority are hidden first. Each column has a `priority` (HIGH, MEDIUM, LOW). On wide terminals all columns show; on narrow ones, LOW priority columns drop first, then MEDIUM.

### Data Binding

Auto-refresh from a data source that implements a `DataSupplier<T>` interface with polling or push notifications. Useful for live data (stock prices, system metrics).