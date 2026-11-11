
package View;

import Model.*;
import javax.swing.*;
import java.awt.*;
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
    private final JLabel rollInfo = Theme.label("", 14, false);
    private final JButton rollButton = Theme.button("הטלת קובייה", true);
    private final JLabel status = Theme.label("", 14, false);
    private final JPanel playersList = new JPanel(new GridLayout(0, 1, 0, 6));
    private JPanel playersCard;
    private final JTextArea log = new JTextArea();
    private final LongSupplier elapsedSeconds;
    private final Timer clockTimer;
    private GameState displayedState;
    private Move pendingMove;
    private PieceInteractionEvent pendingInteraction;
    private PlayerFinishEvent pendingPlayerFinish;
    private boolean animationsEnabled = true;
    private final GameSounds sounds;

    /** Board without audio (used by tests and anywhere sound is not needed). */
    public GameBoardView(LongSupplier elapsedSeconds, Runnable onRoll, IntConsumer onPieceClick, Runnable onExit) {
        this(elapsedSeconds, onRoll, onPieceClick, onExit, GameSounds.silent());
    }

    /** Board that plays dice and step sounds and shows the music and sound switches. */
    public GameBoardView(LongSupplier elapsedSeconds, Runnable onRoll, IntConsumer onPieceClick, Runnable onExit,
                         GameSounds sounds) {
        this.elapsedSeconds = elapsedSeconds;
        this.sounds = Objects.requireNonNull(sounds);
        this.canvas = new BoardCanvas(onPieceClick);
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
            canvas.setAnimationsEnabled(animationsEnabled);
            if (!animationsEnabled) die.finishRollAnimation();
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
        rollInfo.setForeground(Theme.KOALA_MUTED);
        rollInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        dieRow.add(die, BorderLayout.CENTER);
        dieRow.add(rollInfo, BorderLayout.EAST);
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

    private static JPanel roundedPanel(LayoutManager layout, Color background, Color outline) {
        JPanel panel = new JPanel(layout) {
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

    private static JComponent fullWidth(JComponent component, int height) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setMinimumSize(new Dimension(0, height));
        component.setPreferredSize(new Dimension(320, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return component;
    }

    @Override
    public void onPieceMoved(Move move) {
        pendingMove = move;
    }

    @Override
    public void onPieceInteraction(PieceInteractionEvent event) {
        pendingInteraction = event;
    }

    @Override
    public void onPlayerFinished(PlayerFinishEvent event) {
        pendingPlayerFinish = event;
    }

    @Override
    public void onStateChanged(GameState state) {
        displayedState = state;
        // With animations off there is no walk to follow, so the move gets a single step sound.
        if (pendingMove != null && !animationsEnabled) sounds.play(SoundEffect.STEP);
        canvas.setState(state, pendingMove, pendingInteraction, pendingPlayerFinish);
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
        status.setText(state.phase() == GameState.Phase.GAME_OVER ? "המשחק הסתיים" : "התור של " + current.name());
        updatePlayers(state);
        log.setText(buildLogText(state));
        log.setCaretPosition(0);
        log.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    private static String rollText(GameState state) {
        DiceRoll active = state.lastRoll();
        if (state.phase() == GameState.Phase.WAITING_FOR_MOVE && active != null) return "תוצאת ההטלה: " + active.value();
        DiceRoll latest = state.latestRoll();
        return latest == null ? "ממתין להטלה" : "ההטלה האחרונה: " + latest.value();
    }

    private static String buildLogText(GameState state) {
        StringBuilder text = new StringBuilder();
        for (String entry : state.recentLog()) {
            if (entry == null || entry.isBlank()) continue;
            if (text.length() > 0) text.append("\n");
            text.append(formatLogEntry(entry, state));
        }
        return text.toString();
    }

    private static String formatLogEntry(String entry, GameState state) {
        String start = "המשחק התחיל. התור של ";
        if (entry.startsWith(start)) return entry.substring(start.length()).trim() + ": המשחק התחיל";
        String turnPrefix = "התור של ";
        if (entry.startsWith(turnPrefix)) return entry.substring(turnPrefix.length()).trim() + ": התחיל התור";
        for (Player player : state.players()) {
            String name = player.name();
            if (entry.startsWith(name + " ")) return name + ": " + entry.substring(name.length() + 1);
            String finishSuffix = " של " + name + " הגיע לבית הסיום.";
            if (entry.startsWith("כלי ") && entry.endsWith(finishSuffix)) return name + ": " + entry.substring(0, entry.length() - finishSuffix.length()) + " הגיע לבית הסיום.";
            String bonusPrefix = "בזכות ה-6, " + name + " ";
            if (entry.startsWith(bonusPrefix)) return name + ": בזכות ה-6, " + entry.substring(bonusPrefix.length());
        }
        return "מערכת: " + entry;
    }

    private void startVisualRoll(Runnable onRoll) {
        if (displayedState == null || displayedState.phase() != GameState.Phase.WAITING_FOR_ROLL || canvas.isBusy() || die.isRolling()) return;
        sounds.play(SoundEffect.DICE_ROLL);
        if (!animationsEnabled) {
            onRoll.run();
            return;
        }
        die.animateRoll(() -> {
            try {
                if (displayedState != null && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL) onRoll.run();
            } finally {
                updateRollButton();
            }
        });
        rollInfo.setText("הקובייה מתגלגלת...");
        updateRollButton();
    }

    private void updatePlayers(GameState state) {
        playersList.removeAll();
        for (Player player : state.players()) {
            boolean active = player == state.currentPlayer();
            JPanel row = roundedPanel(new BorderLayout(0, 3), active ? Theme.softColorOf(player.color()) : Theme.KOALA_ROW_BACKGROUND, active ? Theme.colorOf(player.color()) : Theme.KOALA_BORDER);
            row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            JPanel rowTop = new JPanel(new BorderLayout(4, 0));
            rowTop.setOpaque(false);
            String nameText = player.name() + (player.isFinished() ? "  |  מקום " + player.rank() : "");
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

    private void updateRollButton() {
        rollButton.setEnabled(displayedState != null && displayedState.phase() == GameState.Phase.WAITING_FOR_ROLL && !canvas.isBusy() && !die.isRolling());
        rollButton.setText(die.isRolling() ? "הקובייה מתגלגלת..." : "הטלת קובייה");
    }

    public void whenVisualEffectsFinish(Runnable continuation) {
        canvas.whenEffectsFinish(continuation);
    }

    public void stopClock() {
        clockTimer.stop();
        die.cancelRollAnimation();
        canvas.stopEffects();
    }
}
