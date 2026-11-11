package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import Model.Difficulty;
import Model.GameEngine;
import Model.GameState;
import Model.Move;
import Model.Piece;
import Model.PlayerColor;

/** Screens ask for the right sounds at the right moments and the switches work. */
class GameSoundsTest {

    /** Test double: records every effect instead of playing it. */
    private static final class RecordingSounds implements GameSounds {
        final List<SoundEffect> played = Collections.synchronizedList(new ArrayList<>());
        boolean musicOn = true;
        boolean effectsOn = true;

        @Override public void play(SoundEffect effect) { if (effectsOn) played.add(effect); }
        @Override public boolean isMusicOn() { return musicOn; }
        @Override public void setMusicOn(boolean on) { musicOn = on; }
        @Override public boolean isEffectsOn() { return effectsOn; }
        @Override public void setEffectsOn(boolean on) { effectsOn = on; }

        long count(SoundEffect effect) {
            synchronized (played) { return played.stream().filter(effect::equals).count(); }
        }
    }

    private RecordingSounds sounds;
    private GameEngine engine;
    private GameBoardView view;
    private int rollCalls;

    @BeforeEach
    void setUp() throws Exception {
        sounds = new RecordingSounds();
        SwingUtilities.invokeAndWait(() -> {
            PlayerColor[] colors = PlayerColor.values();
            engine = new GameEngine(Difficulty.EASY, List.of("גל", "דור"), List.of(colors[0], colors[1]));
            view = new GameBoardView(() -> 0L, () -> rollCalls++, id -> { }, () -> { }, sounds);
            view.onStateChanged(engine.state());
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> view.stopClock());
    }

    @Test
    @DisplayName("Rolling the die plays the dice sound once")
    void rollPlaysDiceSound() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            byName(view, "animations-toggle").doClick(0);
            byName(view, "roll-button").doClick(0);
            assertEquals(1, rollCalls);
            assertEquals(1, sounds.count(SoundEffect.DICE_ROLL));
        });
    }

    @Test
    @DisplayName("A disabled roll button makes no sound")
    void ignoredRollIsSilent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AbstractButton roll = byName(view, "roll-button");
            roll.doClick(0);
            roll.doClick(0);
        });
        SwingUtilities.invokeAndWait(() -> assertEquals(1, sounds.count(SoundEffect.DICE_ROLL),
                "a second click while the die is still rolling must not play again"));
    }

    @Test
    @DisplayName("An animated move plays one step sound per square")
    void stepSoundPerSquare() throws Exception {
        BoardCanvas canvas = canvas();
        SwingUtilities.invokeAndWait(() -> canvas.setState(engine.state(), new Move(0, 0, 4, false, false)));
        waitUntilIdle(canvas);
        assertEquals(4, sounds.count(SoundEffect.STEP));
    }

    @Test
    @DisplayName("Boundary: leaving the castle is a single step sound")
    void releaseIsOneStep() throws Exception {
        BoardCanvas canvas = canvas();
        SwingUtilities.invokeAndWait(() ->
                canvas.setState(engine.state(), new Move(0, Piece.IN_CASTLE, 0, true, false)));
        waitUntilIdle(canvas);
        assertEquals(1, sounds.count(SoundEffect.STEP));
    }

    @Test
    @DisplayName("With animations off a move still plays one step sound")
    void stepSoundWithoutAnimation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            byName(view, "animations-toggle").doClick(0);
            view.onPieceMoved(new Move(0, 0, 3, false, false));
            view.onStateChanged(engine.state());
            assertEquals(1, sounds.count(SoundEffect.STEP));
        });
    }

    @Test
    @DisplayName("The music and sound switches change the shared setting and their own look")
    void togglesSwitchSounds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AbstractButton music = byName(view, "music-toggle");
            AbstractButton effects = byName(view, "effects-toggle");
            assertTrue(music.isSelected());
            music.doClick(0);
            assertFalse(sounds.isMusicOn());
            assertFalse(music.isSelected());
            effects.doClick(0);
            assertFalse(sounds.isEffectsOn());
            byName(view, "animations-toggle").doClick(0);
            byName(view, "roll-button").doClick(0);
            assertEquals(0, sounds.count(SoundEffect.DICE_ROLL), "muted effects must stay silent");
        });
    }

    @Test
    @DisplayName("A new screen shows the setting chosen on the previous one")
    void newScreenShowsSavedSetting() throws Exception {
        sounds.setMusicOn(false);
        SwingUtilities.invokeAndWait(() -> {
            MainMenuView menu = new MainMenuView(() -> { }, () -> { }, () -> { }, sounds);
            assertFalse(byName(menu, "music-toggle").isSelected());
            byName(menu, "music-toggle").doClick(0);
            assertTrue(sounds.isMusicOn());
        });
    }

    private BoardCanvas canvas() throws Exception {
        BoardCanvas[] found = new BoardCanvas[1];
        SwingUtilities.invokeAndWait(() -> found[0] = find(view, BoardCanvas.class));
        return found[0];
    }

    private static void waitUntilIdle(BoardCanvas canvas) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        boolean[] busy = {true};
        while (busy[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            SwingUtilities.invokeAndWait(() -> busy[0] = canvas.isBusy());
        }
        assertFalse(busy[0], "animation did not finish");
    }

    private static AbstractButton byName(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && child instanceof AbstractButton button) return button;
            if (child instanceof Container container) {
                AbstractButton found = byName(container, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = find(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
