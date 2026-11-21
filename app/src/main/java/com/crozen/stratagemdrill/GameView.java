package com.crozen.stratagemdrill;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** Draws the game on a Canvas and turns swipes into arrows. Coordinates are in units of 1/100 of the screen. */
@SuppressLint("ViewConstructor")
final class GameView extends View implements Game.Events {

    private static final int YELLOW = Stratagem.YELLOW;
    private static final int WHITE = 0xFFEDEDED;
    private static final int GREY = 0xFF8A8A8A;
    private static final int DARK = 0xFF2A2A2A;
    private static final int ALERT = 0xFFFF3B30;

    private final Game game;
    private final Sfx sfx;
    private final SharedPreferences prefs;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fm = new Paint.FontMetrics();
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final Typeface bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Typeface regular = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private RadialGradient vignette;

    private Canvas c;
    private float u, ox, oy, size;
    private long lastNs;
    private boolean running;
    private long clock;

    private float downX, downY;
    private boolean swiped;

    GameView(Context ctx) {
        super(ctx);
        prefs = ctx.getSharedPreferences("drill", Context.MODE_PRIVATE);
        sfx = new Sfx(ctx);
        sfx.soundOn = prefs.getBoolean("sound", true);
        game = new Game(this, prefs.getInt("best", 0));
        setKeepScreenOn(true);
    }

    void resume() {
        running = true;
        lastNs = 0;
        postInvalidateOnAnimation();
    }

    void pause() {
        running = false;
    }

    void release() {
        sfx.release();
    }

    /** Long-press on the title screen toggles sound. */
    void toggleSound() {
        sfx.soundOn = !sfx.soundOn;
        prefs.edit().putBoolean("sound", sfx.soundOn).apply();
    }

    // ---- Game.Events ----

    @Override public void onArrowOk(int index) { sfx.arrow(index); }
    @Override public void onArrowWrong() { sfx.wrong(); }
    @Override public void onStratagemDone() { sfx.stratagemDone(); }
    @Override public void onRoundStart() { sfx.roundStart(); }
    @Override public void onRoundComplete() { sfx.roundComplete(); }
    @Override public void onTallyStep() { sfx.tallyStep(); }
    @Override public void onLowTimeTick() { sfx.lowTime(); }

    @Override
    public void onGameOver(boolean newBest) {
        if (newBest) prefs.edit().putInt("best", game.best).apply();
        sfx.gameOver(newBest);
    }

