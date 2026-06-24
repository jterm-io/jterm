package io.jterm.animation;

import io.jterm.core.TerminalSize;
import io.jterm.graphics.TextGraphics;
import io.jterm.style.AnsiColor;
import io.jterm.style.Color;
import io.jterm.style.SGR;
import io.jterm.style.TextCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated network packet flow background. Fixed topology nodes are connected by
 * cyan edges, and bright dots representing packets travel along those edges.
 * Nodes pulse when packets arrive or depart.
 */
public class PacketFlow implements AnimatedBackground {

    private static final int TARGET_FPS = 12;
    private static final int NODE_COUNT = 8;
    private static final int EDGES_PER_NODE = 2;
    private static final int MAX_PACKETS = 12;
    private static final double PACKET_SPEED = 0.06;
    private static final int PULSE_DECAY_FRAMES = 6;

    private final Random random = new Random();

    private volatile boolean running;
    private volatile TerminalSize lastSize;

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Packet> packets = new ArrayList<>();
    private final List<Integer> pulseFrames = new ArrayList<>();

    public PacketFlow(TerminalSize preferredSize) {
        onResize(preferredSize);
    }

    @Override
    public void renderFrame(TextGraphics graphics, TerminalSize size) {
        lastSize = size;
        if (size.columns() <= 0 || size.rows() <= 0) {
            return;
        }

        ensureTopology(size);
        spawnInitialPackets();
        advancePackets();
        decayPulses();
        draw(graphics, size);
    }

    /** Visible for tests: render at a deterministic state. */
    public void renderAtTime(TextGraphics graphics, TerminalSize size, double ignored) {
        renderFrame(graphics, size);
    }

