package View;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Plays the game's music and effects with Java Sound (javax.sound.sampled).
 * Facade: screens call play(...) and the two switches, and never see clips,
 * lines or audio formats.
 *
 * All audio work runs on one background thread, in the order it was requested,
 * so opening or restarting a clip never freezes the Swing UI. If the computer has
 * no usable sound device the game simply stays silent (NFR-01: no crash).
 */
public final class SoundManager implements GameSounds {
    private final ExecutorService audioThread = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "koala-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<SoundEffect, Clip> effects = new EnumMap<>(SoundEffect.class);
    private Clip music;
    private boolean available = true;
    private volatile boolean musicOn = true;
    private volatile boolean effectsOn = true;

    /** Prepare all clips in the background and start the music. */
    public SoundManager() {
        submit(this::loadClips);
        submit(this::applyMusicSwitch);
    }

    @Override
    public void play(SoundEffect effect) {
        if (effect == null || !effectsOn) return;
        submit(() -> {
            Clip clip = effects.get(effect);
            if (clip == null) return;
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        });
    }

    @Override public boolean isMusicOn() { return musicOn; }

    @Override
    public void setMusicOn(boolean on) {
        musicOn = on;
        submit(this::applyMusicSwitch);
    }

    @Override public boolean isEffectsOn() { return effectsOn; }

    @Override
    public void setEffectsOn(boolean on) {
        effectsOn = on;
        if (!on) submit(() -> effects.values().forEach(Clip::stop));
    }

    @Override
    public void shutdown() {
        submit(() -> {
            if (music != null) music.close();
            effects.values().forEach(Clip::close);
        });
        audioThread.shutdown();
    }

    /** Runs on the audio thread. */
    private void loadClips() {
        try {
            music = openClip(SoundSynthesizer.music());
            for (SoundEffect effect : SoundEffect.values()) {
                effects.put(effect, openClip(SoundSynthesizer.effect(effect)));
            }
        } catch (LineUnavailableException | RuntimeException error) {
            // No sound card, the device is busy or the format is unsupported: play without sound.
            available = false;
            System.err.println("Sound is unavailable, the game will run silently: " + error);
        }
    }

    /** Runs on the audio thread. */
    private void applyMusicSwitch() {
        if (!available || music == null) return;
        if (musicOn && !music.isActive()) {
            music.loop(Clip.LOOP_CONTINUOUSLY);
        } else if (!musicOn) {
            music.stop();
        }
    }

    private static Clip openClip(byte[] pcm) throws LineUnavailableException {
        Clip clip = AudioSystem.getClip();
        clip.open(SoundSynthesizer.FORMAT, pcm, 0, pcm.length);
        return clip;
    }

    /** Queue audio work; any failure is logged and the game continues silently. */
    private void submit(Runnable work) {
        try {
            audioThread.execute(() -> {
                try {
                    work.run();
                } catch (RuntimeException error) {
                    System.err.println("Sound error ignored: " + error);
                }
            });
        } catch (RejectedExecutionException afterShutdown) {
            // The application is closing; nothing left to play.
        }
    }
}