    // ---- input ----

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                swiped = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!swiped) {
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    float th = size * 0.08f;
                    if (dx * dx + dy * dy > th * th) {
                        swiped = true;
                        int dir = Math.abs(dx) > Math.abs(dy)
                                ? (dx > 0 ? Stratagem.R : Stratagem.L)
                                : (dy > 0 ? Stratagem.D : Stratagem.U);
                        game.input(dir);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!swiped) {
                    if (game.state == Game.State.TITLE && e.getEventTime() - e.getDownTime() > 600) {
                        toggleSound();
                    } else {
                        game.tap();
                    }
                }
                return true;
            default:
                return true;
        }
    }

    // ---- frame loop ----

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        size = Math.min(w, h);
        u = size / 100f;
        ox = (w - size) / 2f;
        oy = (h - size) / 2f;
        vignette = new RadialGradient(w / 2f, h / 2f, size * 0.62f,
                new int[]{0xFF1C1A10, 0xFF0B0A06, 0xFF000000}, new float[]{0f, 0.65f, 1f},
                Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.nanoTime();
        long dt = lastNs == 0 ? 16 : Math.min(50, (now - lastNs) / 1_000_000L);
        lastNs = now;
        clock += dt;
        game.update(dt);

        c = canvas;
        drawBackground();
        switch (game.state) {
            case TITLE: drawTitle(); break;
            case ROUND_INTRO: drawIntro(); break;
            case PLAYING: drawPlaying(); break;
            case ROUND_END: drawRoundEnd(); break;
            case GAME_OVER: drawGameOver(); break;
        }
        drawScanlines();
        c = null;

        if (running) postInvalidateOnAnimation();
    }

    // ---- screens ----

    private void drawBackground() {
        p.setShader(vignette);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, getWidth(), getHeight(), p);
        p.setShader(null);
    }

    private void drawScanlines() {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x16000000);
        float step = Math.max(3f, u * 0.9f);
        for (float y = 0; y < getHeight(); y += step) {
            c.drawRect(0, y, getWidth(), y + step / 2f, p);
        }
    }

    private void drawTitle() {
        float shift = clock / 60f;
        hazard(16, 22, shift);
        hazard(85, 91, -shift);

        text("STRATAGEM", 50, 35, 12.5f, YELLOW, Paint.Align.CENTER, 0.06f, true);
        text("D R I L L", 50, 45, 5.5f, WHITE, Paint.Align.CENTER, 0.1f, true);

        Stratagem demo = Stratagem.ALL[(int) (clock / 3200) % Stratagem.ALL.length];
        int lit = (int) Math.min(demo.code.length, (clock % 3200) / 260);
        drawArrowRow(demo.code, lit, 57, 7.5f, false);

        text("РЕКОРД  " + game.best, 50, 68, 4.6f, GREY, Paint.Align.CENTER, 0.08f, true);
        if ((clock / 600) % 2 == 0) {
            text("СВАЙП — НАЧАТЬ", 50, 77, 4.8f, YELLOW, Paint.Align.CENTER, 0.08f, true);
        }
        text(sfx.soundOn ? "ЗВУК ВКЛ" : "ЗВУК ВЫКЛ", 50, 96, 3.2f, GREY, Paint.Align.CENTER, 0.08f, false);
    }

    private void drawIntro() {
        long t = game.stateTime;
        float alpha = t < 300 ? ((t / 40) % 2 == 0 ? 0.35f : 1f) : 1f;
        text("ПРИГОТОВЬСЯ", 50, 36, 5.5f, withAlpha(WHITE, alpha), Paint.Align.CENTER, 0.15f, true);
        text("РАУНД " + game.round, 50, 51, 13f, withAlpha(YELLOW, alpha), Paint.Align.CENTER, 0.05f, true);

        float k = Math.min(1f, t / (float) Game.INTRO_MS);
        bar(26, 61, 74, 63, k, YELLOW);
        text(game.queue.size() + " СТРАТАГЕМ", 50, 71, 4f, GREY, Paint.Align.CENTER, 0.1f, true);
        hazard(80, 84, clock / 40f);
    }

    private void drawPlaying() {
        Stratagem s = game.current();
        if (s == null) return;

        boolean err = game.errorFlashMs > 0;
        float shake = err ? (float) Math.sin(game.errorFlashMs * 0.12) * 1.6f * (game.errorFlashMs / 320f) : 0f;

        text("РАУНД " + game.round, 50, 12.5f, 4.2f, GREY, Paint.Align.CENTER, 0.12f, true);
        text(String.valueOf(game.score), 50, 21, 8f, YELLOW, Paint.Align.CENTER, 0.04f, true);

        // progress through the round
        text((game.queueIndex + 1) + "/" + game.queue.size(), 24, 40, 4.4f, GREY, Paint.Align.CENTER, 0.02f, true);

        // current stratagem
        c.save();
        c.translate(shake * u, 0);
        float pulse = game.completeFlashMs > 0 ? 1f + 0.08f * (game.completeFlashMs / 260f) : 1f;
        float half = 11f * pulse;
        iconBox(s, 50 - half, 40 - half, 50 + half, 40 + half, err ? ALERT : YELLOW, 1f);
        c.restore();

        // upcoming
        float x = 64;
        for (int i = game.queueIndex + 1; i < game.queue.size() && i <= game.queueIndex + 2; i++) {
            iconBox(game.queue.get(i), x, 34.5f, x + 11, 45.5f, DARK, i == game.queueIndex + 1 ? 0.75f : 0.4f);
            x += 14;
        }

        // name plate
        p.setStyle(Paint.Style.FILL);
        p.setColor(err ? ALERT : YELLOW);
        rect(16, 54, 84, 61.5f);
        c.drawRect(rf, p);
        float nameSize = fitSize(s.name, 4.6f, 64, true, 0.06f);
        text(s.name, 50, 57.9f, nameSize, 0xFF000000, Paint.Align.CENTER, 0.06f, true);

        // arrows
        c.save();
        c.translate(shake * u, 0);
        drawArrowRow(s.code, game.inputPos, 70.5f, 9f, err);
        c.restore();

        // timer
        float f = game.timeFraction();
        boolean low = f < 0.25f;
        int col = low ? ((clock / 150) % 2 == 0 ? ALERT : 0xFF8F1D17) : YELLOW;
        bar(22, 81, 78, 84.5f, f, col);
    }

    private void drawRoundEnd() {
        text("РАУНД " + game.round + " ПРОЙДЕН", 50, 24, 6f, YELLOW, Paint.Align.CENTER, 0.06f, true);
        hazard(29, 31.5f, clock / 40f);

        String[] labels = {"Бонус раунда", "Бонус времени", "Без ошибок"};
        int[] values = {game.bonusRound, game.bonusTime, game.bonusPerfect};
        for (int i = 0; i < 3; i++) {
            if (game.tallyStep <= i) break;
            float y = 40 + i * 9;
            text(labels[i], 17, y, 4.6f, WHITE, Paint.Align.LEFT, 0.02f, false);
            String v = (i == 2 && values[i] == 0) ? "—" : "+" + values[i];
            text(v, 83, y, 5f, YELLOW, Paint.Align.RIGHT, 0.02f, true);
        }
        text("СЧЁТ", 50, 70, 4f, GREY, Paint.Align.CENTER, 0.15f, true);
        text(String.valueOf(game.score), 50, 79, 9f, YELLOW, Paint.Align.CENTER, 0.04f, true);
    }

    private void drawGameOver() {
        long t = game.stateTime;
        float flick = t < 400 && (t / 50) % 2 == 0 ? 0.3f : 1f;
        hazard(17, 21, clock / 40f);
        text("ВРЕМЯ ВЫШЛО", 50, 31, 7.5f, withAlpha(ALERT, flick), Paint.Align.CENTER, 0.08f, true);
        text("РАУНД " + game.round, 50, 40, 4.2f, GREY, Paint.Align.CENTER, 0.12f, true);
        text(String.valueOf(game.score), 50, 54, 15f, YELLOW, Paint.Align.CENTER, 0.03f, true);
        if (game.newBest) {
            if ((clock / 350) % 2 == 0) text("НОВЫЙ РЕКОРД!", 50, 67, 5.2f, YELLOW, Paint.Align.CENTER, 0.08f, true);
        } else {
            text("РЕКОРД  " + game.best, 50, 67, 4.6f, GREY, Paint.Align.CENTER, 0.08f, true);
        }
        if (t >= Game.GAME_OVER_LOCK_MS && (clock / 600) % 2 == 0) {
            text("СВАЙП — ЕЩЁ РАЗ", 50, 78, 4.8f, WHITE, Paint.Align.CENTER, 0.08f, true);
        }
        hazard(85, 89, -clock / 40f);
    }

    // ---- widgets ----

    private void drawArrowRow(int[] code, int done, float cy, float maxSize, boolean err) {
        int n = code.length;
        float gap = 1.6f;
        float avail = 74f;
        float s = Math.min(maxSize, (avail - (n - 1) * gap) / n);
        float total = n * s + (n - 1) * gap;
        float x = 50 - total / 2 + s / 2;
        for (int i = 0; i < n; i++) {
            int col = err ? ALERT : (i < done ? YELLOW : 0xFF9E9E9E);
            arrow(x, cy, s, code[i], col);
            x += s + gap;
        }
    }

    private void arrow(float cx, float cy, float s, int dir, int color) {
        float deg;
        switch (dir) {
            case Stratagem.R: deg = 90; break;
            case Stratagem.D: deg = 180; break;
            case Stratagem.L: deg = 270; break;
            default: deg = 0; break;
        }
        float X = ox + cx * u, Y = oy + cy * u, k = s * u;
        c.save();
        c.rotate(deg, X, Y);
        path.reset();
        path.moveTo(X, Y - 0.5f * k);
        path.lineTo(X + 0.47f * k, Y - 0.02f * k);
        path.lineTo(X + 0.17f * k, Y - 0.02f * k);
        path.lineTo(X + 0.17f * k, Y + 0.48f * k);
        path.lineTo(X - 0.17f * k, Y + 0.48f * k);
        path.lineTo(X - 0.17f * k, Y - 0.02f * k);
        path.lineTo(X - 0.47f * k, Y - 0.02f * k);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(path, p);
        c.restore();
    }

    private void iconBox(Stratagem s, float l, float t, float r, float b, int border, float alpha) {
        rect(l, t, r, b);
        p.setStyle(Paint.Style.FILL);
        p.setColor(withAlpha(0xFF121212, alpha));
        c.drawRect(rf, p);
        p.setColor(withAlpha(s.color, 0.14f * alpha));
        c.drawRect(rf, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.7f * u);
        p.setColor(withAlpha(border, alpha));
        c.drawRect(rf, p);
        Icons.draw(c, p, path, rf, s.kind, ox + (l + r) / 2 * u, oy + (t + b) / 2 * u, (r - l) * u,
                withAlpha(s.color, alpha));
    }

    private void bar(float l, float t, float r, float b, float frac, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(DARK);
        rect(l, t, r, b);
        c.drawRect(rf, p);
        p.setColor(color);
        rect(l, t, l + (r - l) * Math.max(0, Math.min(1, frac)), b);
        c.drawRect(rf, p);
    }

    /** Yellow/black warning stripes, scrolling with {@code shift}. */
    private void hazard(float top, float bottom, float shift) {
        float h = bottom - top;
        float w = h * 1.2f;
        c.save();
        rect(0, top, 100, bottom);
        c.clipRect(rf);
        p.setStyle(Paint.Style.FILL);
        p.setColor(YELLOW);
        c.drawRect(rf, p);
        p.setColor(0xFF000000);
        float start = -4 * w + (shift % (2 * w));
        for (float x = start; x < 100 + 2 * w; x += 2 * w) {
            path.reset();
            path.moveTo(ox + x * u, oy + top * u);
            path.lineTo(ox + (x + w) * u, oy + top * u);
            path.lineTo(ox + (x + w - h) * u, oy + bottom * u);
            path.lineTo(ox + (x - h) * u, oy + bottom * u);
            path.close();
            c.drawPath(path, p);
        }
        c.restore();
    }

    // ---- text and geometry helpers ----

    private void text(String s, float x, float cy, float sizeU, int color, Paint.Align align,
                      float spacing, boolean isBold) {
        tp.setTypeface(isBold ? bold : regular);
        tp.setTextSize(sizeU * u);
        tp.setLetterSpacing(spacing);
        tp.setTextAlign(align);
        tp.setColor(color);
        tp.getFontMetrics(fm);
        float baseline = oy + cy * u - (fm.ascent + fm.descent) / 2f;
        c.drawText(s, ox + x * u, baseline, tp);
    }

    private float fitSize(String s, float sizeU, float maxWidthU, boolean isBold, float spacing) {
        tp.setTypeface(isBold ? bold : regular);
        tp.setTextSize(sizeU * u);
        tp.setLetterSpacing(spacing);
        float w = tp.measureText(s) / u;
        return w > maxWidthU ? sizeU * maxWidthU / w : sizeU;
    }

    private void rect(float l, float t, float r, float b) {
        rf.set(ox + l * u, oy + t * u, ox + r * u, oy + b * u);
    }

    static int withAlpha(int color, float a) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
