
package View;

import Model.*;
import View.GameSounds.SoundEffect;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;

public final class GameBoardView extends JPanel implements GameListener {
    private static final long serialVersionUID = 1L;
    private final BoardCanvas canvas;
    private final JLabel clock = Theme.label("00:00:00", 24, true);
    private final JLabel level = Theme.label("", 14, false);
    private final JLabel turn = Theme.label("", 18, true);
    private final DieFace die = new DieFace();
    /** Boost / UltraBoost die, shown only when the throw has one (FR-15, FR-21). */
    private final DieFace multiplierDie = new DieFace();
    private final JLabel rollInfo = Theme.label("", 14, false);
    /** FR-15, FR-16: multiplier die value and steps on the track; hidden when there is no multiplier. */
    private final JLabel multiplierInfo = Theme.label("", 13, false);
    private final JLabel trackStepsInfo = Theme.label("", 13, false);
    private final JButton rollButton = Theme.button("הטלת קובייה", true);
    private final JLabel status = Theme.label("", 14, false);
    private final JPanel playersList = new JPanel(new GridLayout(0, 1, 0, 6));
    private JPanel playersCard;
    private final JTextArea log = new JTextArea();
    private final LongSupplier elapsedSeconds;
    private final Timer clockTimer;
    private GameState displayedState;
    private final List<VisualEvent> pendingVisualEvents = new ArrayList<>();
    private final Deque<VisualFrame> visualFrames = new ArrayDeque<>();
    private final List<Runnable> afterVisualSequence = new ArrayList<>();
    private MovementVisual lastMovement;
    private boolean visualSequenceRunning;
    private Timer surpriseTimer;
    private final JLabel surpriseInfo = Theme.label("", 14, true);
    private JPanel surpriseCard;

    private interface VisualEvent { }

    private static final class MovementVisual implements VisualEvent {
        private final Move move;
        private PieceInteractionEvent interaction;
        private PlayerFinishEvent playerFinish;

        private MovementVisual(Move move) { this.move = move; }
    }

    private record SurpriseVisual(SurpriseEvent event) implements VisualEvent { }

    private record VisualFrame(GameState state, VisualEvent event,
                               Map<Integer, Integer> steps) { }
    private boolean animationsEnabled = true;
    private final GameSounds sounds;

    /** Board without audio (used by tests and anywhere sound is not needed). */
    public GameBoardView(LongSupplier elapsedSeconds, Runnable onRoll, IntConsumer onPieceClick, Runnable onExit) {
        this(elapsedSeconds, onRoll, onPieceClick, onExit, GameSounds.silent());
    }

