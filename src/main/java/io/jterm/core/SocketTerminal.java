package io.jterm.core;

import io.jterm.core.input.InputDecoder;
import io.jterm.core.input.KeyStroke;
import io.jterm.style.AnsiCodes;
import io.jterm.style.Color;
import io.jterm.style.Cp437;
import io.jterm.style.SGR;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
    private volatile boolean cp437Mode = false;

    // --- Blocking input pipeline ---
    // A dedicated reader thread blocks on the InputStream, decodes keystrokes
    // via InputDecoder, and pushes them into this queue. This lets pollInput(timeout)
    // use BlockingQueue.poll() for true blocking — sub-millisecond input latency
    // instead of the old 16ms sleep loop.
    private final LinkedBlockingQueue<KeyStroke> inputQueue = new LinkedBlockingQueue<>(256);
    private Thread readerThread;
    private volatile boolean inputClosed = false;
    private volatile boolean inPrivateMode = false;

    /**
     * Construct a terminal from the socket's input and output streams, with an explicit size.
     * This is the constructor used for testing and for protocols where the terminal size is
     * negotiated externally (e.g. Telnet NAWS or SSH pty-req).
     */
    public SocketTerminal(InputStream in, OutputStream out, TerminalSize size) {
        this.in = in;
        this.out = new BufferedOutputStream(out, 65536);  // large enough for 200×100 UTF-8 full render
        this.decoder = new InputDecoder(in);
        this.fixedSize = size;
        this.currentSize = size;
        startReaderThread();
    }

    /**
     * Construct a terminal from the socket's streams and query the remote terminal for its size.
     * Falls back to 80×24 if discovery is unavailable.
     */
    public SocketTerminal(InputStream in, OutputStream out) throws IOException {
        this(in, out, queryTerminalSize(in, out));
    }

    private static TerminalSize queryTerminalSize(InputStream in, OutputStream out) throws IOException {
        // 1. Try ANSI ESC[18t query
        var size = TerminalSizeQuery.query(in, out, 500);
        if (size != null) return size;

        // 2. Final fallback — no stty on network connections
        return new TerminalSize(80, 24);
    }

    /**
     * Switch to the alternate screen buffer and enable raw mode.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void enterPrivateMode() throws IOException {
        writeRaw(AnsiCodes.ENTER_ALT_SCREEN.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.HIDE_CURSOR.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.CLEAR_SCREEN.getBytes(StandardCharsets.UTF_8));
        flush();
        inPrivateMode = true;
    }

    /**
     * Leave the alternate screen buffer and restore original terminal settings.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void exitPrivateMode() throws IOException {
        if (!inPrivateMode) return;
        writeRaw(AnsiCodes.SHOW_CURSOR.getBytes(StandardCharsets.UTF_8));
        writeRaw(AnsiCodes.EXIT_ALT_SCREEN.getBytes(StandardCharsets.UTF_8));
        flush();
        inPrivateMode = false;
    }

    /**
     * Clear the terminal screen.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void clearScreen() throws IOException {
        writeRaw(AnsiCodes.CLEAR_SCREEN.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Move the cursor to the specified column and row.
     *
     * @param column the column index (0-based)
     * @param row the row index (0-based)
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void setCursorPosition(int column, int row) throws IOException {
        writeRaw(AnsiCodes.cursorTo(row, column));
    }

    /**
     * Show or hide the terminal cursor.
     *
     * @param visible true to show, false to hide
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void setCursorVisible(boolean visible) throws IOException {
        writeRaw((visible ? AnsiCodes.SHOW_CURSOR : AnsiCodes.HIDE_CURSOR).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write a single character to the terminal.
     *
     * @param c the character to write
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void putCharacter(char c) throws IOException {
        if (cp437Mode) {
            byte b = Cp437.toCp437(c);
            if (b != -1) {
                writeRaw(new byte[]{b});
            } else {
                // No CP437 mapping — write space as fallback
                writeRaw(new byte[]{0x20});
            }
        } else {
            var bytes = Character.toString(c).getBytes(StandardCharsets.UTF_8);
            writeRaw(bytes);
        }
    }

    /**
     * Flush any buffered output to the underlying stream.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Set the terminal foreground color.
     *
     * @param color the color to apply
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void setForegroundColor(Color color) throws IOException {
        writeRaw(AnsiCodes.setForeground(color));
    }

    /**
     * Set the terminal background color.
     *
     * @param color the color to apply
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void setBackgroundColor(Color color) throws IOException {
        writeRaw(AnsiCodes.setBackground(color));
    }

    /**
     * Enable a Select Graphic Rendition attribute.
     *
     * @param sgr the SGR attribute
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void enableSGR(SGR sgr) throws IOException {
        writeRaw(AnsiCodes.enable(sgr));
    }

    /**
     * Disable a Select Graphic Rendition attribute.
     *
     * @param sgr the SGR attribute
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void disableSGR(SGR sgr) throws IOException {
        writeRaw(AnsiCodes.disable(sgr));
    }

    /**
     * Reset all colors and SGR attributes to defaults.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void resetColorAndSGR() throws IOException {
        writeRaw(AnsiCodes.reset());
    }

    /**
     * Return the current terminal size (columns x rows).
     *
     * @return the terminalsize
     *
     * @throws IOException if an I/O error or other failure occurs
     */
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

    /**
     * Enable or disable CP437 output mode. When enabled, characters are
     * translated from Unicode to CP437 single-byte encoding on output.
     * Enable this for BBS clients that expect CP437 (e.g. MuffinTerm).
     *
     * @param cp437Mode true to enable CP437 translation
     */
    public void setCp437Mode(boolean cp437Mode) {
        this.cp437Mode = cp437Mode;
    }

    /**
     * Return whether CP437 output translation is enabled.
     *
     * @return true if cp437mode, false otherwise
     */
    public boolean isCp437Mode() {
        return cp437Mode;
    }

    /**
     * Poll for the next key stroke without blocking indefinitely.
     *
     * @return an Optional containing the key stroke, or empty if none available
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public Optional<KeyStroke> pollInput() throws IOException {
        if (inputClosed && inputQueue.isEmpty()) return Optional.empty();
        return Optional.ofNullable(inputQueue.poll());
    }

    /**
     * Block until the next key stroke is available.
     *
     * @return the next key stroke
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public KeyStroke readInput() throws IOException {
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new KeyStroke(io.jterm.core.input.KeyType.EOF);
        }
    }

    /**
     * Poll for the next key stroke without blocking indefinitely.
     *
     * @param timeoutMillis maximum time to wait in milliseconds
     *
     * @return an Optional containing the key stroke, or empty if none available
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public Optional<KeyStroke> pollInput(long timeoutMillis) throws IOException {
        if (timeoutMillis <= 0) return pollInput();
        try {
            var ks = inputQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            return Optional.ofNullable(ks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Background reader thread: blocks on the InputStream, decodes keystrokes
     * via InputDecoder, and pushes them into the inputQueue. This decouples
     * the event loop from InputStream blocking — the event loop can use
     * BlockingQueue.poll(timeout) for near-instant input delivery.
     */
    private void startReaderThread() {
        readerThread = Thread.ofVirtual().name("socket-input-reader").start(() -> {
            try {
                while (!inputClosed && !Thread.currentThread().isInterrupted()) {
                    var ks = decoder.poll();
                    if (ks.isPresent()) {
                        inputQueue.put(ks.get());
                    } else {
                        // No data available — check if the stream is still open
                        // by probing available(). If the stream is exhausted, exit.
                        if (in.available() == 0) {
                            // Brief sleep to avoid busy-spin when no data.
                            // The decoder.poll() already checks available(),
                            // so this is only hit when the stream is idle.
                            Thread.sleep(1);
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Normal shutdown
            } catch (IOException e) {
                // Stream closed or error — push EOF so readers can detect it
                inputQueue.offer(new KeyStroke(io.jterm.core.input.KeyType.EOF));
            }
            inputClosed = true;
        });
    }

    /**
     * Register a listener for terminal resize events.
     *
     * @param listener the listener to register
     */
    @Override
    public void addResizeListener(TerminalResizeListener listener) {
        resizeListeners.add(listener);
    }

    /**
     * Remove a previously registered resize listener.
     *
     * @param listener the listener to register
     */
    @Override
    public void removeResizeListener(TerminalResizeListener listener) {
        resizeListeners.remove(listener);
    }

    /**
     * Close the terminal and release resources.
     *
     * @throws IOException if an I/O error or other failure occurs
     */
    @Override
    public void close() throws IOException {
        inputClosed = true;
        if (readerThread != null) readerThread.interrupt();
        exitPrivateMode();
        in.close();
        out.close();
    }

    /** Write raw bytes directly to the terminal output. Thread-safe with respect to output. */
    public synchronized void writeRaw(byte[] data) throws IOException {
        out.write(data);
    }
}
