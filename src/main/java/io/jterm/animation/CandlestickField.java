package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated background that renders candlestick price charts as horizontal
 * bands across the screen. Each band is an independent price series where
 * every candle's open equals the previous candle's close, producing a
 * continuous price walk. Candles use full-block bodies with thin wicks;
 * bullish candles are green, bearish are red. Dim separators divide bands.
 */
public class CandlestickField implements AnimatedBackground {

    private static final int TARGET_FPS = 2;
    private static final int CANDLE_SPACING = 3;  // body col + 2 gap cols
    private static final int MIN_BAND_ROWS = 5;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;
    private ChartBand[] bands;

    /**
     * Constructs a new CandlestickField instance.
     * @param preferredSize the preferred size
     */
    public CandlestickField(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    /**
     * Renders one frame of the animation into the given graphics buffer.
     * @param graphics the graphics
     * @param size the size
     */
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        ensureBands(size);

        // Black background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        for (int i = 0; i < bands.length; i++) {
            ChartBand band = bands[i];
            band.update();
            band.draw(graphics, size.columns());

            // Dim separator line between bands (not after the last one).
            if (i < bands.length - 1) {
                int sepRow = band.startRow + band.height;
                if (sepRow < size.rows()) {
                    TextCell sep = new TextCell('─', AnsiColor.BRIGHT_BLACK, AnsiColor.BLACK);
                    for (int c = 0; c < size.columns(); c++) {
                        graphics.setCell(c, sepRow, sep);
                    }
                }
            }
        }
    }

    private void ensureBands(TerminalSize size) {
        int cols = size.columns();
        int rows = size.rows();
        int candleCount = Math.max(1, cols / CANDLE_SPACING);

        // Determine number of bands: aim for ~8 rows per band, minimum 5.
        int bandHeight = Math.max(MIN_BAND_ROWS, rows / 4);
        int usableRows = rows;  // separators take 1 row each between bands
        int numBands = Math.max(1, (usableRows + 1) / (bandHeight + 1));
        // Recalculate height to evenly divide.
        int totalSeparators = numBands - 1;
        int actualBandHeight = Math.max(MIN_BAND_ROWS, (usableRows - totalSeparators) / numBands);

        if (bands != null && bands.length == numBands && bands[0].candles.length == candleCount
                && bands[0].height == actualBandHeight) {
            return;
        }

        bands = new ChartBand[numBands];
        int currentRow = 0;
        for (int i = 0; i < numBands; i++) {
            int height = actualBandHeight;
            // Last band absorbs any remainder.
            if (i == numBands - 1) {
                int used = currentRow + height + totalSeparators;
                if (used < rows) {
                    height += rows - used;
                }
            }
            bands[i] = new ChartBand(currentRow, height, candleCount, random);
            currentRow += height + 1;  // +1 for separator
        }
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.columns() > 0 && newSize.rows() > 0) {
            ensureBands(newSize);
        }
    }

    @Override
    /** Starts the animation. */
    public void start() { running = true; }

    @Override
    public void stop() { running = false; }

    @Override
    /** Returns whether the animation is running.
     * @return true if running */
    public boolean isRunning() { return running; }

    @Override
    /** Returns the target frame rate.
 * @return the target FPS */
    public int targetFps() { return TARGET_FPS; }

    @Override
    public TerminalSize lastSize() { return lastSize; }

    /** Visible for tests: returns the current band array.
 * @return the current band array */
    public ChartBand[] getBands() { return bands; }

    /** Visible for tests: returns the number of bands.
 * @return the number of bands */
    public int getBandCount() { return bands == null ? 0 : bands.length; }

    /**
     * One horizontal chart strip with its own continuous price series.
     */
    public static final class ChartBand {
        private static final double DRIFT = 0.08;
        private static final double VOLATILITY = 0.12;
        private static final char FULL_BLOCK = '\u2588';
        private static final char WICK = '\u2502';

        private final int startRow;
        private final int height;
        private final Candle[] candles;
        private final Random random;

        ChartBand(int startRow, int height, int candleCount, Random random) {
            this.startRow = startRow;
            this.height = height;
            this.random = random;
            this.candles = new Candle[candleCount];
            // Seed the series with a continuous price walk.
            double price = 0.4 + random.nextDouble() * 0.2;  // start near middle
            for (int i = 0; i < candleCount; i++) {
                double open = price;
                double change = (random.nextDouble() - 0.5) * VOLATILITY;
                double close = clamp(open + change, 0.05, 0.95);
                double wickRange = Math.max(Math.abs(close - open), 0.03) * (0.5 + random.nextDouble());
                double high = Math.max(open, close) + wickRange * random.nextDouble() * 0.5;
                double low = Math.min(open, close) - wickRange * random.nextDouble() * 0.5;
                candles[i] = new Candle(open, close, clamp(high, 0.02, 0.98), clamp(low, 0.02, 0.98));
                price = close;
            }
        }

        void update() {
            // Advance the price walk: each candle's open = previous close.
            double prevClose = candles[candles.length - 1].close;
            for (int i = 0; i < candles.length; i++) {
                double open = prevClose;
                double change = (random.nextDouble() - 0.5) * VOLATILITY;
                double meanReversion = (0.5 - open) * 0.03;
                double close = clamp(open + change + meanReversion, 0.05, 0.95);
                double bodyRange = Math.abs(close - open);
                double wickRange = Math.max(bodyRange, 0.03) * (0.5 + random.nextDouble());
                double high = Math.max(open, close) + wickRange * random.nextDouble() * 0.5;
                double low = Math.min(open, close) - wickRange * random.nextDouble() * 0.5;
                candles[i] = new Candle(open, close, clamp(high, 0.02, 0.98), clamp(low, 0.02, 0.98));
                prevClose = close;
            }
        }

        void draw(TextGraphics graphics, int cols) {
            for (int i = 0; i < candles.length; i++) {
                int screenX = i * CANDLE_SPACING;
                if (screenX >= cols) break;
                drawCandle(graphics, screenX, candles[i], cols);
            }
        }

        private void drawCandle(TextGraphics graphics, int x, Candle candle, int cols) {
            boolean bullish = candle.close >= candle.open;
            AnsiColor bodyColor = bullish ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_RED;
            AnsiColor wickColor = AnsiColor.BRIGHT_BLACK;  // dim wicks

            int top = priceToRow(candle.high);
            int bottom = priceToRow(candle.low);
            int bodyTop = priceToRow(Math.max(candle.open, candle.close));
            int bodyBottom = priceToRow(Math.min(candle.open, candle.close));

            // Ensure body is at least 1 row.
            if (bodyTop == bodyBottom) {
                bodyBottom = Math.min(bodyTop + 1, height - 1);
            }

            // Draw wick (dim, thin) across the full high-low range.
            TextCell wickCell = new TextCell(WICK, wickColor, AnsiColor.BLACK);
            for (int row = top; row <= bottom; row++) {
                int screenRow = startRow + row;
                if (row >= 0 && row < height && screenRow >= 0) {
                    graphics.setCell(x, screenRow, wickCell);
                }
            }

            // Draw body (bright, full block) over the wick for body rows.
            TextCell bodyCell = new TextCell(FULL_BLOCK, bodyColor, AnsiColor.BLACK, SGR.BOLD);
            for (int row = bodyTop; row <= bodyBottom; row++) {
                int screenRow = startRow + row;
                if (row >= 0 && row < height && screenRow >= 0) {
                    graphics.setCell(x, screenRow, bodyCell);
                }
            }
        }

        private int priceToRow(double price) {
            // price [0..1] → row [0..height-1], inverted (high price = top = row 0)
            int row = (int) ((1.0 - price) * (height - 1));
            return Math.max(0, Math.min(height - 1, row));
        }

        /** Returns all candle data.
 * @return the candle array */

        public Candle[] getCandles() { return candles; }
        /** Returns the row where this band starts.
 * @return the starting row */
        public int getStartRow() { return startRow; }
        /** Returns the band height in rows.
 * @return the height in rows */
        public int getHeight() { return height; }
    }

    /** Immutable OHLC data for a single candle. */
    public static final class Candle {
        private final double open;
        private final double close;
        private final double high;
        private final double low;

        Candle(double open, double close, double high, double low) {
            this.open = open;
            this.close = close;
            this.high = high;
            this.low = low;
        }

        /** Returns whether this candle is bullish (close >= open).
 * @return true if bullish */

        public boolean isBullish() { return close >= open; }
        /** Returns the opening price.
 * @return the opening price */
        public double getOpen() { return open; }
        /** Returns the closing price.
 * @return the closing price */
        public double getClose() { return close; }
        /** Returns the high price.
 * @return the high price */
        public double getHigh() { return high; }
        /** Returns the low price.
 * @return the low price */
        public double getLow() { return low; }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
