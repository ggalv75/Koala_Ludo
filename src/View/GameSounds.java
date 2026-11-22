package View;

/**
 * Everything the screens need from the audio system: play an effect and switch
 * background music and sound effects on or off.
 * Screens depend on this interface only, so tests and computers without a sound
 * card can use {@link #silent()} instead of the real {@link SoundManager}.
 */
public interface GameSounds {

    /** The short sounds the game plays in response to game events. */
    enum SoundEffect {
        /** The die tumbles; played when a roll starts. */
        DICE_ROLL,
        /** A pawn lands on a square; played once per square it passes. */
        STEP,
        /** The game is over and the results are shown. */
        GAME_OVER
    }

    /** Play a short effect, unless sound effects are switched off. Never throws. */
    void play(SoundEffect effect);

    /** Report whether background music is switched on. */
    boolean isMusicOn();

    /** Switch the looping background music on or off. */
    void setMusicOn(boolean on);

    /** Report whether sound effects are switched on. */
    boolean isEffectsOn();

    /** Switch dice, step and game-over sounds on or off. */
    void setEffectsOn(boolean on);

    /** Release audio resources when the application closes. */
    default void shutdown() { }

    /**
     * Null Object: remembers the switches but makes no sound. Used by tests and as a
     * safe default, so screens never have to check for a missing audio system.
     */
    static GameSounds silent() {
        return new GameSounds() {
            private boolean musicOn = true;
            private boolean effectsOn = true;

            /** Ignore the request; this implementation is silent by design. */
            @Override public void play(SoundEffect effect) { }

            @Override public boolean isMusicOn() { return musicOn; }

            @Override public void setMusicOn(boolean on) { musicOn = on; }

            @Override public boolean isEffectsOn() { return effectsOn; }

            @Override public void setEffectsOn(boolean on) { effectsOn = on; }
        };
    }
}
