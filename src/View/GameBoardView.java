package View;

import Model.*;
import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;

/**
 * Game screen (SRS 3.1.1): board, game clock (FR-48), current player, die and roll button,
 * players' progress and a short game log. It observes the Model through {@link GameListener}.
 */
public final class GameBoardView extends JPanel implements GameListener {
    private static final long serialVersionUID = 1L;
    private final BoardCanvas canvas;
    private final JLabel clock = Theme.label("00:00:00", 24, true);
    private final JLabel level = Theme.label("", 14, false);
    private final JLabel turn = Theme.label("", 18, true);
    private final DieFace die = new DieFace();
    private final JLabel rollInfo = Theme.label("", 14, false);
    private final JButton rollButton = Theme.button("הטלת קובייה", true);
    private final JLabel status = Theme.label("", 14, false);
    private final JPanel playersList = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JTextArea log = new JTextArea();
    private final LongSupplier elapsedSeconds;
    private final Timer clockTimer;
    private GameState displayedState;
    private Move pendingMove;
    private PieceInteractionEvent pendingInteraction;
    private PlayerFinishEvent pendingPlayerFinish;
    private boolean animationsEnabled = true;

    /** Build the game UI and connect it to Controller callbacks. */
    public GameBoardView(LongSupplier elapsedSeconds, Runnable onRoll, IntConsumer onPieceClick, Runnable onExit) {
        this.elapsedSeconds = elapsedSeconds;
        this.canvas = new BoardCanvas(onPieceClick);
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("לודו קואלה", 24, true), BorderLayout.LINE_START);
        JButton exit = Theme.button("יציאה למסך הפתיחה", false);
        exit.setName("board-exit");
        exit.addActionListener(e -> onExit.run());
        header.add(exit, BorderLayout.LINE_END);
        add(header, BorderLayout.NORTH);

        JPanel boardCard = Theme.card(new BorderLayout());
        boardCard.add(canvas, BorderLayout.CENTER);
        add(boardCard, BorderLayout.CENTER);
        add(createSidePanel(onRoll), BorderLayout.LINE_START);

