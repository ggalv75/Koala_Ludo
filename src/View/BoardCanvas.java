package View;

import Model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private GameState state;
    private final IntConsumer onPieceClick;
    private final List<DrawnPiece> drawnPieces = new ArrayList<>();
    private int originX;
    private int originY;
    private int cell;

    private record DrawnPiece(int id, int x, int y, int radius) { }

    public BoardCanvas(IntConsumer onPieceClick) {
        this.onPieceClick = onPieceClick;
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(640, 640));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getPoint()); }
        });
    }

    public void setState(GameState state) {
        this.state = state;
        repaint();
    }

    private void handleClick(Point point) {
        for (int i = drawnPieces.size() - 1; i >= 0; i--) {
            DrawnPiece piece = drawnPieces.get(i);
            if (point.distance(piece.x(), piece.y()) <= piece.radius() + 3) {
                onPieceClick.accept(piece.id());
                return;
            }
        }
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
        drawPieces(g, board);
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
            if (piece.isInCastle()) {
                Rectangle r = rect(board.castleCorner(piece.color()));
                int index = piece.number() - 1;
                int x = (int) (r.x + cell * (1.9 + (index % 2) * 2.2));
                int y = (int) (r.y + cell * (1.9 + (index / 2) * 2.2));
                drawPiece(g, piece, x, y, (int) (cell * 0.55));
            } else if (state.isFinished(piece)) {
                int k = finishedCount.merge(piece.color(), 1, Integer::sum) - 1;
                Point p = finishSpot(board, piece.color(), k);
                drawPiece(g, piece, p.x, p.y, (int) Math.max(5, cell * 0.2));
            } else {
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
        if (state.canMove(piece)) {
            g.setColor(new Color(Theme.HIGHLIGHT.getRed(), Theme.HIGHLIGHT.getGreen(), Theme.HIGHLIGHT.getBlue(), 190));
            g.fillOval(x - radius - 5, y - radius - 5, 2 * radius + 10, 2 * radius + 10);
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
