package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated background that fills the screen with a grid of miniature ASCII
 * candlestick bars. Each candle displays a randomized OHLC value that updates
 * each frame. Bullish candles are green, bearish candles are red. Candles are
 * drawn with block characters for the body and vertical-bar wicks.
 */
public class CandlestickField implements AnimatedBackground {

    private static final int TARGET_FPS = 6;

    // Each candle occupies a 2-column wide cell; the candle body is drawn
    // with left/right half-blocks (\u258c / \u2590) and the wick is a vertical bar.
    private static final int CANDLE_WIDTH = 2;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private Candle[][] candles;

    public CandlestickField(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        ensureCandles(size);

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        int cols = size.columns();
        int rows = size.rows();

        for (int cy = 0; cy < candles.length; cy++) {
            for (int cx = 0; cx < candles[cy].length; cx++) {
                Candle candle = candles[cy][cx];
                candle.update(rows);
                candle.draw(graphics, rows);
            }
        }
    }

    private void ensureCandles(TerminalSize size) {
        int cols = size.columns();
        int rows = size.rows();
        int candleCols = Math.max(1, cols / CANDLE_WIDTH);
        int candleRows = Math.max(1, rows);

        if (candles != null && candles.length == candleRows && candles[0].length == candleCols) {
            return;
        }

        candles = new Candle[candleRows][candleCols];
        for (int y = 0; y < candleRows; y++) {
            for (int x = 0; x < candleCols; x++) {
                candles[y][x] = new Candle(x * CANDLE_WIDTH, y, rows, random);
            }
        }
    }

    @Override
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.columns() <= 0 || newSize.rows() <= 0) {
            return;
        }
        ensureCandles(newSize);
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int targetFps() {
        return TARGET_FPS;
    }

    @Override
    public TerminalSize lastSize() {
        return lastSize;
    }

    /** Visible for tests: returns the current candle grid. */
    public Candle[][] getCandles() {
        return candles;
    }

    /** Visible for tests: returns the number of candles in each row. */
    public int getCandleRowCount() {
        return candles == null ? 0 : candles[0].length;
    }

    /** A single ASCII candlestick with randomized OHLC data. */
    public static final class Candle {
        private static final double RANDOM_SCALE = 0.15;
        private static final double REVERSION = 0.05;
        private static final char LEFT_BLOCK = '\u258c';
        private static final char RIGHT_BLOCK = '\u2590';
        private static final char FULL_BLOCK = '\u2588';
        private static final char WICK = '\u2503';

        private final int screenX;
        private final int screenY;
        private final int totalRows;
        private final Random random;

        private double open;
        private double high;
        private double low;
        private double close;
        private boolean bullish;

        Candle(int screenX, int screenY, int totalRows, Random random) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.totalRows = totalRows;
            this.random = random;
            this.open = random.nextDouble();
            this.close = open + (random.nextDouble() - 0.5) * RANDOM_SCALE;
            computeHighLow();
        }

        void update(int rows) {
            // Random walk with mean reversion. Price is normalized to [0, 1]
            // and mapped to terminal rows when drawn.
            double change = (random.nextDouble() - 0.5) * RANDOM_SCALE;
            double meanReversion = (0.5 - close) * REVERSION;
            open = close;
            close = clamp(close + change + meanReversion, 0.05, 0.95);
            computeHighLow();
        }

        private void computeHighLow() {
            double range = Math.max(Math.abs(close - open), 0.02) * (1.0 + random.nextDouble());
            high = Math.max(open, close) + range * 0.5;
            low = Math.min(open, close) - range * 0.5;
            bullish = close >= open;
        }

        void draw(TextGraphics graphics, int rows) {
            AnsiColor fg = bullish ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;
            AnsiColor bg = AnsiColor.BLACK;
            TextCell bodyLeft = new TextCell(LEFT_BLOCK, fg, bg, SGR.BOLD);
            TextCell bodyRight = new TextCell(RIGHT_BLOCK, fg, bg, SGR.BOLD);
            TextCell bodyFull = new TextCell(FULL_BLOCK, fg, bg, SGR.BOLD);
            TextCell wick = new TextCell(WICK, fg, bg, SGR.BOLD);

            int top = priceToRow(high, rows);
            int bottom = priceToRow(low, rows);
            int bodyTop = priceToRow(Math.max(open, close), rows);
            int bodyBottom = priceToRow(Math.min(open, close), rows);

            // Draw wick from high to low.
            for (int y = top; y <= bottom; y++) {
                if (y >= 0 && y < rows) {
                    graphics.setCell(screenX, y, wick);
                }
            }

            // Draw body. If the body is at least one row tall, fill with left/right
            // half-blocks. If the open/close round to the same row, draw a single
            // full-width block character on that row.
            if (bodyTop == bodyBottom) {
                if (bodyTop >= 0 && bodyTop < rows) {
                    graphics.setCell(screenX, bodyTop, bodyFull);
                }
            } else {
                for (int y = bodyTop; y <= bodyBottom; y++) {
                    if (y >= 0 && y < rows) {
                        graphics.setCell(screenX, y, bodyLeft);
                        if (screenX + 1 < graphics.getSize().columns()) {
                            graphics.setCell(screenX + 1, y, bodyRight);
                        }
                    }
                }
            }
        }

        private int priceToRow(double price, int rows) {
            int row = (int) ((1.0 - price) * (rows - 1));
            return clamp(row, 0, rows - 1);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        /** True if this candle closed at or above its open. */
        public boolean isBullish() {
            return bullish;
        }

        /** Normalized close price, roughly [0, 1]. */
        public double getClose() {
            return close;
        }
    }
}
