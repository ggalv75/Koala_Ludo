package View;

import javax.sound.sampled.AudioFormat;
import java.util.Random;

/**
 * Builds every sound of the game in code, as raw PCM samples.
 * Generating the audio means the game ships no sound files: it has no external
 * paths to break (NFR-07) and no third-party recordings whose licence we would
 * need to document. All output is deterministic, so it can be unit-tested.
 */
final class SoundSynthesizer {
    /** 22.05 kHz, 16-bit, mono, signed little-endian: small and supported everywhere. */
    static final AudioFormat FORMAT = new AudioFormat(22_050f, 16, 1, true, false);
    static final int SAMPLE_RATE = (int) FORMAT.getSampleRate();

    /** The rattle lasts as long as the die animation, so sound and picture end together. */
    static final double DICE_SECONDS = DieFace.DURATION_MS / 1000.0;
    static final double STEP_SECONDS = 0.08;
    static final double GAME_OVER_SECONDS = 2.4;

    /** Loudness relative to full scale; music stays quiet so effects remain clear. */
    static final double MUSIC_PEAK = 0.28;
    static final double EFFECT_PEAK = 0.62;

    private static final double MUSIC_BPM = 104;
    private static final int MUSIC_BEATS = 32;

    private SoundSynthesizer() { }

    /** PCM bytes for one effect. */
    static byte[] effect(SoundEffect effect) {
        return switch (effect) {
            case DICE_ROLL -> toPcm(diceRoll(), EFFECT_PEAK);
            case STEP -> toPcm(step(), EFFECT_PEAK * 0.75);
            case GAME_OVER -> toPcm(gameOver(), EFFECT_PEAK);
        };
    }

    /** PCM bytes for the background music; designed to loop without a click. */
    static byte[] music() { return toPcm(musicLoop(), MUSIC_PEAK); }

    /** Length of the music loop in seconds (eight bars of 4/4). */
    static double musicSeconds() { return MUSIC_BEATS * beat(); }

    private static double beat() { return 60.0 / MUSIC_BPM; }

    // ---------------------------------------------------------------- effects

    /** Irregular plastic clicks of a tumbling die, ending with a soft landing thud. */
    private static double[] diceRoll() {
        double[] buffer = new double[samples(DICE_SECONDS)];
        Random random = new Random(7);
        double time = 0.01;
        double amplitude = 1.0;
        while (time < DICE_SECONDS - 0.12) {
            addClick(buffer, time, amplitude, random);
            time += 0.045 + random.nextDouble() * 0.05;
            amplitude = Math.max(0.45, amplitude * 0.93);
        }
        addTone(buffer, DICE_SECONDS - 0.11, 0.1, 170, 0.9, 0.002, 0.03);
        addClick(buffer, DICE_SECONDS - 0.11, 0.8, random);
        return buffer;
    }

    /** One click: a short filtered noise burst plus a bright, fast-decaying ping. */
    private static void addClick(double[] buffer, double start, double amplitude, Random random) {
        addNoise(buffer, start, 0.014, amplitude * 0.7, random.nextLong(), 0.35);
        addTone(buffer, start, 0.03, 1900 + random.nextDouble() * 900, amplitude * 0.35, 0.0005, 0.008);
    }

    /** A bubble "pop": a short sine whose pitch rises quickly, then fades. */
    private static double[] step() {
        double[] buffer = new double[samples(STEP_SECONDS)];
        double glide = 0.018;
        for (int i = 0; i < buffer.length; i++) {
            double t = i / (double) SAMPLE_RATE;
            // Integral of a pitch rising from 380 Hz to 1080 Hz, so the glide has no phase jumps.
            double phase = 2 * Math.PI * (1080 * t - 700 * glide * (1 - Math.exp(-t / glide)));
            buffer[i] += Math.sin(phase) * envelope(t, STEP_SECONDS, 0.002, 0.018);
        }
        return buffer;
    }

    /** A rising C-major arpeggio, a held chord and a short sparkle on top. */
    private static double[] gameOver() {
        double[] buffer = new double[samples(GAME_OVER_SECONDS)];
        int[] arpeggio = {67, 72, 76, 79};
        for (int i = 0; i < arpeggio.length; i++) {
            addNote(buffer, i * 0.13, 0.3, arpeggio[i], 0.7, 0.01, 0.25);
        }
        for (int note : new int[] {72, 76, 79, 84}) {
            addNote(buffer, 0.55, GAME_OVER_SECONDS - 0.6, note, note == 84 ? 0.7 : 0.45, 0.02, 0.8);
        }
        int[] sparkle = {88, 91, 96};
        for (int i = 0; i < sparkle.length; i++) {
            addNote(buffer, 1.0 + i * 0.09, 0.4, sparkle[i], 0.25, 0.003, 0.12);
        }
        return buffer;
    }

    // ------------------------------------------------------------------ music

    /** Chord roots per bar: C | Am | F | G | C | Am | Dm G | C. */
    private static final int[][] CHORDS = {
            {48, 52, 55}, {45, 48, 52}, {41, 45, 48}, {43, 47, 50},
            {48, 52, 55}, {45, 48, 52}, {50, 53, 57}, {48, 52, 55}
    };

