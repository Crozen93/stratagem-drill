package com.crozen.stratagemdrill;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Sound effects synthesized in code (no audio files are shipped) plus haptics.
 * The sounds are rendered once to WAV in the cache folder and played through SoundPool
 * for low latency.
 */
final class Sfx {

    static final int SR = 44100;
    static final int SINE = 0, SQUARE = 1, SAW = 2, TRI = 3, NOISE = 4;
    private static final Random RNG = new Random(7);

    private final SoundPool pool;
    private final Vibrator vib;
    private final int[] keys = new int[8];
    private final int err, done, roundStart, roundEnd, tally, gameOver, lowTick, best;
    boolean soundOn = true;

    Sfx(Context ctx) {
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(aa).build();
        vib = ctx.getSystemService(Vibrator.class);

        File dir = new File(ctx.getCacheDir(), "sfx1");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        for (int i = 0; i < keys.length; i++) keys[i] = load(dir, "key" + i, key(i));
        err = load(dir, "err", error());
        done = load(dir, "done", complete());
        roundStart = load(dir, "rstart", genRoundStart());
        roundEnd = load(dir, "rend", genRoundEnd());
        tally = load(dir, "tally", tally());
        gameOver = load(dir, "over", genGameOver());
        lowTick = load(dir, "low", lowTick());
        best = load(dir, "best", newBest());
    }

    // ---- public API ----

    void arrow(int index) {
        play(keys[Math.min(index, keys.length - 1)], 0.9f);
        buzz(14, 120);
    }

    void wrong() {
        play(err, 1f);
        pattern(new long[]{0, 70, 50, 90}, new int[]{0, 255, 0, 255});
    }

    void stratagemDone() {
        play(done, 1f);
        pattern(new long[]{0, 25, 40, 45}, new int[]{0, 200, 0, 255});
    }

    void roundStart() { play(roundStart, 0.9f); }

    void roundComplete() {
        play(roundEnd, 1f);
        pattern(new long[]{0, 40, 60, 40, 60, 120}, new int[]{0, 180, 0, 180, 0, 255});
    }

    void tallyStep() { play(tally, 0.8f); }

    void lowTime() { play(lowTick, 0.7f); }

    void gameOver(boolean newBest) {
        play(newBest ? best : gameOver, 1f);
        buzz(350, 255);
    }

    void release() { pool.release(); }

    // ---- playback ----

    private void play(int id, float vol) {
        if (soundOn && id != 0) pool.play(id, vol, vol, 1, 0, 1f);
    }

