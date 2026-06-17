package io.jterm.core.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Parses raw terminal input bytes into keyboard events. */
public class InputDecoder {

    private final InputStream input;

    public InputDecoder(InputStream input) {
        this.input = input;
    }

    public Optional<KeyStroke> poll() throws IOException {
        if (input.available() == 0) {
            return Optional.empty();
        }
        int first = input.read();
        if (first == -1) {
            return Optional.of(new KeyStroke(KeyType.EOF));
        }

        if (first == 0x1b) { // ESC
            return readEscapeSequence(first);
        }

        if (first == '\r' || first == '\n') {
            return Optional.of(new KeyStroke(KeyType.ENTER));
        }
        if (first == '\t') {
            return Optional.of(new KeyStroke(KeyType.TAB));
        }
        if (first == 0x7f) {
            return Optional.of(new KeyStroke(KeyType.BACKSPACE));
        }
        if (first >= 1 && first <= 26) { // Ctrl+A..Ctrl+Z, treat as letters
            return Optional.of(KeyStroke.character((char) ('A' + first - 1), true, false, false));
        }

        return Optional.of(KeyStroke.character((char) first, false, false, false));
    }

    private Optional<KeyStroke> readEscapeSequence(int esc) throws IOException {
        if (input.available() == 0) {
            // Wait briefly to distinguish standalone Escape from Alt+key sequences.
            // Over network connections (e.g. iPad via SSH), ESC and the following byte
            // can arrive in separate TCP packets. A 5ms wait gives the next byte
            // time to arrive without noticeable latency for real Escape presses.
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (input.available() == 0) {
                // Standalone Escape
                return Optional.of(new KeyStroke(KeyType.ESCAPE));
            }
        }
        int second = input.read();
        if (second == -1) {
            return Optional.of(new KeyStroke(KeyType.ESCAPE));
        }

        if (second == '[') {
            return readCsiSequence();
        }
        if (second == 'O') {
            return readOSequence();
        }
        if (Character.isLetterOrDigit(second) || second >= 32 && second < 127) {
            return Optional.of(KeyStroke.character((char) second, false, true, false));
        }
        return Optional.of(new KeyStroke(KeyType.UNKNOWN));
    }

    private Optional<KeyStroke> readCsiSequence() throws IOException {
        StringBuilder params = new StringBuilder();
        int ch;
        while ((ch = input.read()) != -1) {
            char c = (char) ch;
            if (c >= '0' && c <= '9' || c == ';' || c == '?') {
                params.append(c);
            } else if (c >= 0x40 && c <= 0x7e) { // final byte
                return Optional.of(mapCsi(params.toString(), c));
            } else {
                return Optional.of(new KeyStroke(KeyType.UNKNOWN));
            }
        }
        return Optional.of(new KeyStroke(KeyType.UNKNOWN));
    }

    private Optional<KeyStroke> readOSequence() throws IOException {
        int ch = input.read();
        if (ch == -1) return Optional.of(new KeyStroke(KeyType.UNKNOWN));
        char c = (char) ch;
        KeyType type = switch (c) {
            case 'P' -> KeyType.F1;
            case 'Q' -> KeyType.F2;
            case 'R' -> KeyType.F3;
            case 'S' -> KeyType.F4;
            default -> KeyType.UNKNOWN;
        };
        return Optional.of(new KeyStroke(type));
    }

    private KeyStroke mapCsi(String params, char finalByte) {
        KeyType type = switch (finalByte) {
            case 'A' -> KeyType.ARROW_UP;
            case 'B' -> KeyType.ARROW_DOWN;
            case 'C' -> KeyType.ARROW_RIGHT;
            case 'D' -> KeyType.ARROW_LEFT;
            case 'H' -> KeyType.HOME;
            case 'F' -> KeyType.END;
            case 'Z' -> KeyType.TAB; // shift-tab is treated as Tab
            case '~' -> mapTilde(params);
            default -> KeyType.UNKNOWN;
        };
        return new KeyStroke(type);
    }

    private KeyType mapTilde(String params) {
        return switch (params) {
            case "1", "7" -> KeyType.HOME;
            case "2" -> KeyType.INSERT;
            case "3" -> KeyType.DELETE;
            case "4", "8" -> KeyType.END;
            case "5" -> KeyType.PAGE_UP;
            case "6" -> KeyType.PAGE_DOWN;
            case "11" -> KeyType.F1;
            case "12" -> KeyType.F2;
            case "13" -> KeyType.F3;
            case "14" -> KeyType.F4;
            case "15" -> KeyType.F5;
            case "17" -> KeyType.F6;
            case "18" -> KeyType.F7;
            case "19" -> KeyType.F8;
            case "20" -> KeyType.F9;
            case "21" -> KeyType.F10;
            case "23" -> KeyType.F11;
            case "24" -> KeyType.F12;
            default -> KeyType.UNKNOWN;
        };
    }
}
