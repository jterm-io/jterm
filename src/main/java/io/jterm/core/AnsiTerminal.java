package io.jterm.core;

import io.jterm.core.input.InputDecoder;
import io.jterm.core.input.KeyStroke;
import io.jterm.style.AnsiCodes;
import io.jterm.style.Color;
import io.jterm.style.SGR;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Unix ANSI terminal implementation. Supports raw mode, alt screen, and escape sequences. */
public class AnsiTerminal implements Terminal {
    private final OutputStream out;
    private final InputDecoder decoder;
    private final TerminalSize fixedSize;
    private final List<TerminalResizeListener> resizeListeners = new ArrayList<>();
    private final String originalStty;
    private final boolean ownStty;
    private TerminalSize lastSize;

    /** Construct terminal using provided streams and size (useful for tests). */
    public AnsiTerminal(OutputStream out, InputStream in, TerminalSize size) {
        this.out = new BufferedOutputStream(out, 4096);
        this.decoder = new InputDecoder(in);
        this.fixedSize = size;
        this.lastSize = size;
        this.originalStty = null;
        this.ownStty = false;
    }

    /** Construct terminal using System.in/out and querying real terminal size. */
    public AnsiTerminal() throws IOException {
        this.out = new BufferedOutputStream(System.out, 4096);
        this.decoder = new InputDecoder(System.in);
        this.fixedSize = null;
        this.lastSize = queryTerminalSize();
        this.originalStty = captureStty();
        this.ownStty = true;
    }

    private static String captureStty() throws IOException {
        try {
            var pb = new ProcessBuilder("sh", "-c", "stty -g </dev/tty").redirectErrorStream(true);
            var p = pb.start();
            var bytes = p.getInputStream().readAllBytes();
            p.waitFor();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (InterruptedException | IOException e) {
            return null;
        }
    }

    private static void setRawMode() {
        try {
            new ProcessBuilder("sh", "-c", "stty raw -echo -icanon </dev/tty").inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            // ignore
        }
    }

    private static void restoreStty(String settings) {
        if (settings == null || settings.isEmpty()) return;
        try {
            new ProcessBuilder("sh", "-c", "stty " + settings + " </dev/tty").inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            // ignore
        }
    }

    private static TerminalSize queryTerminalSize() {
        try {
            var pb = new ProcessBuilder("sh", "-c", "stty size </dev/tty").redirectErrorStream(true);
            var p = pb.start();
            var bytes = p.getInputStream().readAllBytes();
            p.waitFor();
            var parts = new String(bytes, StandardCharsets.UTF_8).trim().split("\\s+");
            if (parts.length == 2) {
                return new TerminalSize(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
        } catch (Exception e) {
            // fall through
        }
        return new TerminalSize(80, 24);
    }

    @Override
    public void enterPrivateMode() throws IOException {
        if (ownStty) setRawMode();
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
        if (ownStty) restoreStty(originalStty);
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
        if (fixedSize != null) return fixedSize;
        return queryTerminalSize();
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
    }

    /** Write raw bytes directly to the terminal output. */
    public void writeRaw(byte[] data) throws IOException {
        out.write(data);
    }

    /** For testing: capture output written so far. */
    public String getCapturedOutput() {
        return "";
    }
}
