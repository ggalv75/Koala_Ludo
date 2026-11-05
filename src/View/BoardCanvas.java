package View;

import Model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Draws the board, the pieces and the pieces that can move (FR-04, FR-07 to FR-11).
 * A click on a piece is reported to the Controller; the Model decides whether it is legal.
 */
public final class BoardCanvas extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int FRAME_DELAY_MS = 16;
    private static final long LEG_DURATION_NS = 130_000_000L;
    private static final long RELEASE_DURATION_NS = 400_000_000L;
    private static final long CELEBRATION_DURATION_NS = 620_000_000L;

    private GameState state;
    private final IntConsumer onPieceClick;
    private final List<DrawnPiece> drawnPieces = new ArrayList<>();
    private final javax.swing.Timer effectsTimer;
    private final List<Runnable> onEffectsFinished = new ArrayList<>();
    private boolean animationsEnabled = true;
    private int hoveredPieceId = -1;
    private Motion motion;
    private Celebration celebration;
    private int originX;
    private int originY;
    private int cell;

    private record DrawnPiece(int id, int x, int y, int radius) { }
    private record Motion(Move move, PlayerColor color, long startedAt, long duration) { }
    private record Celebration(int pieceId, PlayerColor color, long startedAt) { }

    public BoardCanvas(IntConsumer onPieceClick) {
        this.onPieceClick = Objects.requireNonNull(onPieceClick);
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(640, 640));
        effectsTimer = new javax.swing.Timer(FRAME_DELAY_MS, e -> tick());
        effectsTimer.setCoalesce(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) { updateHover(-1); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { handleHover(e.getPoint()); }
        });
    }

    /** Keep movement visual-only: the Model has already committed its legal Move. */
    public void setState(GameState state, Move committedMove) {
        this.state = Objects.requireNonNull(state);
        updateHover(-1);
        if (committedMove != null && animationsEnabled) {
            Piece piece = state.piece(committedMove.pieceId());
            int legs = committedMove.release() ? 1 : Math.max(1, committedMove.toStep() - committedMove.fromStep());
            long duration = committedMove.release() ? RELEASE_DURATION_NS : legs * LEG_DURATION_NS;
            motion = new Motion(committedMove, piece.color(), System.nanoTime(), duration);
            celebration = null;
        }
        updateTimer();
        repaint();
    }

    /** Legacy convenience for initial game state and ordinary notifications. */
    public void setState(GameState state) { setState(state, null); }

    /** Disabling animations also provides a reduced-motion option for accessibility. */
    public void setAnimationsEnabled(boolean enabled) {
        if (animationsEnabled == enabled) return;
        animationsEnabled = enabled;
        if (!enabled) {
            boolean hadEffects = isBusy();
            motion = null;
            celebration = null;
            if (hadEffects) completeVisualEffects();
        }
        updateTimer();
        repaint();
    }

    public boolean isBusy() { return motion != null || celebration != null; }

    /** Used by Controller to keep the final-piece animation visible before ResultsView. */
    public void whenEffectsFinish(Runnable continuation) {
        Objects.requireNonNull(continuation);
        if (!isBusy()) continuation.run();
        else onEffectsFinished.add(continuation);
    }

    /** Stop timers and discard deferred callbacks when navigating away from a game. */
    public void stopEffects() {
        effectsTimer.stop();
        motion = null;
        celebration = null;
        onEffectsFinished.clear();
        updateHover(-1);
    }

    private void completeVisualEffects() {
        List<Runnable> callbacks = List.copyOf(onEffectsFinished);
        onEffectsFinished.clear();
        for (Runnable callback : callbacks) callback.run();
    }

    private void tick() {
        long now = System.nanoTime();
        boolean wasBusy = isBusy();
        if (motion != null && now - motion.startedAt() >= motion.duration()) {
            Motion completed = motion;
            motion = null;
            if (state != null && completed.move().toStep() == state.board().finishStep()) {
                celebration = new Celebration(completed.move().pieceId(), completed.color(), now);
            }
        }
        if (celebration != null && now - celebration.startedAt() >= CELEBRATION_DURATION_NS) {
            celebration = null;
        }
        updateTimer();
        repaint();
        if (wasBusy && !isBusy()) completeVisualEffects();
    }

    private void updateTimer() {
        boolean shouldRun = isBusy() || (animationsEnabled && state != null
                && state.phase() == GameState.Phase.WAITING_FOR_MOVE && !state.legalMoves().isEmpty());
        if (shouldRun && !effectsTimer.isRunning()) effectsTimer.start();
        if (!shouldRun && effectsTimer.isRunning()) effectsTimer.stop();
    }

    private void handleClick(Point point) {
        if (isBusy()) return;
        DrawnPiece firstHit = null;
        for (int i = drawnPieces.size() - 1; i >= 0; i--) {
            DrawnPiece piece = drawnPieces.get(i);
            if (point.distance(piece.x(), piece.y()) <= piece.radius() + 3) {
                if (firstHit == null) firstHit = piece;
                if (state != null && state.canMove(state.piece(piece.id()))) {
                    onPieceClick.accept(piece.id());
                    return;
                }
            }
        }
        if (firstHit != null) onPieceClick.accept(firstHit.id());
    }

    private void handleHover(Point point) {
        if (isBusy() || state == null || state.phase() != GameState.Phase.WAITING_FOR_MOVE) {
            updateHover(-1);
            return;
        }
        for (int i = drawnPieces.size() - 1; i >= 0; i--) {
            DrawnPiece piece = drawnPieces.get(i);
            if (point.distance(piece.x(), piece.y()) <= piece.radius() + 4
                    && state.canMove(state.piece(piece.id()))) {
                updateHover(piece.id());
                return;
            }
        }
        updateHover(-1);
    }

    private void updateHover(int pieceId) {
        if (hoveredPieceId == pieceId) return;
        hoveredPieceId = pieceId;
        setCursor(Cursor.getPredefinedCursor(pieceId >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (state == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Board board = state.board();
        int n = board.size();
        cell = Math.max(10, Math.min((getWidth() - 20) / n, (getHeight() - 20) / n));
        originX = (getWidth() - cell * n) / 2;
        originY = (getHeight() - cell * n) / 2;

        for (PlayerColor color : PlayerColor.values()) drawCastle(g, board, color);
        board.cells().forEach((position, c) -> drawCell(g, position, c));
        drawBlocks(g);
        drawCenter(g, board);
        drawPathPreview(g, board);
        drawPieces(g, board);
        drawFinishCelebration(g, board);
        g.dispose();
    }

    private Rectangle rect(Position p) {
        return new Rectangle(originX + p.col() * cell, originY + p.row() * cell, cell, cell);
    }

    private void drawCastle(Graphics2D g, Board board, PlayerColor color) {
        Rectangle r = rect(board.castleCorner(color));
        int size = cell * 6;
        g.setColor(Theme.softColorOf(color));
        g.fillRoundRect(r.x + 2, r.y + 2, size - 4, size - 4, cell, cell);
        g.setColor(Theme.colorOf(color));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(r.x + 2, r.y + 2, size - 4, size - 4, cell, cell);
    }

    private void drawCell(Graphics2D g, Position position, Cell c) {
        Rectangle r = rect(position);
        Color fill = switch (c.type()) {
            case START, ARROW, HOME -> Theme.softColorOf(c.color());
            case SAFE -> Theme.SAFE_SOFT;
            case REGULAR -> Theme.SURFACE;
        };
        g.setColor(fill);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Theme.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(r.x, r.y, r.width, r.height);
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        switch (c.type()) {
            case START -> drawText(g, "#", cx, cy, Theme.colorOf(c.color()), (int) (cell * 0.6));
            case SAFE -> {
                g.setColor(Theme.SAFE);
                g.fill(star(cx, cy, cell * 0.38, cell * 0.16));
            }
            case ARROW -> drawArrow(g, c.color(), cx, cy);
            default -> { }
        }
    }

    /** FR-30: a ring in the owner's colour marks a Double Block. */
    private void drawBlocks(Graphics2D g) {
        Set<Position> done = new HashSet<>();
        for (Piece piece : state.pieces()) {
            Position p = state.positionOf(piece);
            if (p == null || !done.add(p) || !InteractionRules.isDoubleBlock(state, p)) continue;
            Map<PlayerColor, Integer> perColor = new EnumMap<>(PlayerColor.class);
            for (Piece other : InteractionRules.piecesAt(state, p)) perColor.merge(other.color(), 1, Integer::sum);
            PlayerColor owner = perColor.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).findFirst().orElseThrow();
            Rectangle r = rect(p);
            g.setColor(Theme.colorOf(owner));
            g.setStroke(new BasicStroke(3f));
            g.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        }
    }

    private void drawCenter(Graphics2D g, Board board) {
        Rectangle r = rect(board.centerCorner());
        int d = cell * 3;
        int x = r.x;
        int y = r.y;
        int mx = x + d / 2;
        int my = y + d / 2;
        fillTriangle(g, PlayerColor.RED, new int[]{x, x, mx}, new int[]{y, y + d, my});
        fillTriangle(g, PlayerColor.GREEN, new int[]{x, x + d, mx}, new int[]{y, y, my});
        fillTriangle(g, PlayerColor.YELLOW, new int[]{x + d, x + d, mx}, new int[]{y, y + d, my});
        fillTriangle(g, PlayerColor.BLUE, new int[]{x, x + d, mx}, new int[]{y + d, y + d, my});
        g.setColor(Theme.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, d, d);
    }

    private void fillTriangle(Graphics2D g, PlayerColor color, int[] xs, int[] ys) {
        g.setColor(Theme.softColorOf(color));
        g.fillPolygon(xs, ys, 3);
    }

    private void drawPieces(Graphics2D g, Board board) {
        drawnPieces.clear();
        Map<Position, List<Piece>> onSquares = new LinkedHashMap<>();
        Map<PlayerColor, Integer> finishedCount = new EnumMap<>(PlayerColor.class);
        for (Piece piece : state.pieces()) {
            // Preserve the destination slot even while the moving piece is drawn separately.
            boolean hideMoving = motion != null && motion.move().pieceId() == piece.id();
            if (piece.isInCastle()) {
                Rectangle r = rect(board.castleCorner(piece.color()));
                int index = piece.number() - 1;
                int x = (int) (r.x + cell * (1.9 + (index % 2) * 2.2));
                int y = (int) (r.y + cell * (1.9 + (index / 2) * 2.2));
                if (!hideMoving) drawPiece(g, piece, x, y, (int) (cell * 0.55));
            } else if (state.isFinished(piece)) {
                int k = finishedCount.merge(piece.color(), 1, Integer::sum) - 1;
                Point p = finishSpot(board, piece.color(), k);
                if (!hideMoving) drawPiece(g, piece, p.x, p.y, (int) Math.max(5, cell * 0.2));
            } else if (!hideMoving) {
                onSquares.computeIfAbsent(state.positionOf(piece), key -> new ArrayList<>()).add(piece);
            }
        }
        onSquares.forEach((position, group) -> {
            Rectangle r = rect(position);
            int count = group.size();
            for (int i = 0; i < count; i++) {
                double dx = count == 1 ? 0 : ((i % 2) - 0.5) * 0.42;
                double dy = count <= 2 ? 0 : ((i / 2) - 0.5) * 0.42;
                int radius = (int) (cell * (count == 1 ? 0.34 : 0.24));
                drawPiece(g, group.get(i), (int) (r.getCenterX() + dx * cell), (int) (r.getCenterY() + dy * cell), radius);
            }
        });
        if (motion != null) drawMovingPiece(g, board);
    }

    /** Pieces that finished are drawn inside their colour's triangle in the centre. */
    private Point finishSpot(Board board, PlayerColor color, int k) {
        Rectangle r = rect(board.centerCorner());
        double cx = r.x + cell * 1.5;
        double cy = r.y + cell * 1.5;
        double offset = cell * 0.85;
        double along = (k - 1.5) * cell * 0.38;
        return switch (color) {
            case RED -> new Point((int) (cx - offset), (int) (cy + along));
            case GREEN -> new Point((int) (cx + along), (int) (cy - offset));
            case YELLOW -> new Point((int) (cx + offset), (int) (cy + along));
            case BLUE -> new Point((int) (cx + along), (int) (cy + offset));
        };
    }

    private void drawPiece(Graphics2D g, Piece piece, int x, int y, int radius) {
        drawnPieces.add(new DrawnPiece(piece.id(), x, y, radius));
        if (!isBusy() && state.canMove(piece)) {
            double pulse = animationsEnabled ? 0.5 + 0.5 * Math.sin(System.nanoTime() / 215_000_000.0) : 0.5;
            int reach = 4 + (int) Math.round(pulse * 4);
            int alpha = 75 + (int) Math.round(pulse * 75);
            g.setColor(alpha(Theme.HIGHLIGHT, alpha / 3));
            g.fillOval(x - radius - reach, y - radius - reach,
                    2 * (radius + reach), 2 * (radius + reach));
            g.setColor(alpha(Theme.HIGHLIGHT, alpha));
            g.setStroke(new BasicStroke(2.3f));
            g.drawOval(x - radius - reach + 2, y - radius - reach + 2,
                    2 * (radius + reach - 2), 2 * (radius + reach - 2));
        }
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(x - radius + 1, y - radius + 3, radius * 2, radius * 2);
        g.setColor(Theme.colorOf(piece.color()));
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1.5f, radius * 0.14f)));
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        if (radius >= 7) {
            Color text = piece.color() == PlayerColor.YELLOW ? Theme.INK : Color.WHITE;
            drawText(g, String.valueOf(piece.number()), x, y, text, Math.max(9, (int) (radius * 1.1)));
        }
    }

    /** A translucent preview follows only the actual legal Move supplied by MoveValidator. */
    private void drawPathPreview(Graphics2D g, Board board) {
        if (hoveredPieceId < 0 || isBusy() || state.phase() != GameState.Phase.WAITING_FOR_MOVE) return;
        Move candidate = null;
        for (Move move : state.legalMoves()) {
            if (move.pieceId() == hoveredPieceId) { candidate = move; break; }
        }
        if (candidate == null) return;
        Piece piece = state.piece(hoveredPieceId);
        Color tint = Theme.colorOf(piece.color());
        int from = candidate.release() ? 0 : candidate.fromStep() + 1;
        int to = candidate.toStep();
        for (int step = from; step <= to; step++) {
            Position p = board.positionOf(piece.color(), step);
            if (p == null) continue; // finish area is rendered separately below
            Rectangle r = rect(p);
            g.setColor(alpha(tint, step == to ? 100 : 43));
            g.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 9, 9);
            g.setColor(alpha(tint, 190));
            int dot = Math.max(3, cell / 9);
            g.fillOval((int) r.getCenterX() - dot / 2, (int) r.getCenterY() - dot / 2, dot, dot);
        }
        Point target = anchor(board, piece, to);
        int radius = Math.max(6, (int) (cell * 0.4));
        g.setColor(alpha(tint, 220));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(target.x - radius, target.y - radius, radius * 2, radius * 2);
    }

    /** Paints one piece along the exact route; no intermediate Model mutations. */
    private void drawMovingPiece(Graphics2D g, Board board) {
        Motion anim = motion;
        Move move = anim.move();
        Piece piece = state.piece(move.pieceId());
        int legs = move.release() ? 1 : Math.max(1, move.toStep() - move.fromStep());
        double progress = Math.min(1.0, Math.max(0.0,
                (double) (System.nanoTime() - anim.startedAt()) / anim.duration()));
        double travelled = progress * legs;
        int leg = Math.min(legs - 1, (int) Math.floor(travelled));
        double t = smoothstep(Math.min(1.0, travelled - leg));

        Point a = (move.release() && leg == 0)
                ? castleSlot(board, piece)
                : anchor(board, piece, move.fromStep() + leg);
        Point b = anchor(board, piece, move.release() ? 0 : move.fromStep() + leg + 1);
        int x = (int) Math.round(a.x + (b.x - a.x) * t);
        int y = (int) Math.round(a.y + (b.y - a.y) * t - Math.sin(t * Math.PI) * cell * 0.15);
        int trackRadius = Math.max(5, (int) (cell * 0.34));
        int startRadius = move.release() ? (int) (cell * 0.55) : trackRadius;
        int endRadius = state.isFinished(piece) ? Math.max(5, (int) (cell * 0.2)) : trackRadius;
        double sizeProgress = progress;
        int radius = (int) Math.round(startRadius + (endRadius - startRadius) * sizeProgress);
        // Suppress clickable hitboxes while the move is animating.
        drawPiece(g, piece, x, y, Math.max(5, radius));
    }

    private Point anchor(Board board, Piece piece, int step) {
        if (step == Piece.IN_CASTLE) return castleSlot(board, piece);
        Position p = board.positionOf(piece.color(), step);
        if (p != null) {
            Rectangle r = rect(p);
            return new Point((int) r.getCenterX(), (int) r.getCenterY());
        }
        if (step == board.finishStep()) return finishSpot(board, piece.color(), finishedIndex(piece));
        throw new IllegalArgumentException("Invalid step " + step);
    }

    private Point castleSlot(Board board, Piece piece) {
        Rectangle r = rect(board.castleCorner(piece.color()));
        int index = piece.number() - 1;
        return new Point((int) (r.x + cell * (1.9 + (index % 2) * 2.2)),
                (int) (r.y + cell * (1.9 + (index / 2) * 2.2)));
    }

    private int finishedIndex(Piece piece) {
        int index = 0;
        for (Piece other : state.pieces()) {
            if (other.color() == piece.color() && state.isFinished(other)) {
                if (other.id() == piece.id()) return index;
                index++;
            }
        }
        return index;
    }

    /** A brief, deterministic sparkle halo for one piece reaching the finish. */
    private void drawFinishCelebration(Graphics2D g, Board board) {
        if (celebration == null) return;
        Piece piece = state.piece(celebration.pieceId());
        Point center = anchor(board, piece, board.finishStep());
        double t = Math.min(1.0, Math.max(0.0,
                (double) (System.nanoTime() - celebration.startedAt()) / CELEBRATION_DURATION_NS));
        int opacity = (int) Math.round(225 * (1.0 - t));
        Color color = Theme.colorOf(celebration.color());
        g.setColor(alpha(color, opacity));
        g.setStroke(new BasicStroke(2.4f));
        int r = (int) (cell * (0.28 + 0.85 * t));
        g.drawOval(center.x - r, center.y - r, 2 * r, 2 * r);
        for (int k = 0; k < 10; k++) {
            double angle = 2 * Math.PI * k / 10.0;
            int d = (int) (cell * (0.55 + 0.9 * t));
            int x = (int) (center.x + Math.cos(angle) * d);
            int y = (int) (center.y + Math.sin(angle) * d);
            int size = Math.max(2, (int) (cell * 0.15 * (1 - t)));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }

    private static double smoothstep(double t) { return t * t * (3.0 - 2.0 * t); }

    private static Color alpha(Color color, int opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, opacity)));
    }

    private void drawText(Graphics2D g, String text, int cx, int cy, Color color, int size) {
        g.setFont(Theme.font(size, true));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    /** Arrow pointing into the colour's home path (FR-10). */
    private void drawArrow(Graphics2D g, PlayerColor color, int cx, int cy) {
        double angle = switch (color) {
            case RED -> 0;
            case GREEN -> Math.PI / 2;
            case YELLOW -> Math.PI;
            case BLUE -> -Math.PI / 2;
        };
        double s = cell * 0.32;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(-s, -s * 0.28);
        arrow.lineTo(s * 0.2, -s * 0.28);
        arrow.lineTo(s * 0.2, -s * 0.7);
        arrow.lineTo(s, 0);
        arrow.lineTo(s * 0.2, s * 0.7);
        arrow.lineTo(s * 0.2, s * 0.28);
        arrow.lineTo(-s, s * 0.28);
        arrow.closePath();
        Graphics2D rotated = (Graphics2D) g.create();
        rotated.translate(cx, cy);
        rotated.rotate(angle);
        rotated.setColor(Theme.colorOf(color));
        rotated.fill(arrow);
        rotated.dispose();
    }

    private static Shape star(double cx, double cy, double outer, double inner) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + r * Math.cos(a);
            double y = cy + r * Math.sin(a);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }
}
