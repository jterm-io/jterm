package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.Random;

/**
 * Animated background that renders a simulated order-book / market-depth ladder.
 * Bid levels (green) stack on the left and ask levels (red) on the right; each
 * row shows a price and a size bar made of full-block and shade characters.
 * Prices drift each frame to simulate market movement, and the current mid
 * price is shown in the center of the ladder.
 */
public class MarketDepth implements AnimatedBackground {

    private static final int TARGET_FPS = 5;

    private static final char FULL_BLOCK = '\u2588';
    private static final char SHADE = '\u2591';
    private static final char MID_ARROW = '\u25b2';

    // Visible price range around the mid price.
    private static final double PRICE_STEP = 0.50;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private double midPrice;
    private double bidSizes[];
    private double askSizes[];

    /**
     * Constructs a new MarketDepth instance.
     * @param preferredSize the preferred size
     */
    public MarketDepth(TerminalSize preferredSize) {
        this.midPrice = 100.0;
        this.bidSizes = new double[0];
        this.askSizes = new double[0];
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

        updateMarket();

        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        int rows = size.rows();
        int cols = size.columns();
        int halfCols = cols / 2;

        ensureLadder(rows);

        // Center row displays the mid price. Rows above are asks (higher prices),
        // rows below are bids (lower prices).
        int centerRow = rows / 2;

        // Draw mid price row first.
        drawMid(graphics, centerRow, cols);

        // Draw asks above the mid row.
        int askLevels = Math.min(centerRow, askSizes.length);
        for (int i = 1; i <= askLevels; i++) {
            double price = midPrice + i * PRICE_STEP;
            int row = centerRow - i;
            if (row >= 0) {
                double sizeValue = askSizes[i - 1];
                drawSide(graphics, row, halfCols, cols, price, sizeValue, AnsiColor.BRIGHT_RED, false);
            }
        }

        // Draw bids below the mid row.
        int bidLevels = Math.min(rows - centerRow - 1, askSizes.length);
        for (int i = 1; i <= bidLevels; i++) {
            double price = midPrice - i * PRICE_STEP;
            int row = centerRow + i;
            if (row < rows) {
                double sizeValue = bidSizes[i - 1];
                drawSide(graphics, row, 0, halfCols, price, sizeValue, AnsiColor.BRIGHT_GREEN, true);
            }
        }
    }

    private void ensureLadder(int rows) {
        int needed = Math.max(1, rows / 2);
        if (bidSizes.length != needed) {
            bidSizes = new double[needed];
            askSizes = new double[needed];
            for (int i = 0; i < needed; i++) {
                bidSizes[i] = randomSize();
                askSizes[i] = randomSize();
            }
        }
    }

    private void updateMarket() {
        double change = (random.nextDouble() - 0.5) * 0.30;
        double meanReversion = (100.0 - midPrice) * 0.02;
        midPrice = clamp(midPrice + change + meanReversion, 1.0, 199.0);

        for (int i = 0; i < bidSizes.length; i++) {
            bidSizes[i] = mutateSize(bidSizes[i]);
        }
        for (int i = 0; i < askSizes.length; i++) {
            askSizes[i] = mutateSize(askSizes[i]);
        }
    }

    private double mutateSize(double current) {
        double next = current + (random.nextDouble() - 0.5) * 30.0;
        return clamp(next, 5.0, 200.0);
    }

    private double randomSize() {
        return 20.0 + random.nextDouble() * 130.0;
    }

