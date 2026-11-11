package View;

/**
 * Null Object for {@link GameSounds}: used by tests and as a safe default,
 * so screens never have to check for a missing audio system.
 */
final class SilentSounds implements GameSounds {
    private boolean musicOn = true;
    private boolean effectsOn = true;

    /** Ignore the request; this implementation is silent by design. */
    @Override public void play(SoundEffect effect) { }

    @Override public boolean isMusicOn() { return musicOn; }

    @Override public void setMusicOn(boolean on) { musicOn = on; }

    @Override public boolean isEffectsOn() { return effectsOn; }

    @Override public void setEffectsOn(boolean on) { effectsOn = on; }
}
