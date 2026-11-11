package View;

/**
 * Everything the screens need from the audio system: play an effect and switch
 * background music and sound effects on or off.
 * Screens depend on this interface only, so tests and computers without a sound
 * card can use {@link #silent()} instead of the real {@link SoundManager}.
 */
public interface GameSounds {
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

    /** Null Object: remembers the switches but makes no sound. */
    static GameSounds silent() { return new SilentSounds(); }
}