    private void drawSide(TextGraphics graphics, int row, int leftCol, int rightCol,
                          double price, double sizeValue, AnsiColor fg, boolean leftSide) {
        int available = Math.max(1, rightCol - leftCol);
        int barLength = (int) (Math.min(sizeValue, 200.0) / 200.0 * available);
        barLength = clamp(barLength, 0, available);

        String priceText = String.format(java.util.Locale.US, "%6.2f", price);
        int priceWidth = priceText.length();

        TextCell fgCell = new TextCell(FULL_BLOCK, fg, AnsiColor.BLACK, SGR.BOLD);
        TextCell shadeCell = new TextCell(SHADE, fg, AnsiColor.BLACK, SGR.BOLD);
        TextCell priceCell = new TextCell(' ', fg, AnsiColor.BLACK, SGR.BOLD);

        if (leftSide) {
            // Price at the right edge of the left half, bar grows leftward.
            int priceStart = rightCol - priceWidth;
            for (int i = 0; i < priceWidth && priceStart + i < rightCol; i++) {
                int col = priceStart + i;
                if (col >= 0) {
                    graphics.setCell(col, row, priceCell.withCharacter(priceText.charAt(i)));
                }
            }
            int barStart = priceStart - barLength;
            for (int i = 0; i < barLength; i++) {
                int col = barStart + i;
                if (col >= leftCol && col >= 0) {
                    graphics.setCell(col, row, fgCell);
                }
            }
            int shadeStart = leftCol;
            int shadeEnd = Math.min(barStart, rightCol);
            for (int col = shadeStart; col < shadeEnd; col++) {
                if (col >= 0) {
                    graphics.setCell(col, row, shadeCell);
                }
            }
        } else {
            // Price at the left edge of the right half, bar grows rightward.
            int priceStart = leftCol;
            for (int i = 0; i < priceWidth && priceStart + i < rightCol; i++) {
                int col = priceStart + i;
                if (col >= 0) {
                    graphics.setCell(col, row, priceCell.withCharacter(priceText.charAt(i)));
                }
            }
            int barStart = priceStart + priceWidth;
            for (int i = 0; i < barLength && barStart + i < rightCol; i++) {
                int col = barStart + i;
                if (col >= 0) {
                    graphics.setCell(col, row, fgCell);
                }
            }
            int shadeStart = barStart + barLength;
            for (int col = shadeStart; col < rightCol; col++) {
                if (col >= 0) {
                    graphics.setCell(col, row, shadeCell);
                }
            }
        }
    }

    private void drawMid(TextGraphics graphics, int row, int cols) {
        String midText = String.format(java.util.Locale.US, " %c %.2f ", MID_ARROW, midPrice);
        int start = Math.max(0, (cols - midText.length()) / 2);
        TextCell fg = new TextCell(' ', AnsiColor.BRIGHT_YELLOW, AnsiColor.BLACK, SGR.BOLD);
        for (int i = 0; i < midText.length() && start + i < cols; i++) {
            graphics.setCell(start + i, row, fg.withCharacter(midText.charAt(i)));
        }
    }

    @Override
    /**
     * Reallocates internal buffers for the new terminal size.
     * @param newSize the new size
     */
    public void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        if (newSize.rows() > 0) {
            ensureLadder(newSize.rows());
        }
    }

    @Override
    /**
     * Starts the animation.
     */
    public void start() {
        running = true;
    }

    @Override
    /**
     * Stops the animation.
     */
    public void stop() {
        running = false;
    }

    @Override
    /**
     * Returns whether the running flag is set.
     * @return the result
     */
    public boolean isRunning() {
        return running;
    }

    @Override
    /**
     * Returns the target frame rate in frames per second.
     * @return the result
     */
    public int targetFps() {
        return TARGET_FPS;
    }

    @Override
    /**
     * Returns the last terminal size the animation was rendered at.
     * @return the result
     */
    public TerminalSize lastSize() {
        return lastSize;
    }

    /**
     * Returns the current mid price, visible for tests.
     *
     * @return the mid price
     */
    public double getMidPrice() {
        return midPrice;
    }

    /**
     * Sets the mid price directly, visible for tests.
     *
     * @param midPrice the mid price, clamped to 1.0-199.0
     */
    public void setMidPrice(double midPrice) {
        this.midPrice = clamp(midPrice, 1.0, 199.0);
    }

    /**
     * Returns the bid size array, visible for tests.
     *
     * @return a copy of the bid sizes (may be empty)
     */
    public double[] getBidSizes() {
        return bidSizes.clone();
    }

    /**
     * Returns the ask sizes.
     * @return the result
     */
    public double[] getAskSizes() {
        return askSizes.clone();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
