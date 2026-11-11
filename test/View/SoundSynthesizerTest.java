package View;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The generated audio has the right length, never clips and loops without a click. */
class SoundSynthesizerTest {

    private static final double SAMPLE_RATE = SoundSynthesizer.SAMPLE_RATE;

    @Test
    @DisplayName("Every effect exists and is shorter than three seconds")
    void everyEffectHasSound() {
        for (SoundEffect effect : SoundEffect.values()) {
            byte[] pcm = SoundSynthesizer.effect(effect);
            double seconds = samples(pcm) / SAMPLE_RATE;
            assertTrue(seconds > 0.05 && seconds < 3, effect + " length: " + seconds);
            assertTrue(peak(pcm) > 0.1, effect + " is silent");
        }
    }

    @Test
    @DisplayName("The dice sound lasts exactly as long as the die animation")
    void diceSoundMatchesDieAnimation() {
        double seconds = samples(SoundSynthesizer.effect(SoundEffect.DICE_ROLL)) / SAMPLE_RATE;
        assertEquals(DieFace.DURATION_MS / 1000.0, seconds, 0.001);
    }

    @Test
    @DisplayName("Boundary: no sound reaches full scale (no clipping)")
    void noClipping() {
        assertTrue(peak(SoundSynthesizer.music()) < 0.99);
        for (SoundEffect effect : SoundEffect.values()) {
            assertTrue(peak(SoundSynthesizer.effect(effect)) < 0.99, effect + " clips");
        }
    }

    @Test
    @DisplayName("Music is quieter than the effects so they stay audible over it")
    void musicStaysInBackground() {
        double music = peak(SoundSynthesizer.music());
        for (SoundEffect effect : SoundEffect.values()) {
            assertTrue(music < peak(SoundSynthesizer.effect(effect)), effect + " is drowned by the music");
        }
    }

    @Test
    @DisplayName("The music loop starts and ends in silence, so repeating it makes no click")
    void musicLoopsSeamlessly() {
        byte[] music = SoundSynthesizer.music();
        assertEquals(0, sample(music, 0));
        assertEquals(0, sample(music, samples(music) - 1));
        assertEquals(SoundSynthesizer.musicSeconds(), samples(music) / SAMPLE_RATE, 0.001);
    }

    @Test
    @DisplayName("Generation is deterministic")
    void deterministic() {
        assertArrayEquals(toInts(SoundSynthesizer.effect(SoundEffect.DICE_ROLL)),
                toInts(SoundSynthesizer.effect(SoundEffect.DICE_ROLL)));
    }

    @Test
    @DisplayName("Without a usable sound device the manager stays silent instead of crashing")
    void managerNeverThrows() throws Exception {
        GameSounds sounds = new SoundManager();
        for (SoundEffect effect : SoundEffect.values()) sounds.play(effect);
        sounds.play(null);
        sounds.setMusicOn(false);
        sounds.setEffectsOn(false);
        sounds.play(SoundEffect.STEP);
        sounds.shutdown();
        sounds.play(SoundEffect.STEP);
        sounds.setMusicOn(true);
        assertTrue(sounds.isMusicOn());
        assertFalse(sounds.isEffectsOn());
    }

    private static int samples(byte[] pcm) { return pcm.length / 2; }

    private static int sample(byte[] pcm, int index) {
        return (short) ((pcm[2 * index] & 0xff) | (pcm[2 * index + 1] << 8));
    }

    private static double peak(byte[] pcm) {
        int max = 0;
        for (int i = 0; i < samples(pcm); i++) max = Math.max(max, Math.abs(sample(pcm, i)));
        return max / (double) Short.MAX_VALUE;
    }

    private static int[] toInts(byte[] pcm) {
        int[] values = new int[pcm.length];
        for (int i = 0; i < pcm.length; i++) values[i] = pcm[i];
        return values;
    }
}