    private synchronized void ensureTopology(TerminalSize size) {
        if (!nodes.isEmpty()) {
            return;
        }

        int cols = size.columns();
        int rows = size.rows();

        // Use minimal margins so nodes spread across the full screen width.
        int marginX = Math.max(1, cols / 20);
        int marginY = Math.max(1, rows / 20);
        int usableW = Math.max(1, cols - 2 * marginX);
        int usableH = Math.max(1, rows - 2 * marginY);

        nodes.clear();
        edges.clear();
        packets.clear();
        pulseFrames.clear();

        for (int i = 0; i < NODE_COUNT; i++) {
            // Spread nodes across the full usable area (0.0 to 1.0)
            int x = marginX + (int) Math.round(usableW * random.nextDouble());
            int y = marginY + (int) Math.round(usableH * random.nextDouble());
            x = Math.max(marginX, Math.min(cols - marginX - 1, x));
            y = Math.max(marginY, Math.min(rows - marginY - 1, y));
            nodes.add(new Node(i, "N" + (i + 1), x, y));
            pulseFrames.add(0);
        }

        // Connect each node to its nearest neighbors.
        for (int i = 0; i < nodes.size(); i++) {
            Node a = nodes.get(i);
            List<Node> others = new ArrayList<>(nodes);
            others.remove(a);
            others.sort((n1, n2) -> Double.compare(distanceSq(a, n1), distanceSq(a, n2)));
            for (int j = 0; j < Math.min(EDGES_PER_NODE, others.size()); j++) {
                Node b = others.get(j);
                Edge edge = new Edge(a, b);
                if (!edges.contains(edge)) {
                    edges.add(edge);
                    a.neighbors.add(b);
                    b.neighbors.add(a);
                }
            }
        }

        // Ensure the graph is fully connected via a deterministic fallback.
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node a = nodes.get(i);
            Node b = nodes.get(i + 1);
            Edge fallback = new Edge(a, b);
            if (!edges.contains(fallback)) {
                edges.add(fallback);
                a.neighbors.add(b);
                b.neighbors.add(a);
            }
        }
    }

    private void spawnInitialPackets() {
        while (packets.size() < Math.min(4, edges.size())) {
            spawnPacketFromRandomNode();
        }
    }

    private void spawnPacketFromRandomNode() {
        if (nodes.isEmpty()) {
            return;
        }
        Node source = nodes.get(random.nextInt(nodes.size()));
        spawnFrom(source);
    }

    private void spawnFrom(Node source) {
        if (source.neighbors.isEmpty()) {
            return;
        }
        Node dest = source.neighbors.get(random.nextInt(source.neighbors.size()));
        AnsiColor color = random.nextBoolean() ? AnsiColor.BRIGHT_GREEN : AnsiColor.BRIGHT_YELLOW;
        packets.add(new Packet(source, dest, 0.0, color));
        triggerPulse(source.index);
    }

    private synchronized void advancePackets() {
        List<Packet> arrived = new ArrayList<>();
        for (Packet p : packets) {
            p.progress += PACKET_SPEED;
            if (p.progress >= 1.0) {
                p.progress = 1.0;
                arrived.add(p);
            }
        }

        for (Packet p : arrived) {
            packets.remove(p);
            triggerPulse(p.destination.index);
            if (packets.size() < MAX_PACKETS) {
                spawnFrom(p.destination);
            } else {
                spawnPacketFromRandomNode();
            }
        }
    }

    private void decayPulses() {
        for (int i = 0; i < pulseFrames.size(); i++) {
            int frames = pulseFrames.get(i);
            if (frames > 0) {
                pulseFrames.set(i, frames - 1);
            }
        }
    }

    private void triggerPulse(int nodeIndex) {
        if (nodeIndex >= 0 && nodeIndex < pulseFrames.size()) {
            pulseFrames.set(nodeIndex, PULSE_DECAY_FRAMES);
        }
    }

    private void draw(TextGraphics graphics, TerminalSize size) {
        // 1. Background.
        TextCell bg = new TextCell(' ', AnsiColor.BLACK, AnsiColor.BLACK);
        graphics.fillRectangle(0, 0, size.columns(), size.rows(), bg);

        // 2. Edges (dim cyan).
        TextCell edgeCell = new TextCell('-', AnsiColor.CYAN, AnsiColor.BLACK, SGR.DIM);
        for (Edge e : edges) {
            graphics.drawLineSmooth(e.a.x, e.a.y, e.b.x, e.b.y, edgeCell);
        }

        // 3. Nodes (blue, bright when pulsing).
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            TextCell nodeCell = nodeCell(n, pulseFrames.get(i));
            graphics.setCell(n.x, n.y, nodeCell);
        }

        // 4. Packets (bright dots on top).
        for (Packet p : packets) {
            int x = (int) Math.round(lerp(p.source.x, p.destination.x, p.progress));
            int y = (int) Math.round(lerp(p.source.y, p.destination.y, p.progress));
            if (x >= 0 && x < size.columns() && y >= 0 && y < size.rows()) {
                TextCell packetCell = new TextCell('*', p.color, AnsiColor.BLACK, SGR.BOLD);
                graphics.setCell(x, y, packetCell);
            }
        }
    }

    private TextCell nodeCell(Node n, int pulseRemaining) {
        char glyph;
        if (n.index % 3 == 0) {
            glyph = '@';
        } else if (n.index % 3 == 1) {
            glyph = '#';
        } else {
            glyph = 'O';
        }

        boolean pulsing = pulseRemaining > 0;
        Color fg = pulsing ? AnsiColor.BRIGHT_BLUE : AnsiColor.BLUE;
        SGR sgr = pulsing ? SGR.BOLD : SGR.DIM;
        return new TextCell(glyph, fg, AnsiColor.BLACK, sgr);
    }

    private static double lerp(int a, int b, double t) {
        return a + (b - a) * t;
    }

    private static double distanceSq(Node a, Node b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    @Override
    public synchronized void onResize(TerminalSize newSize) {
        this.lastSize = newSize;
        nodes.clear();
        edges.clear();
        packets.clear();
        pulseFrames.clear();
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

    /** Visible for tests: read the current nodes. */
    public synchronized List<Node> getNodes() {
        return List.copyOf(nodes);
    }

    /** Visible for tests: read the current edges. */
    public synchronized List<Edge> getEdges() {
        return List.copyOf(edges);
    }

    /** Visible for tests: read the current packets. */
    public synchronized List<Packet> getPackets() {
        return List.copyOf(packets);
    }

    /** Visible for tests: read the current pulse counters. */
    public synchronized List<Integer> getPulseFrames() {
        return List.copyOf(pulseFrames);
    }

    /** Network node with fixed position and mutable neighbor list. */
    public static final class Node {
        private final int index;
        private final String label;
        private final int x;
        private final int y;
        private final List<Node> neighbors = new ArrayList<>();

        public Node(int index, String label, int x, int y) {
            this.index = index;
            this.label = label;
            this.x = x;
            this.y = y;
        }

        public int index() { return index; }
        public String label() { return label; }
        public int x() { return x; }
        public int y() { return y; }
        public List<Node> neighbors() { return List.copyOf(neighbors); }
    }

    /** Undirected edge between two nodes. */
    public static final class Edge {
        private final Node a;
        private final Node b;

        public Edge(Node a, Node b) {
            this.a = a;
            this.b = b;
        }

        public Node a() { return a; }
        public Node b() { return b; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge other)) return false;
            return (a == other.a && b == other.b) || (a == other.b && b == other.a);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(a) + System.identityHashCode(b);
        }
    }

    /** Packet traveling from source to destination with progress 0.0..1.0. */
    public static final class Packet {
        private final Node source;
        private final Node destination;
        private double progress;
        private final AnsiColor color;

        public Packet(Node source, Node destination, double progress, AnsiColor color) {
            this.source = source;
            this.destination = destination;
            this.progress = progress;
            this.color = color;
        }

        public Node source() { return source; }
        public Node destination() { return destination; }
        public double progress() { return progress; }
        public AnsiColor color() { return color; }
    }
}
