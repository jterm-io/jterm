package io.jterm.screen;

import io.jterm.core.Terminal;
import io.jterm.core.TerminalPosition;
import io.jterm.core.TerminalSize;
import io.jterm.style.AnsiColor;
import io.jterm.style.AnsiCodes;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;
import io.jterm.style.Color;
import io.jterm.style.ThemeManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

/** Double-buffered screen with delta refresh and SGR state batching. */
public class DefaultScreen implements Screen {
    private final Terminal terminal;
    private volatile ScreenBuffer backBuffer;
    private volatile ScreenBuffer frontBuffer;
    private volatile TerminalSize size;
    private volatile TerminalPosition cursorPosition = TerminalPosition.TOP_LEFT;
    private volatile boolean started;
    /** Persistent SGR state tracker — survives across refresh calls so
     *  delta refreshes know the terminal's actual current SGR state. */
    private final SgrStateTracker sgrState = new SgrStateTracker();

    public Terminal getTerminal() { return terminal; }

    public DefaultScreen(TerminalSize size) {
        this(new io.jterm.core.MockTerminal(size));
    }

    public DefaultScreen(Terminal terminal) {
        this.terminal = terminal;
        try {
            this.size = terminal.getTerminalSize();
        } catch (IOException e) {
            this.size = new TerminalSize(80, 24);
        }
        this.backBuffer = new ScreenBuffer(this.size);
        this.frontBuffer = new ScreenBuffer(this.size);
    }

    @Override
    public void startScreen() throws IOException {
        terminal.enterPrivateMode();
        terminal.setCursorVisible(false);
        started = true;
    }

    @Override
    public void stopScreen() throws IOException {
        started = false;
        terminal.setCursorVisible(true);
        terminal.exitPrivateMode();
    }

    @Override
    public void close() throws IOException {
        stopScreen();
        terminal.close();
    }

    @Override
    public void clear() {
        var theme = ThemeManager.active();
        backBuffer.fill(new TextCell(' ', theme.foreground(), theme.background()));
    }

    @Override
    public void setCell(int col, int row, TextCell cell) {
        backBuffer.setCell(col, row, cell);
    }

    @Override
    public void setCell(TerminalPosition pos, TextCell cell) {
        backBuffer.setCell(pos, cell);
    }

    @Override
    public TextCell getFrontCell(int col, int row) {
        return frontBuffer.getCell(col, row);
    }

    @Override
    public TextCell getBackCell(int col, int row) {
        return backBuffer.getCell(col, row);
    }

    @Override
    public void refresh() throws IOException {
        refresh(RefreshType.AUTOMATIC);
    }

    @Override
    public void refresh(RefreshType type) throws IOException {
        if (!started) {
            // If not started (e.g., tests), still perform a logical refresh but don't write to terminal.
            frontBuffer.copyFrom(backBuffer);
            return;
        }
        if (type == RefreshType.COMPLETE) {
            doCompleteRefresh();
        } else if (type == RefreshType.DELTA) {
            doDeltaRefresh();
        } else {
            var diffs = backBuffer.diffFrom(frontBuffer);
            if (diffs.size() > (frontBuffer.size().area() / 2)) {
                doCompleteRefresh();
            } else {
                doDeltaRefresh();
            }
        }
        frontBuffer.copyFrom(backBuffer);
    }