        clockTimer = new Timer(1000, e -> clock.setText(GameClock.format(elapsedSeconds.getAsLong())));
        clockTimer.start();
    }

    /** Build the status, die, players and history side panel. */
    private JPanel createSidePanel(Runnable onRoll) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 700));

        JPanel info = Theme.card(new GridLayout(0, 1, 0, 4));
        info.add(clock);
        info.add(level);
        info.add(turn);
        JCheckBox animations = new JCheckBox("אנימציות משחק וקובייה", true);
        animations.setName("animations-toggle");
        animations.setOpaque(false);
        animations.setFont(Theme.font(13, false));
        animations.addActionListener(e -> {
            animationsEnabled = animations.isSelected();
            canvas.setAnimationsEnabled(animationsEnabled);
            if (!animationsEnabled) die.finishRollAnimation();
            updateRollButton();
        });
        info.add(animations);
        side.add(fullWidth(info, 130));
        side.add(Box.createVerticalStrut(10));

        JPanel dice = Theme.card(new BorderLayout(10, 8));
        JPanel dieRow = new JPanel(new BorderLayout(10, 0));
        dieRow.setOpaque(false);
        dieRow.add(die, BorderLayout.LINE_START);
        dieRow.add(rollInfo, BorderLayout.CENTER);
        dice.add(dieRow, BorderLayout.NORTH);
        rollButton.setName("roll-button");
        rollButton.addActionListener(e -> startVisualRoll(onRoll));
        dice.add(rollButton, BorderLayout.CENTER);
        rollButton.setPreferredSize(new Dimension(260, 44));
        status.setForeground(Theme.MUTED);
        status.setPreferredSize(new Dimension(260, 40));
        status.setVerticalAlignment(SwingConstants.TOP);
        dice.add(status, BorderLayout.SOUTH);
        side.add(fullWidth(dice, 200));
        side.add(Box.createVerticalStrut(10));

        JPanel players = Theme.card(new BorderLayout(0, 8));
        players.add(Theme.label("שחקנים", 16, true), BorderLayout.NORTH);
        playersList.setOpaque(false);
        players.add(playersList, BorderLayout.CENTER);
        side.add(fullWidth(players, 190));
        side.add(Box.createVerticalStrut(10));

        JPanel logCard = Theme.card(new BorderLayout(0, 6));
        logCard.add(Theme.label("מהלך המשחק", 16, true), BorderLayout.NORTH);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(Theme.font(13, false));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(null);
        logCard.add(scroll, BorderLayout.CENTER);
        logCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        logCard.setMinimumSize(new Dimension(0, 80));
        logCard.setPreferredSize(new Dimension(320, 120)); // takes whatever height is left
        side.add(logCard);
        return side;
    }

    /** Set a consistent width and height for a side panel section. */
    private static JComponent fullWidth(JComponent component, int height) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setMinimumSize(new Dimension(0, height));
        component.setPreferredSize(new Dimension(320, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return component;
    }

    /** Remember a committed movement until its state update arrives. */
    @Override
    public void onPieceMoved(Move move) {
        pendingMove = move; // next onStateChanged contains the committed game state
    }

    /** Cache semantic interaction effects until the committed state notification arrives. */
    @Override
    public void onPieceInteraction(PieceInteractionEvent event) {
        pendingInteraction = event;
    }

    /** Cache the earned rank until the finishing move is ready to animate. */
    @Override
    public void onPlayerFinished(PlayerFinishEvent event) {
        pendingPlayerFinish = event;
    }

    /** Refresh the board using the Model's state and any completed movement events. */
    @Override
    public void onStateChanged(GameState state) {
        displayedState = state;
        canvas.setState(state, pendingMove, pendingInteraction, pendingPlayerFinish);
        PlayerFinishEvent finishedPlayer = pendingPlayerFinish;
        pendingMove = null;
        pendingInteraction = null;
        pendingPlayerFinish = null;
        if (canvas.isBusy()) canvas.whenEffectsFinish(this::updateRollButton);
        clock.setText(GameClock.format(elapsedSeconds.getAsLong()));
        level.setText("רמת קושי: " + state.difficulty().displayName());
        Player current = state.currentPlayer();
        turn.setText("התור של " + current.name());
        turn.setIcon(KoalaPiecePainter.icon(current.color(), 24));
        DiceRoll latest = state.latestRoll();
        die.setValue(latest == null ? 0 : latest.value());
        if (!die.isRolling()) rollInfo.setText(rollText(state));
        updateRollButton();
        String statusMessage = finishedPlayer == null ? state.lastMessage()
                : finishedPlayer.playerName() + " השלים/ה את כל ארבעת הכלים! מקום " + finishedPlayer.rank();
        status.setText("<html>" + Dialogs.escape(statusMessage) + "</html>");
        updatePlayers(state);
        log.setText(String.join("\n", state.recentLog()));
        log.setCaretPosition(0);
        log.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    /** Label the active roll separately from the last visible result. */
    private static String rollText(GameState state) {
        DiceRoll active = state.lastRoll();
        if (state.phase() == GameState.Phase.WAITING_FOR_MOVE && active != null)
            return "תוצאת ההטלה: " + active.value();
        DiceRoll latest = state.latestRoll();
        return latest == null ? "ממתין להטלה" : "ההטלה האחרונה: " + latest.value();
    }

    /** Start one animated die roll without letting another roll overlap it. */
    private void startVisualRoll(Runnable onRoll) {
        if (displayedState == null || displayedState.phase() != GameState.Phase.WAITING_FOR_ROLL
                || canvas.isBusy() || die.isRolling()) return;
        if (!animationsEnabled) {
            onRoll.run();
            return;
        }
        die.animateRoll(() -> {
            try {
                if (displayedState != null
                        && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL) onRoll.run();
            } finally {
                updateRollButton();
            }
        });
        rollInfo.setText("הקובייה מתגלגלת...");
        updateRollButton();
    }

    /** Refresh player progress and ranking labels. */
    private void updatePlayers(GameState state) {
        playersList.removeAll();
        for (Player player : state.players()) {
            String text = player.name() + "  |  בבית: " + state.finishedPieceCount(player) + "/" + GameRules.PIECES_PER_PLAYER;
            if (player.isFinished()) text += "  |  מקום " + player.rank();
            JLabel row = Theme.label(text, 14, player == state.currentPlayer());
            row.setIcon(KoalaPiecePainter.icon(player.color(), 22));
            row.setIconTextGap(8);
            row.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            playersList.add(row);
        }
        playersList.revalidate();
        playersList.repaint();
    }

    /** Disable rolling whenever the rules or an animation forbid it. */
    private void updateRollButton() {
        rollButton.setEnabled(displayedState != null
                && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL
                && !canvas.isBusy() && !die.isRolling());
        rollButton.setText(die.isRolling() ? "הקובייה מתגלגלת..." : "הטלת קובייה");
    }

    /** After the last animated piece finishes, the Controller can show the results. */
    public void whenVisualEffectsFinish(Runnable continuation) {
        canvas.whenEffectsFinish(continuation);
    }

    /** Called by the Controller when the game ends or the player leaves the screen. */
    public void stopClock() {
        clockTimer.stop();
        die.cancelRollAnimation();
        canvas.stopEffects();
    }
}