    private void buzz(long ms, int amp) {
        if (vib == null || !vib.hasVibrator()) return;
        vib.vibrate(VibrationEffect.createOneShot(ms, vib.hasAmplitudeControl() ? amp : VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void pattern(long[] timings, int[] amps) {
        if (vib == null || !vib.hasVibrator()) return;
        if (vib.hasAmplitudeControl()) {
            vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1));
        } else {
            vib.vibrate(VibrationEffect.createWaveform(timings, -1));
        }
    }

    private int load(File dir, String name, short[] pcm) {
        File f = new File(dir, name + ".wav");
        try {
            writeWav(f, pcm);
            return pool.load(f.getPath(), 1);
        } catch (IOException e) {
            return 0;
        }
    }

    // ---- sound design ----

    /** Terminal blip, pitch climbs a whole tone with every arrow in the code. */
    static short[] key(int i) {
        float f = (float) (620 * Math.pow(2, i * 2 / 12.0));
        float[] b = buf(0.075f);
        tone(b, 0, 0.075f, f * 1.03f, f, SQUARE, 0.32f, 0.002f, 2.2f);
        tone(b, 0, 0.05f, f * 2, f * 2, SINE, 0.18f, 0.002f, 3f);
        tone(b, 0, 0.006f, 1, 1, NOISE, 0.35f, 0.0005f, 1f);
        lowpass(b, 6000);
        return finish(b, 1.7f);
    }

    /** Harsh two-pulse "denied" buzz. */
    static short[] error() {
        float[] b = buf(0.3f);
        for (int k = 0; k < 2; k++) {
            float s = k * 0.15f;
            tone(b, s, 0.13f, 150, 128, SAW, 0.40f, 0.003f, 0.8f);
            tone(b, s, 0.13f, 158, 134, SAW, 0.35f, 0.003f, 0.8f);
            tone(b, s, 0.13f, 75, 66, SQUARE, 0.22f, 0.003f, 0.8f);
        }
        lowpass(b, 2500);
        return finish(b, 1.3f);
    }

    /** Lock-in: two rising digital tones over a low thump. */
    static short[] complete() {
        float[] b = buf(0.24f);
        tone(b, 0, 0.18f, 95, 45, SINE, 0.75f, 0.002f, 1.5f);
        tone(b, 0, 0.015f, 1, 1, NOISE, 0.3f, 0.001f, 1f);
        tone(b, 0.0f, 0.07f, 880, 900, SQUARE, 0.22f, 0.002f, 1.2f);
        tone(b, 0.07f, 0.16f, 1320, 1340, SQUARE, 0.22f, 0.002f, 1.6f);
        tone(b, 0.07f, 0.16f, 2640, 2640, SINE, 0.08f, 0.002f, 2f);
        lowpass(b, 7000);
        return finish(b, 1f);
    }

    /** Alert: beep, beep, high beep. */
    static short[] genRoundStart() {
        float[] b = buf(0.62f);
        float[] f = {660, 660, 990};
        for (int i = 0; i < 3; i++) {
            float s = i * 0.2f;
            float d = i == 2 ? 0.22f : 0.12f;
            tone(b, s, d, f[i], f[i], SQUARE, 0.2f, 0.004f, 0.7f);
            tone(b, s, d, f[i] / 2, f[i] / 2, TRI, 0.3f, 0.004f, 0.7f);
        }
        lowpass(b, 5000);
        return finish(b, 1.6f);
    }

    /** Short victory arpeggio. */
    static short[] genRoundEnd() {
        float[] b = buf(0.75f);
        float[] f = {523.25f, 659.25f, 783.99f, 1046.5f};
        for (int i = 0; i < 4; i++) {
            float s = i * 0.09f;
            float d = i == 3 ? 0.4f : 0.11f;
            tone(b, s, d, f[i], f[i], SQUARE, 0.18f, 0.004f, 1f);
            tone(b, s, d, f[i], f[i] * 1.004f, SAW, 0.12f, 0.004f, 1f);
            tone(b, s, d, f[i] / 2, f[i] / 2, TRI, 0.25f, 0.004f, 1f);
        }
        lowpass(b, 5500);
        return finish(b, 1.7f);
    }

    static short[] tally() {
        float[] b = buf(0.06f);
        tone(b, 0, 0.05f, 1600, 1500, SINE, 0.4f, 0.001f, 2f);
        tone(b, 0, 0.005f, 1, 1, NOISE, 0.25f, 0.0005f, 1f);
        return finish(b, 1.3f);
    }

    static short[] lowTick() {
        float[] b = buf(0.05f);
        tone(b, 0, 0.045f, 1050, 1000, SQUARE, 0.2f, 0.001f, 2f);
        lowpass(b, 4000);
        return finish(b, 2.2f);
    }

    /** Falling three-note "mission failed". */
    static short[] genGameOver() {
        float[] b = buf(1.1f);
        float[] f = {392, 311.1f, 233.1f};
        for (int i = 0; i < 3; i++) {
            float s = i * 0.26f;
            float d = i == 2 ? 0.6f : 0.24f;
            tone(b, s, d, f[i], f[i] * (i == 2 ? 0.94f : 1f), SAW, 0.25f, 0.005f, 0.9f);
            tone(b, s, d, f[i] / 2, f[i] / 2, SQUARE, 0.18f, 0.005f, 0.9f);
        }
        lowpass(b, 2200);
        return finish(b, 1.6f);
    }

    static short[] newBest() {
        float[] b = buf(0.9f);
        float[] f = {523.25f, 783.99f, 1046.5f, 1318.5f};
        for (int i = 0; i < 4; i++) {
            float s = i * 0.11f;
            float d = i == 3 ? 0.5f : 0.13f;
            tone(b, s, d, f[i], f[i], SQUARE, 0.18f, 0.004f, 1f);
            tone(b, s, d, f[i] * 1.5f, f[i] * 1.5f, SINE, 0.08f, 0.004f, 1.2f);
            tone(b, s, d, f[i] / 2, f[i] / 2, TRI, 0.25f, 0.004f, 1f);
        }
        lowpass(b, 6000);
        return finish(b, 1.7f);
    }

    // ---- synthesis helpers ----

    private static float[] buf(float sec) {
        return new float[(int) (sec * SR)];
    }

    /** Adds a tone with an exponential pitch sweep f0→f1 and an attack/decay envelope. */
    static void tone(float[] b, float start, float dur, float f0, float f1, int wave,
                     float vol, float attack, float curve) {
        int s0 = (int) (start * SR);
        int n = (int) (dur * SR);
        double phase = 0;
        double ratio = f1 / (double) f0;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float t = i / (float) SR;
            float k = i / (float) n;
            double f = f0 * Math.pow(ratio, k);
            phase += f / SR;
            phase -= Math.floor(phase);
            float v;
            switch (wave) {
                case SINE: v = (float) Math.sin(2 * Math.PI * phase); break;
                case SQUARE: v = phase < 0.5 ? 0.8f : -0.8f; break;
                case SAW: v = (float) (2 * phase - 1); break;
                case TRI: v = (float) (4 * Math.abs(phase - 0.5) - 1); break;
                default: v = RNG.nextFloat() * 2 - 1; break;
            }
            float env = Math.min(1f, t / attack) * (float) Math.pow(1 - k, curve);
            b[s0 + i] += v * env * vol;
        }
    }

    static void lowpass(float[] b, float cutoff) {
        float rc = 1f / (2f * (float) Math.PI * cutoff);
        float dt = 1f / SR;
        float a = dt / (rc + dt);
        float y = 0;
        for (int i = 0; i < b.length; i++) {
            y += a * (b[i] - y);
            b[i] = y;
        }
    }

    static short[] finish(float[] b, float gain) {
        short[] out = new short[b.length];
        int fade = Math.min(b.length, SR / 500);
        for (int i = 0; i < b.length; i++) {
            float v = (float) Math.tanh(b[i] * gain * 1.2f) * 0.92f;
            if (i >= b.length - fade) v *= (b.length - i) / (float) fade;
            out[i] = (short) (v * 32767);
        }
        return out;
    }

    static void writeWav(File f, short[] pcm) throws IOException {
        int dataLen = pcm.length * 2;
        ByteBuffer bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        bb.put(new byte[]{'R', 'I', 'F', 'F'}).putInt(36 + dataLen)
                .put(new byte[]{'W', 'A', 'V', 'E'})
                .put(new byte[]{'f', 'm', 't', ' '}).putInt(16)
                .putShort((short) 1).putShort((short) 1)
                .putInt(SR).putInt(SR * 2)
                .putShort((short) 2).putShort((short) 16)
                .put(new byte[]{'d', 'a', 't', 'a'}).putInt(dataLen);
        for (short s : pcm) bb.putShort(s);
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(bb.array());
        }
    }
}
