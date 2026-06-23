package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.screen.ScreenBuffer;
import io.jterm.style.AnsiColor;
import io.jterm.style.TextCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PacketFlow}: AnimatedBackground contract, non-empty render,
 * packet movement, packet respawning, and small terminal sizes.
 */
class PacketFlowTest {

    @Test
    @DisplayName("implements AnimatedBackground contract")
    void implementsAnimatedBackground() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        assertTrue(flow instanceof AnimatedBackground);
        assertEquals(12, flow.targetFps());
        flow.start();
        assertTrue(flow.isRunning());
        flow.stop();
        assertFalse(flow.isRunning());
    }

    @Test
    @DisplayName("renderFrame produces non-empty output")
    void rendersNonBlankOutput() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        flow.renderFrame(new TextGraphics(buffer), size);

        int nonBlank = 0;
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') nonBlank++;
            }
        }
        assertTrue(nonBlank > 0, "expected non-blank rendered output");
    }

    @Test
    @DisplayName("topology has nodes and edges after render")
    void hasNodesAndEdges() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        flow.renderFrame(new TextGraphics(new ScreenBuffer(size)), size);

        assertTrue(flow.getNodes().size() >= 6, "expected at least 6 nodes");
        assertFalse(flow.getEdges().isEmpty(), "expected at least one edge");
    }

    @Test
    @DisplayName("packets move over frames (progress increases)")
    void packetsMove() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        flow.renderFrame(new TextGraphics(buffer), size);
        var packetsBefore = flow.getPackets();
        assertFalse(packetsBefore.isEmpty(), "expected packets to exist");
        double progressBefore = packetsBefore.get(0).progress();

        flow.renderFrame(new TextGraphics(buffer), size);
        double progressAfter = flow.getPackets().get(0).progress();

        assertTrue(progressAfter > progressBefore,
                "packet progress should increase from " + progressBefore + " to " + progressAfter);
    }

    @Test
    @DisplayName("new packets spawn after arrival")
    void packetsSpawnAfterArrival() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        // Render enough frames for packets to arrive and respawn.
        for (int i = 0; i < 200; i++) {
            flow.renderFrame(new TextGraphics(buffer), size);
        }

        assertFalse(flow.getPackets().isEmpty(), "packets should respawn after arrivals");
    }

    @Test
    @DisplayName("node colors include blue and bright blue")
    void usesNodeColors() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        flow.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }
        assertTrue(found.contains(AnsiColor.BLUE) || found.contains(AnsiColor.BRIGHT_BLUE),
                "expected blue node colors in output");
    }

    @Test
    @DisplayName("edge colors include cyan")
    void usesEdgeColors() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        flow.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) != ' ') {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }
        assertTrue(found.contains(AnsiColor.CYAN), "expected cyan edge colors in output");
    }

    @Test
    @DisplayName("packet colors include bright green or yellow")
    void usesPacketColors() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(80, 24);
        var buffer = new ScreenBuffer(size);

        flow.renderFrame(new TextGraphics(buffer), size);

        Set<AnsiColor> found = new HashSet<>();
        for (int r = 0; r < size.rows(); r++) {
            for (int c = 0; c < size.columns(); c++) {
                if (buffer.getCell(c, r).character().charAt(0) == '*') {
                    found.add((AnsiColor) buffer.getCell(c, r).fg());
                }
            }
        }
        assertTrue(found.contains(AnsiColor.BRIGHT_GREEN) || found.contains(AnsiColor.BRIGHT_YELLOW),
                "expected bright green/yellow packet colors");
    }

    @Test
    @DisplayName("renderFrame does not throw on empty size")
    void emptySizeDoesNotCrash() {
        var flow = new PacketFlow(new TerminalSize(0, 0));
        var buffer = new ScreenBuffer(new TerminalSize(0, 0));
        assertDoesNotThrow(() -> flow.renderFrame(new TextGraphics(buffer), new TerminalSize(0, 0)));
    }

    @Test
    @DisplayName("small terminal sizes render without throwing")
    void smallTerminalSizes() {
        var flow = new PacketFlow(new TerminalSize(5, 5));
        var size = new TerminalSize(5, 5);
        var buffer = new ScreenBuffer(size);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                flow.renderFrame(new TextGraphics(buffer), size);
            }
        });
    }

    @Test
    @DisplayName("onResize stores the new size and clears topology")
    void onResizeStoresSize() {
        var flow = new PacketFlow(new TerminalSize(80, 24));
        var size = new TerminalSize(100, 40);
        flow.onResize(size);
        assertEquals(size, flow.lastSize());
        assertTrue(flow.getNodes().isEmpty(), "resize should clear cached topology");
    }
}