    private void doCompleteRefresh() throws IOException {
        terminal.setCursorPosition(0, 0);
        // Write the SGR reset to the terminal — don't discard it.
        // Without this, the terminal keeps its stale SGR state (e.g. white
        // background from a selection highlight) and subsequent cells render
        // with the wrong colors. This is the "white left behind" bug.
        byte[] resetBytes = sgrState.reset();
        if (resetBytes.length > 0) terminal.writeRaw(resetBytes);
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                var cell = backBuffer.getCell(c, r);
                byte[] sgrBytes = sgrState.transitionTo(cell);
                if (sgrBytes.length > 0) terminal.writeRaw(sgrBytes);
                terminal.putCharacter(cell.character().charAt(0));
            }
        }
        terminal.flush();
    }

    private void doDeltaRefresh() throws IOException {
        var diffs = backBuffer.diffFrom(frontBuffer);
        if (diffs.isEmpty()) return;

        int lastRow = -1;
        int lastCol = -1;

        for (var diff : diffs) {
            if (diff.row() != lastRow || diff.column() != lastCol + 1) {
                terminal.setCursorPosition(diff.column(), diff.row());
            }
            byte[] sgrBytes = sgrState.transitionTo(diff.cell());
            if (sgrBytes.length > 0) terminal.writeRaw(sgrBytes);
            terminal.putCharacter(diff.cell().character().charAt(0));
            lastRow = diff.row();
            lastCol = diff.column();
            if (diff.cell().isDoubleWidth()) lastCol++;
        }
        terminal.flush();
    }

    @Override
    public TerminalSize getTerminalSize() {
        return size;
    }

    @Override
    public TerminalSize doResizeIfNecessary() throws IOException {
        var newSize = terminal.getTerminalSize();
        if (!newSize.equals(size)) {
            this.size = newSize;
            this.backBuffer = new ScreenBuffer(size);
            this.frontBuffer = new ScreenBuffer(size);
        }
        return size;
    }

    @Override
    public void setCursorPosition(TerminalPosition position) {
        this.cursorPosition = position;
    }

    @Override
    public TerminalPosition getCursorPosition() {
        return cursorPosition;
    }

    /** Tracks current terminal SGR state and emits only delta transitions. */
    static class SgrStateTracker {
        private Color currentFg = AnsiColor.DEFAULT;
        private Color currentBg = AnsiColor.DEFAULT;
        private final EnumSet<SGR> currentMods = EnumSet.noneOf(SGR.class);

        byte[] transitionTo(TextCell target) {
            // Optimization + correctness: when target is fully default (no fg/bg/modifiers),
            // emit a single SGR reset rather than individual sequences. This avoids the
            // "stuck background" bug where some terminals don't properly interpret
            // \033[49m (default bg) when preceded by other partial resets.
            boolean targetIsDefault = target.fg() == AnsiColor.DEFAULT
                    && target.bg() == AnsiColor.DEFAULT
                    && target.modifiers().isEmpty();
            if (targetIsDefault && (!currentFg.equals(AnsiColor.DEFAULT)
                    || !currentBg.equals(AnsiColor.DEFAULT)
                    || !currentMods.isEmpty())) {
                byte[] reset = AnsiCodes.reset();
                currentFg = AnsiColor.DEFAULT;
                currentBg = AnsiColor.DEFAULT;
                currentMods.clear();
                return reset;
            }

            var sb = new StringBuilder();
            boolean changed = false;

            if (!target.fg().equals(currentFg)) {
                sb.append(AnsiCodes.CSI).append(new String(target.fg().fgSequence(), StandardCharsets.UTF_8)).append("m");
                currentFg = target.fg();
                changed = true;
            }
            if (!target.bg().equals(currentBg)) {
                sb.append(AnsiCodes.CSI).append(new String(target.bg().bgSequence(), StandardCharsets.UTF_8)).append("m");
                currentBg = target.bg();
                changed = true;
            }
            for (SGR mod : target.modifiers()) {
                if (!currentMods.contains(mod)) {
                    sb.append(new String(AnsiCodes.enable(mod), StandardCharsets.UTF_8));
                    currentMods.add(mod);
                    changed = true;
                }
            }
            var it = currentMods.iterator();
            while (it.hasNext()) {
                SGR mod = it.next();
                if (!target.modifiers().contains(mod)) {
                    sb.append(new String(AnsiCodes.disable(mod), StandardCharsets.UTF_8));
                    it.remove();
                    changed = true;
                }
            }

            return changed ? sb.toString().getBytes(StandardCharsets.UTF_8) : new byte[0];
        }

        byte[] reset() {
            currentFg = AnsiColor.DEFAULT;
            currentBg = AnsiColor.DEFAULT;
            currentMods.clear();
            return AnsiCodes.reset();
        }
    }
}