    /** Melody as {start beat, length in beats, MIDI note}. */
    private static final double[][] MELODY = {
            {0, 1, 76}, {1, 1, 79}, {2, 2, 84},
            {4, 1, 81}, {5, 1, 79}, {6, 2, 76},
            {8, 1, 77}, {9, 1, 81}, {10, 1, 84}, {11, 1, 81},
            {12, 2, 79}, {14, 1, 74}, {15, 1, 71},
            {16, 1, 72}, {17, 1, 76}, {18, 1, 79}, {19, 1, 76},
            {20, 2, 81}, {22, 1, 79}, {23, 1, 76},
            {24, 1, 77}, {25, 1, 74}, {26, 1, 79}, {27, 1, 83},
            {28, 3, 84}
    };

    /** Bass on beats 1 and 3, a plucked arpeggio in eighth notes and a simple tune. */
    private static double[] musicLoop() {
        double beat = beat();
        double[] buffer = new double[samples(musicSeconds())];
        for (int bar = 0; bar < CHORDS.length; bar++) {
            int[] chord = CHORDS[bar];
            double barStart = bar * 4 * beat;
            // Bar 7 changes chord halfway (Dm to G), so its third beat takes the G root.
            int thirdBeatRoot = bar == 6 ? CHORDS[3][0] : chord[0];
            addNote(buffer, barStart, beat * 1.8, chord[0], 0.9, 0.01, 0.35);
            addNote(buffer, barStart + 2 * beat, beat * 1.8, thirdBeatRoot, 0.8, 0.01, 0.35);
            int[] pattern = {0, 1, 2, 1, 0, 1, 2, 1};
            int eighths = bar == CHORDS.length - 1 ? 4 : 8;
            for (int e = 0; e < eighths; e++) {
                int[] arpChord = (bar == 6 && e >= 4) ? CHORDS[3] : chord;
                addNote(buffer, barStart + e * beat / 2, beat * 0.45, arpChord[pattern[e]] + 12, 0.32, 0.004, 0.16);
            }
        }
        for (double[] note : MELODY) {
            addNote(buffer, note[0] * beat, note[1] * beat * 0.95, (int) note[2], 0.55, 0.015, 0.9);
        }
        return buffer;
    }

    // --------------------------------------------------------------- building

    /** Add a soft, slightly bright note (sine with weak overtones). */
    private static void addNote(double[] buffer, double start, double duration, int midi,
                                double amplitude, double attack, double decay) {
        double frequency = 440.0 * Math.pow(2, (midi - 69) / 12.0);
        int from = Math.max(0, (int) (start * SAMPLE_RATE));
        int to = Math.min(buffer.length, from + samples(duration));
        for (int i = from; i < to; i++) {
            double t = (i - from) / (double) SAMPLE_RATE;
            double phase = 2 * Math.PI * frequency * t;
            double wave = Math.sin(phase) + 0.3 * Math.sin(2 * phase) + 0.12 * Math.sin(3 * phase);
            buffer[i] += amplitude * wave * envelope(t, duration, attack, decay);
        }
    }

    /** Add a pure sine tone. */
    private static void addTone(double[] buffer, double start, double duration, double frequency,
                                double amplitude, double attack, double decay) {
        int from = Math.max(0, (int) (start * SAMPLE_RATE));
        int to = Math.min(buffer.length, from + samples(duration));
        for (int i = from; i < to; i++) {
            double t = (i - from) / (double) SAMPLE_RATE;
            buffer[i] += amplitude * Math.sin(2 * Math.PI * frequency * t) * envelope(t, duration, attack, decay);
        }
    }

    /** Add a burst of low-pass filtered noise (smaller alpha = darker sound). */
    private static void addNoise(double[] buffer, double start, double duration, double amplitude,
                                 long seed, double alpha) {
        Random random = new Random(seed);
        int from = Math.max(0, (int) (start * SAMPLE_RATE));
        int to = Math.min(buffer.length, from + samples(duration));
        double filtered = 0;
        for (int i = from; i < to; i++) {
            double t = (i - from) / (double) SAMPLE_RATE;
            filtered += alpha * ((random.nextDouble() * 2 - 1) - filtered);
            buffer[i] += amplitude * filtered * envelope(t, duration, 0.0005, duration / 3);
        }
    }

    /**
     * Linear attack, exponential decay, and a 10 ms fade at the very end so a
     * note never stops abruptly (an abrupt stop is heard as a click).
     */
    private static double envelope(double t, double duration, double attack, double decay) {
        double level = t < attack ? t / attack : Math.exp(-(t - attack) / decay);
        double fade = 0.01;
        double remaining = duration - t;
        if (remaining < fade) level *= Math.max(0, remaining / fade);
        return level;
    }

    private static int samples(double seconds) { return (int) Math.round(seconds * SAMPLE_RATE); }

    /** Scale to the requested peak and convert to 16-bit little-endian PCM. */
    static byte[] toPcm(double[] buffer, double peak) {
        double max = 0;
        for (double v : buffer) max = Math.max(max, Math.abs(v));
        double scale = max == 0 ? 0 : peak * Short.MAX_VALUE / max;
        byte[] pcm = new byte[buffer.length * 2];
        for (int i = 0; i < buffer.length; i++) {
            int sample = (int) Math.round(buffer[i] * scale);
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
            pcm[2 * i] = (byte) sample;
            pcm[2 * i + 1] = (byte) (sample >> 8);
        }
        return pcm;
    }
}
