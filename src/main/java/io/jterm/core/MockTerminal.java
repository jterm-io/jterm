package io.jterm.core;

import io.jterm.core.input.KeyStroke;
import io.jterm.style.Color;
import io.jterm.style.SGR;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Terminal implementation backed by byte arrays, suitable for tests and non-tty environments. */
public class MockTerminal implements Terminal {
    private final OutputStream out;
    private final InputStream in;
    private final TerminalSize size;
    private final List<TerminalResizeListener> listeners = new ArrayList<>();
    private final StringBuilder output = new StringBuilder();
    private boolean closed;
    private io.jterm.core.input.InputDecoder decoder;

    public MockTerminal(TerminalSize size) {
        this(size, new java.io.ByteArrayOutputStream(), new java.io.ByteArrayInputStream(new byte[0]));
    }

    public MockTerminal(TerminalSize size, OutputStream out, InputStream in) {
        this.size = size;
        this.out = out;
        this.in = in;
        this.decoder = new io.jterm.core.input.InputDecoder(in);
    }

    @Override
    public void enterPrivateMode() throws IOException {}

    @Override
    public void exitPrivateMode() throws IOException {}

    @Override
    public void clearScreen() throws IOException {
        output.append("\033[2J");
    }

    @Override
    public void setCursorPosition(int column, int row) throws IOException {
        output.append(String.format("\033[%d;%dH", row + 1, column + 1));
    }

    @Override
    public void setCursorVisible(boolean visible) throws IOException {}

    @Override
    public void putCharacter(char c) throws IOException {
        output.append(c);
        out.write(c);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void setForegroundColor(Color color) throws IOException {
        output.append("\033[").append(new String(color.fgSequence(), java.nio.charset.StandardCharsets.UTF_8)).append("m");
    }

    @Override
    public void setBackgroundColor(Color color) throws IOException {
        output.append("\033[").append(new String(color.bgSequence(), java.nio.charset.StandardCharsets.UTF_8)).append("m");
    }

    @Override
    public void enableSGR(SGR sgr) throws IOException {
        output.append("\033[").append(sgr.code).append("m");
    }

    @Override
    public void disableSGR(SGR sgr) throws IOException {
        output.append("\033[").append(switch (sgr) {
            case BOLD, DIM -> 22;
            case ITALIC -> 23;
            case UNDERLINE -> 24;
            case BLINK -> 25;
            case REVERSE -> 27;
            case HIDDEN -> 28;
            case STRIKETHROUGH -> 29;
        }).append("m");
    }

    @Override
    public void resetColorAndSGR() throws IOException {
        output.append("\033[0m");
    }

    @Override
    public TerminalSize getTerminalSize() throws IOException {
        return size;
    }

    @Override
    public Optional<KeyStroke> pollInput() throws IOException {
        return decoder != null ? decoder.poll() : Optional.empty();
    }

    @Override
    public KeyStroke readInput() throws IOException {
        if (decoder == null) return new KeyStroke(io.jterm.core.input.KeyType.EOF);
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

    // MockTerminal uses the default pollInput(timeout) from the interface (poll+sleep).

    @Override
    public void addResizeListener(TerminalResizeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeResizeListener(TerminalResizeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    public String getOutput() {
        return output.toString();
    }

    public void clearOutput() {
        output.setLength(0);
    }

    public boolean isClosed() {
        return closed;
    }
}