    /** Initializes the board, sidebar, action handlers and clock timer, with dice and step sounds. */
    public GameBoardView(LongSupplier elapsedSeconds, Runnable onRoll, IntConsumer onPieceClick, Runnable onExit,
                         GameSounds sounds) {
        this.elapsedSeconds = elapsedSeconds;
        this.sounds = Objects.requireNonNull(sounds);
        this.canvas = new BoardCanvas(pieceId -> {
            if (!visualSequenceRunning) onPieceClick.accept(pieceId);
        });
        canvas.setOnSquareLanded(() -> sounds.play(SoundEffect.STEP));
        canvas.setBackground(new Color(245, 238, 219));
        canvas.setOpaque(true);
        setLayout(new BorderLayout(0, 10));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.label("לודו קואלה", 24, true), BorderLayout.LINE_START);
        JButton exit = Theme.button("יציאה למסך הפתיחה", false);
        exit.setName("board-exit");
        exit.addActionListener(e -> onExit.run());
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(new SoundToggle(SoundToggle.Kind.MUSIC, sounds));
        headerActions.add(new SoundToggle(SoundToggle.Kind.EFFECTS, sounds));
        headerActions.add(exit);
        header.add(headerActions, BorderLayout.LINE_END);
        add(header, BorderLayout.NORTH);
        JPanel boardCard = Theme.card(new BorderLayout());
        boardCard.setOpaque(true);
        boardCard.setBackground(new Color(245, 238, 219));
        boardCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(193, 180, 153), 2), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        boardCard.add(canvas, BorderLayout.CENTER);
        add(boardCard, BorderLayout.CENTER);
        add(createSidePanel(onRoll), BorderLayout.LINE_START);
        clockTimer = new Timer(1000, e -> clock.setText(GameClock.format(elapsedSeconds.getAsLong())));
        clockTimer.start();
    }

    /** Builds the sidebar containing game status, dice controls, players and event log. */
    private JComponent createSidePanel(Runnable onRoll) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        JPanel info = roundedPanel(new BorderLayout(0, 7), new Color(255, 254, 250), Theme.KOALA_BORDER);
        JPanel infoTop = new JPanel(new BorderLayout(8, 0));
        infoTop.setOpaque(false);
        clock.setFont(Theme.font(22, true));
        clock.setForeground(Theme.KOALA_PRIMARY_DARK);
        clock.setHorizontalAlignment(SwingConstants.RIGHT);
        level.setForeground(Theme.KOALA_MUTED);
        infoTop.add(clock, BorderLayout.EAST);
        infoTop.add(level, BorderLayout.WEST);
        info.add(infoTop, BorderLayout.NORTH);
        turn.setForeground(Theme.KOALA_TEXT);
        turn.setHorizontalAlignment(SwingConstants.RIGHT);
        turn.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        info.add(turn, BorderLayout.CENTER);
        JCheckBox animations = new JCheckBox("אנימציות משחק וקובייה", true);
        animations.setName("animations-toggle");
        animations.setOpaque(false);
        animations.setFont(Theme.font(12, false));
        animations.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        animations.addActionListener(e -> {
            animationsEnabled = animations.isSelected();
            boolean waitingOnSurprise = surpriseTimer != null;
            if (!animationsEnabled && surpriseTimer != null) {
                surpriseTimer.stop();
                surpriseTimer = null;
            }
            canvas.setAnimationsEnabled(animationsEnabled);
            if (!animationsEnabled && waitingOnSurprise) advanceVisualSequence();
            if (!animationsEnabled) {
                die.finishRollAnimation();
                multiplierDie.finishRollAnimation();
            }
            updateRollButton();
        });
        info.add(animations, BorderLayout.SOUTH);
        side.add(fullWidth(info, 125));
        side.add(Box.createVerticalStrut(10));
        JPanel dice = roundedPanel(new BorderLayout(0, 8), new Color(255, 254, 250), Theme.KOALA_BORDER);
        JLabel diceTitle = Theme.label("הטלת קובייה", 16, true);
        diceTitle.setForeground(Theme.KOALA_TEXT);
        diceTitle.setHorizontalAlignment(SwingConstants.RIGHT);
        dice.add(diceTitle, BorderLayout.NORTH);
        JPanel dieRow = new JPanel(new BorderLayout(8, 0));
        dieRow.setOpaque(false);
        // BoxLayout skips hidden components, so at Easy the step die keeps the whole width.
        JPanel diceFaces = new JPanel();
        diceFaces.setLayout(new BoxLayout(diceFaces, BoxLayout.LINE_AXIS));
        diceFaces.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        diceFaces.setOpaque(false);
        die.setMinimumSize(new Dimension(40, 40));
        multiplierDie.setMinimumSize(new Dimension(40, 40));
        diceFaces.add(die);
        multiplierDie.setName("multiplier-die");
        multiplierDie.setVisible(false);
        diceFaces.add(multiplierDie);
        dieRow.add(diceFaces, BorderLayout.CENTER);
        dieRow.add(rollInfoColumn(), BorderLayout.EAST);
        dice.add(dieRow, BorderLayout.CENTER);
        JPanel diceBottom = new JPanel(new BorderLayout(0, 6));
        diceBottom.setOpaque(false);
        rollButton.setName("roll-button");
        rollButton.setBackground(Theme.KOALA_PRIMARY);
        rollButton.setForeground(Color.WHITE);
        rollButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.KOALA_PRIMARY), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        rollButton.addActionListener(e -> startVisualRoll(onRoll));
        rollButton.setPreferredSize(new Dimension(260, 40));
        diceBottom.add(rollButton, BorderLayout.NORTH);
        status.setFont(Theme.font(13, false));
        status.setForeground(Theme.KOALA_MUTED);
        status.setVerticalAlignment(SwingConstants.CENTER);
        status.setHorizontalAlignment(SwingConstants.RIGHT);
        status.setPreferredSize(new Dimension(260, 22));
        diceBottom.add(status, BorderLayout.SOUTH);
        dice.add(diceBottom, BorderLayout.SOUTH);
        side.add(fullWidth(dice, 202));
        side.add(Box.createVerticalStrut(10));
        surpriseCard = roundedPanel(new BorderLayout(), new Color(255, 248, 221), Theme.KOALA_BORDER);
        surpriseInfo.setName("surprise-info");
        surpriseInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        surpriseInfo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        surpriseInfo.setForeground(Theme.KOALA_PRIMARY_DARK);
        surpriseCard.add(surpriseInfo, BorderLayout.CENTER);
        surpriseCard.setVisible(false);
        side.add(fullWidth(surpriseCard, 100));
        side.add(Box.createVerticalStrut(10));
        playersCard = roundedPanel(new BorderLayout(0, 8), new Color(255, 254, 250), Theme.KOALA_BORDER);
        JLabel playersTitle = Theme.label("שחקנים", 16, true);
        playersTitle.setForeground(Theme.KOALA_TEXT);
        playersTitle.setHorizontalAlignment(SwingConstants.RIGHT);
        playersCard.add(playersTitle, BorderLayout.NORTH);
        playersList.setOpaque(false);
        playersList.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        playersCard.add(playersList, BorderLayout.CENTER);
        side.add(fullWidth(playersCard, 150));
        side.add(Box.createVerticalStrut(10));
        JPanel logCard = roundedPanel(new BorderLayout(0, 7), new Color(255, 254, 250), Theme.KOALA_BORDER);
        JLabel logTitle = Theme.label("מהלך המשחק", 16, true);
        logTitle.setForeground(Theme.KOALA_TEXT);
        logTitle.setHorizontalAlignment(SwingConstants.RIGHT);
        logCard.add(logTitle, BorderLayout.NORTH);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(Theme.font(13, false));
        log.setForeground(Theme.KOALA_TEXT);
        log.setBackground(Theme.KOALA_ROW_BACKGROUND);
        log.setMargin(new Insets(8, 8, 8, 8));
        log.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createLineBorder(Theme.KOALA_BORDER));
        logScroll.getVerticalScrollBar().setUnitIncrement(12);
        logCard.add(logScroll, BorderLayout.CENTER);
        side.add(fullWidth(logCard, 180));
        JScrollPane sidebar = new JScrollPane(side, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.setBorder(null);
        sidebar.setOpaque(false);
        sidebar.getViewport().setOpaque(false);
        sidebar.setPreferredSize(new Dimension(338, 700));
        sidebar.getVerticalScrollBar().setUnitIncrement(16);
        return sidebar;
    }

    /** Creates a rounded panel with a custom background and border. */
    private static JPanel roundedPanel(LayoutManager layout, Color background, Color outline) {
        JPanel panel = new JPanel(layout) {
            /** Paints the panel's rounded background and outline. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return panel;
    }

    /** Sets consistent width constraints for sidebar sections. */
    private static JComponent fullWidth(JComponent component, int height) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setMinimumSize(new Dimension(0, height));
        component.setPreferredSize(new Dimension(320, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return component;
    }

    /** Preserve every committed movement in the order published by the Model. */
    @Override
    public void onPieceMoved(Move move) {
        lastMovement = new MovementVisual(Objects.requireNonNull(move));
        pendingVisualEvents.add(lastMovement);
    }

    @Override
    public void onPieceInteraction(PieceInteractionEvent event) {
        ensureMovementVisual().interaction = event;
    }

    @Override
    public void onPlayerFinished(PlayerFinishEvent event) {
        ensureMovementVisual().playerFinish = event;
    }

    /** Observer callback: present the surprise after the movement that triggered it. */
    @Override
    public void onSurpriseDrawn(SurpriseEvent event) {
        pendingVisualEvents.add(new SurpriseVisual(Objects.requireNonNull(event)));
        lastMovement = null;
    }

    private MovementVisual ensureMovementVisual() {
        if (lastMovement == null) {
            lastMovement = new MovementVisual(null);
            pendingVisualEvents.add(lastMovement);
        }
        return lastMovement;
    }

    /** Refreshes the board and sidebar from the latest game state. */
    @Override
    public void onStateChanged(GameState state) {
        displayedState = state;
        enqueueVisualFrames(state);
        clock.setText(GameClock.format(elapsedSeconds.getAsLong()));
        level.setText("רמת קושי: " + state.difficulty().displayName());
        Player current = state.currentPlayer();
        turn.setText("התור של " + current.name());
        turn.setIcon(KoalaPiecePainter.icon(current.color(), 24));
        DiceRoll latest = state.latestRoll();
        die.setValue(latest == null ? 0 : latest.value());
        updateMultiplierDie(state, latest);
        if (!die.isRolling()) showRollText(state);
        updateRollButton();
        String feedback = state.feedbackMessage();
        status.setText(feedback != null ? feedback : state.phase() == GameState.Phase.GAME_OVER ? "המשחק הסתיים" : "התור של " + current.name());
        status.setToolTipText(feedback);
        updatePlayers(state);
        log.setText(buildLogText(state));
        log.setCaretPosition(0);
        log.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }


    /**
     * Reconstruct intermediate piece positions from the final state and ordered
     * movement/capture events. These maps belong to the View only.
     */
    private void enqueueVisualFrames(GameState state) {
        if (pendingVisualEvents.isEmpty()) {
            if (!visualSequenceRunning) canvas.setState(state);
            return;
        }
        List<VisualEvent> events = List.copyOf(pendingVisualEvents);
        pendingVisualEvents.clear();
        lastMovement = null;
        Map<Integer, Integer> steps = new HashMap<>();
        for (Piece piece : state.pieces()) steps.put(piece.id(), piece.step());

        for (int i = events.size() - 1; i >= 0; i--) {
            if (!(events.get(i) instanceof MovementVisual movement)) continue;
            if (movement.interaction != null && movement.interaction.capture() != null) {
                PieceInteractionEvent.Capture capture = movement.interaction.capture();
                Piece victim = state.piece(capture.pieceId());
                int previousStep = state.board().route(victim.color()).indexOf(capture.square());
                steps.put(victim.id(), previousStep);
            }
            if (movement.move != null) {
                steps.put(movement.move.pieceId(), movement.move.fromStep());
            }
        }

        for (VisualEvent event : events) {
            if (event instanceof MovementVisual movement) {
                if (movement.move != null) {
                    steps.put(movement.move.pieceId(), movement.move.toStep());
                }
                if (movement.interaction != null && movement.interaction.capture() != null) {
                    steps.put(movement.interaction.capture().pieceId(), Piece.IN_CASTLE);
                }
            }
            visualFrames.addLast(new VisualFrame(state, event, Map.copyOf(steps)));
        }
        if (!visualSequenceRunning) {
            visualSequenceRunning = true;
            canvas.setInteractionBlocked(true);
            advanceVisualSequence();
        }
    }

    /** Play each movement and surprise once, without allowing input between frames. */
    private void advanceVisualSequence() {
        while (!visualFrames.isEmpty()) {
            VisualFrame frame = visualFrames.removeFirst();
            if (frame.event() instanceof SurpriseVisual surprise) {
                canvas.setState(frame.state(), null, null, null, frame.steps());
                showSurprise(surprise.event(), frame.state());
                if (animationsEnabled) {
                    surpriseTimer = new Timer(1100, event -> {
                        Timer source = (Timer) event.getSource();
                        source.stop();
                        if (source != surpriseTimer) return;
                        surpriseTimer = null;
                        advanceVisualSequence();
                    });
                    surpriseTimer.setRepeats(false);
                    surpriseTimer.start();
                    updateRollButton();
                    return;
                }
                continue;
            }
            MovementVisual movement = (MovementVisual) frame.event();
            if (movement.move != null && !animationsEnabled) sounds.play(SoundEffect.STEP);
            canvas.setState(frame.state(), movement.move, movement.interaction,
                    movement.playerFinish, frame.steps());
            if (canvas.isBusy()) {
                canvas.whenEffectsFinish(this::advanceVisualSequence);
                updateRollButton();
                return;
            }
        }
        visualSequenceRunning = false;
        if (displayedState != null) canvas.setState(displayedState);
        canvas.setInteractionBlocked(false);
        updateRollButton();
        List<Runnable> callbacks = List.copyOf(afterVisualSequence);
        afterVisualSequence.clear();
        for (Runnable callback : callbacks) callback.run();
    }

    private void showSurprise(SurpriseEvent event, GameState state) {
        String player = state.players().get(event.playerIndex()).name();
        String text = "הפתעה ל" + player + ": " + event.type().displayName();
        if (event.type() == SurpriseType.MOVE_OPPONENT_BACKWARD
                && state.phase() == GameState.Phase.WAITING_FOR_SURPRISE_CHOICE) {
            text += "\nבחר כלי יריב מסומן על הלוח.";
        }
        surpriseInfo.setText("<html><div dir='rtl' style='width:250px'>"
                + Dialogs.escape(text).replace("\n", "<br>") + "</div></html>");
        surpriseInfo.setToolTipText(text);
        surpriseCard.setVisible(true);
    }

    /** Returns the active roll result or the most recent completed roll. */
    private static String rollText(GameState state) {
        DiceRoll active = state.lastRoll();
        if (state.phase() == GameState.Phase.WAITING_FOR_MOVE && active != null) return "תוצאת ההטלה: " + active.value();
        DiceRoll latest = state.latestRoll();
        return latest == null ? "ממתין להטלה" : "ההטלה האחרונה: " + latest.value();
    }

    /** The roll text column: step die result, then the multiplier lines when there are any. */
    private JComponent rollInfoColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
        column.setOpaque(false);
        column.add(Box.createVerticalGlue());
        for (JLabel label : List.of(rollInfo, multiplierInfo, trackStepsInfo)) {
            label.setForeground(Theme.KOALA_MUTED);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setAlignmentX(Component.RIGHT_ALIGNMENT);
            column.add(label);
        }
        multiplierInfo.setName("multiplier-info");
        trackStepsInfo.setName("track-steps-info");
        multiplierInfo.setVisible(false);
        trackStepsInfo.setVisible(false);
        column.add(Box.createVerticalGlue());
        return column;
    }

    /** FR-16: the step die result, and with a multiplier die also its value and the steps on the track. */
    private void showRollText(GameState state) {
        rollInfo.setText(rollText(state));
        DiceRoll shown = state.latestRoll();
        boolean multiplier = shown != null && shown.hasMultiplier();
        multiplierInfo.setVisible(multiplier);
        trackStepsInfo.setVisible(multiplier);
        if (!multiplier) return;
        multiplierInfo.setText("קוביית " + shown.multiplierDie().displayName() + ": " + shown.multiplierValue());
        trackStepsInfo.setText("צעדים במסלול: " + shown.trackSteps());
    }

    /**
     * Shows the multiplier die when the next throw has one, or keeps showing the last
     * multiplier result until the next roll. Hidden at Easy outside the UltraBoost surprise.
     */
    private void updateMultiplierDie(GameState state, DiceRoll latest) {
        MultiplierDie upcoming = state.phase() == GameState.Phase.WAITING_FOR_ROLL
                ? state.nextThrowMultiplier() : MultiplierDie.NONE;
        MultiplierDie shown = upcoming != MultiplierDie.NONE ? upcoming
                : latest != null ? latest.multiplierDie() : MultiplierDie.NONE;
        multiplierDie.setVisible(shown != MultiplierDie.NONE);
        if (shown == MultiplierDie.NONE) return;
        multiplierDie.setKind(shown.faces(), multiplierTint(shown));
        multiplierDie.setToolTipText("קוביית " + shown.displayName());
        boolean sameDieAsLatest = latest != null && latest.multiplierDie() == shown;
        multiplierDie.setValue(sameDieAsLatest ? latest.multiplierValue() : 0);
    }

    /** Boost and UltraBoost get their own face colours so players can tell them apart. */
    private static Color multiplierTint(MultiplierDie die) {
        return die == MultiplierDie.BOOST ? new Color(255, 211, 150) : new Color(214, 188, 255);
    }

    /** Formats recent game events with the newest message first. */
    private static String buildLogText(GameState state) {
        return String.join("\n", state.recentLog());
    }

    /** Plays the dice animation before requesting a roll from the Controller. */
    private void startVisualRoll(Runnable onRoll) {
        if (displayedState == null || displayedState.phase() != GameState.Phase.WAITING_FOR_ROLL || visualSequenceRunning || canvas.isBusy() || die.isRolling()) return;
        surpriseCard.setVisible(false);
        sounds.play(SoundEffect.DICE_ROLL);
        if (!animationsEnabled) {
            onRoll.run();
            return;
        }
        // Only the dice of the coming throw tumble; a die left over from an earlier throw stays still.
        if (displayedState.nextThrowMultiplier() != MultiplierDie.NONE) multiplierDie.animateRoll(() -> { });
        die.animateRoll(() -> {
            try {
                if (displayedState != null && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL) onRoll.run();
            } finally {
                updateRollButton();
            }
        });
        rollInfo.setText("הקובייה מתגלגלת...");
        multiplierInfo.setVisible(false);
        trackStepsInfo.setVisible(false);
        updateRollButton();
    }

    /** Rebuilds player rows with current-turn highlighting, ranks and finish progress. */
    private void updatePlayers(GameState state) {
        playersList.removeAll();
        for (Player player : state.players()) {
            boolean active = player == state.currentPlayer();
            JPanel row = roundedPanel(new BorderLayout(0, 3), active ? Theme.softColorOf(player.color()) : Theme.KOALA_ROW_BACKGROUND, active ? Theme.colorOf(player.color()) : Theme.KOALA_BORDER);
            row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            JPanel rowTop = new JPanel(new BorderLayout(4, 0));
            rowTop.setOpaque(false);
            String nameText = player.name() + (player.isFinished() ? "  |  מקום " + player.rank() : "")
                    + (state.losesNextTurn(player) ? "  |  מפסיד/ה תור" : "");
            JLabel name = Theme.label(nameText, 13, active);
            name.setIcon(KoalaPiecePainter.icon(player.color(), 20));
            name.setIconTextGap(6);
            name.setHorizontalAlignment(SwingConstants.RIGHT);
            name.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            rowTop.add(name, BorderLayout.EAST);
            int finished = state.finishedPieceCount(player);
            JLabel counter = Theme.label(finished + "/" + GameRules.PIECES_PER_PLAYER + " בבית", 12, false);
            counter.setForeground(Theme.KOALA_MUTED);
            rowTop.add(counter, BorderLayout.WEST);
            row.add(rowTop, BorderLayout.CENTER);
            JProgressBar progress = new JProgressBar(0, GameRules.PIECES_PER_PLAYER);
            progress.setValue(finished);
            progress.setForeground(Theme.colorOf(player.color()));
            progress.setBackground(new Color(226, 233, 227));
            progress.setBorderPainted(false);
            progress.setPreferredSize(new Dimension(10, 5));
            row.add(progress, BorderLayout.SOUTH);
            JPanel shiftedRow = new JPanel(new BorderLayout());
            shiftedRow.setOpaque(false);
            shiftedRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            shiftedRow.add(row, BorderLayout.CENTER);
            playersList.add(shiftedRow);
        }
        int height = 52 + state.players().size() * 43;
        playersCard.setMinimumSize(new Dimension(0, height));
        playersCard.setPreferredSize(new Dimension(320, height));
        playersCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        playersCard.revalidate();
        playersList.revalidate();
        playersList.repaint();
    }

    /** Enables dice rolling only when the game and animations allow it. */
    private void updateRollButton() {
        rollButton.setEnabled(displayedState != null && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL && !visualSequenceRunning && !canvas.isBusy() && !die.isRolling());
        boolean special = displayedState != null && displayedState.specialRollPending();
        rollButton.setText(die.isRolling() ? "הקובייה מתגלגלת..." : special ? "הטלה מיוחדת - UltraBoost" : "הטלת קובייה");
    }

    /** Runs the continuation after all board visual effects finish. */
    public void whenVisualEffectsFinish(Runnable continuation) {
        Objects.requireNonNull(continuation);
        if (visualSequenceRunning) afterVisualSequence.add(continuation);
        else canvas.whenEffectsFinish(continuation);
    }

    /** Stops the clock timer, dice animation and board effects. */
    public void stopClock() {
        clockTimer.stop();
        die.cancelRollAnimation();
        multiplierDie.cancelRollAnimation();
        if (surpriseTimer != null) surpriseTimer.stop();
        surpriseTimer = null;
        visualFrames.clear();
        pendingVisualEvents.clear();
        afterVisualSequence.clear();
        lastMovement = null;
        visualSequenceRunning = false;
        canvas.setInteractionBlocked(false);
        canvas.stopEffects();
    }
}

