package io.jterm.core;

import io.jterm.core.input.InputDecoder;
import io.jterm.core.input.KeyStroke;
import io.jterm.style.AnsiCodes;
import io.jterm.style.Color;
import io.jterm.style.SGR;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Terminal implementation that wraps a network socket's InputStream and OutputStream.
 * Used for remote terminal sessions (SSH/Telnet) in the BBS project.
 */
public class SocketTerminal implements Terminal {

    private final InputStream in;
    private final OutputStream out;
    private final InputDecoder decoder;
    private final TerminalSize fixedSize;
    private volatile TerminalSize currentSize;
    private final List<TerminalResizeListener> resizeListeners = new CopyOnWriteArrayList<>();

    /**
     * Construct a terminal from the socket's input and output streams, with an explicit size.
     * This is the constructor used for testing and for protocols where the terminal size is
     * negotiated externally (e.g. Telnet NAWS or SSH pty-req).
     */
    public SocketTerminal(InputStream in, OutputStream out, TerminalSize size) {
        this.in = in;
        this.out = new BufferedOutputStream(out, 4096);
        this.decoder = new InputDecoder(in);
        this.fixedSize = size;
        this.currentSize = size;
    }

    /**
     * Construct a terminal from the socket's streams and query the remote terminal for its size.
     * Falls back to 80×24 if discovery is unavailable.
     */
    public SocketTerminal(InputStream in, OutputStream out) throws IOException {
        this(in, out, queryTerminalSize(in, out));
    }

    private static TerminalSize queryTerminalSize(InputStream in, OutputStream out) throws IOException {
        // TODO: implement TIOCGWINSZ-over-ANSI (ESC [ 18 t / ESC [ 8 ; rows ; cols t)
        // For now, return a safe default. Network terminals should prefer the size constructor.
        return new TerminalSize(80, 24);
    }

    @Override
    public void enterPrivateMode() throws IOException {
        writeRaw(AnsiCodes.ENTER_ALT_SCREEN.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.HIDE_CURSOR.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.CLEAR_SCREEN.getBytes(StandardCharsets.UTF_8));
        flush();
    }

    @Override
    public void exitPrivateMode() throws IOException {
        writeRaw(AnsiCodes.SHOW_CURSOR.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.EXIT_ALT_SCREEN.getBytes(StandardCharsets.UTF_8));
        flush();
    }

    @Override
    public void clearScreen() throws IOException {
        writeRaw(AnsiCodes.CLEAR_SCREEN.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void setCursorPosition(int column, int row) throws IOException {
        writeRaw(AnsiCodes.cursorTo(row, column));
    }

    @Override
    public void setCursorVisible(boolean visible) throws IOException {
        writeRaw((visible ? AnsiCodes.SHOW_CURSOR : AnsiCodes.HIDE_CURSOR).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void putCharacter(char c) throws IOException {
        var bytes = Character.toString(c).getBytes(StandardCharsets.UTF_8);
        writeRaw(bytes);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void setForegroundColor(Color color) throws IOException {
        writeRaw(AnsiCodes.setForeground(color));
    }

    @Override
    public void setBackgroundColor(Color color) throws IOException {
        writeRaw(AnsiCodes.setBackground(color));
    }

    @Override
    public void enableSGR(SGR sgr) throws IOException {
        writeRaw(AnsiCodes.enable(sgr));
    }

    @Override
    public void disableSGR(SGR sgr) throws IOException {
        writeRaw(AnsiCodes.disable(sgr));
    }

    @Override
    public void resetColorAndSGR() throws IOException {
        writeRaw(AnsiCodes.reset());
    }

    @Override
    public TerminalSize getTerminalSize() throws IOException {
        return currentSize;
    }

    /** Updates the terminal size (e.g. from Telnet NAWS). Notifies resize listeners. */
    public void setTerminalSize(TerminalSize size) {
        var old = this.currentSize;
        this.currentSize = size;
        if (!size.equals(old)) {
            for (var listener : resizeListeners) {
                listener.onResized(size);
            }
        }
    }

    @Override
    public Optional<KeyStroke> pollInput() throws IOException {
        return decoder.poll();
    }

    @Override
    public KeyStroke readInput() throws IOException {
        while (true) {
            var ks = decoder.poll();
            if (ks.isPresent()) return ks.get();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new KeyStroke(io.jterm.core.input.KeyType.EOF);
            }
        }
    }

    @Override
    public void addResizeListener(TerminalResizeListener listener) {
        resizeListeners.add(listener);
    }

    @Override
    public void removeResizeListener(TerminalResizeListener listener) {
        resizeListeners.remove(listener);
    }

    @Override
    public void close() throws IOException {
        exitPrivateMode();
        in.close();
        out.close();
    }

    /** Write raw bytes directly to the terminal output. Thread-safe with respect to output. */
    public synchronized void writeRaw(byte[] data) throws IOException {
        out.write(data);
    }
}
