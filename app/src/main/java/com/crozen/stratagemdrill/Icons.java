package com.crozen.stratagemdrill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Simple original glyphs drawn with Canvas, one per stratagem kind. */
final class Icons {

    private Icons() {}

    /** @param cx,cy centre in pixels, @param s box size in pixels. */
    static void draw(Canvas c, Paint p, Path path, RectF rf, Stratagem.Kind kind,
                     float cx, float cy, float s, int color) {
        p.setColor(color);
        p.setStrokeWidth(s * 0.07f);
        p.setStrokeCap(Paint.Cap.SQUARE);
        switch (kind) {
            case STRIKE: {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx, cy, s * 0.24f, p);
                c.drawLine(cx - s * 0.38f, cy, cx - s * 0.1f, cy, p);
                c.drawLine(cx + s * 0.1f, cy, cx + s * 0.38f, cy, p);
                c.drawLine(cx, cy - s * 0.38f, cx, cy - s * 0.1f, p);
                c.drawLine(cx, cy + s * 0.1f, cx, cy + s * 0.38f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, s * 0.05f, p);
                break;
            }
            case LASER: {
                p.setStyle(Paint.Style.FILL);
                c.drawRect(cx - s * 0.05f, cy - s * 0.4f, cx + s * 0.05f, cy + s * 0.18f, p);
                rf.set(cx - s * 0.3f, cy + s * 0.14f, cx + s * 0.3f, cy + s * 0.32f);
                p.setStyle(Paint.Style.STROKE);
                c.drawOval(rf, p);
                c.drawLine(cx - s * 0.18f, cy - s * 0.05f, cx - s * 0.32f, cy - s * 0.15f, p);
                c.drawLine(cx + s * 0.18f, cy - s * 0.05f, cx + s * 0.32f, cy - s * 0.15f, p);
                break;
            }
            case BOMB: {
                p.setStyle(Paint.Style.FILL);
                rf.set(cx - s * 0.13f, cy - s * 0.2f, cx + s * 0.13f, cy + s * 0.34f);
                c.drawOval(rf, p);
                path.reset();
                path.moveTo(cx - s * 0.2f, cy - s * 0.38f);
                path.lineTo(cx + s * 0.2f, cy - s * 0.38f);
                path.lineTo(cx + s * 0.07f, cy - s * 0.14f);
                path.lineTo(cx - s * 0.07f, cy - s * 0.14f);
                path.close();
                c.drawPath(path, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.045f);
                c.drawLine(cx - s * 0.3f, cy - s * 0.1f, cx - s * 0.3f, cy + s * 0.15f, p);
                c.drawLine(cx + s * 0.3f, cy - s * 0.1f, cx + s * 0.3f, cy + s * 0.15f, p);
                break;
            }
            case SMOKE: {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx - s * 0.14f, cy + s * 0.08f, s * 0.15f, p);
                c.drawCircle(cx + s * 0.14f, cy + s * 0.08f, s * 0.15f, p);
                c.drawCircle(cx, cy - s * 0.12f, s * 0.17f, p);
                break;
            }
            case SUPPLY: {
                p.setStyle(Paint.Style.STROKE);
                rf.set(cx - s * 0.28f, cy - s * 0.22f, cx + s * 0.28f, cy + s * 0.3f);
                c.drawRect(rf, p);
                c.drawLine(rf.left, rf.top, rf.right, rf.bottom, p);
                c.drawLine(rf.right, rf.top, rf.left, rf.bottom, p);
                c.drawLine(rf.left, cy - s * 0.36f, rf.right, cy - s * 0.36f, p);
                break;
            }
            case WEAPON: {
                p.setStyle(Paint.Style.FILL);
                c.drawRect(cx - s * 0.38f, cy - s * 0.1f, cx + s * 0.2f, cy + s * 0.02f, p);
                c.drawRect(cx + s * 0.2f, cy - s * 0.07f, cx + s * 0.4f, cy - s * 0.01f, p);
                c.drawRect(cx - s * 0.12f, cy + s * 0.02f, cx - s * 0.02f, cy + s * 0.24f, p);
                path.reset();
                path.moveTo(cx - s * 0.38f, cy - s * 0.1f);
                path.lineTo(cx - s * 0.38f, cy + s * 0.2f);
                path.lineTo(cx - s * 0.26f, cy + s * 0.2f);
                path.lineTo(cx - s * 0.22f, cy + s * 0.02f);
                path.close();
                c.drawPath(path, p);
                break;
            }
            case SHIELD: {
                p.setStyle(Paint.Style.STROKE);
                rf.set(cx - s * 0.32f, cy - s * 0.26f, cx + s * 0.32f, cy + s * 0.38f);
                c.drawArc(rf, 180, 180, false, p);
                c.drawLine(cx - s * 0.38f, cy + s * 0.06f, cx + s * 0.38f, cy + s * 0.06f, p);
                p.setStrokeWidth(s * 0.04f);
                rf.set(cx - s * 0.18f, cy - s * 0.1f, cx + s * 0.18f, cy + s * 0.22f);
                c.drawArc(rf, 180, 180, false, p);
                break;
            }
            case TURRET: {
                p.setStyle(Paint.Style.FILL);
                c.drawRect(cx - s * 0.15f, cy - s * 0.2f, cx + s * 0.1f, cy + s * 0.02f, p);
                c.drawRect(cx + s * 0.1f, cy - s * 0.14f, cx + s * 0.38f, cy - s * 0.07f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx - s * 0.03f, cy + s * 0.02f, cx - s * 0.28f, cy + s * 0.36f, p);
                c.drawLine(cx - s * 0.03f, cy + s * 0.02f, cx + s * 0.22f, cy + s * 0.36f, p);
                c.drawLine(cx - s * 0.03f, cy + s * 0.02f, cx - s * 0.03f, cy + s * 0.36f, p);
                break;
            }
            case MINES: {
                p.setStyle(Paint.Style.STROKE);
                path.reset();
                path.moveTo(cx, cy - s * 0.34f);
                path.lineTo(cx + s * 0.36f, cy + s * 0.3f);
                path.lineTo(cx - s * 0.36f, cy + s * 0.3f);
                path.close();
                c.drawPath(path, p);
                c.drawLine(cx, cy - s * 0.1f, cx, cy + s * 0.09f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy + s * 0.19f, s * 0.04f, p);
                break;
            }
            case BEACON: {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx, cy - s * 0.08f, cx, cy + s * 0.36f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy - s * 0.12f, s * 0.07f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.05f);
                for (int i = 1; i <= 2; i++) {
                    float r = s * (0.12f + 0.12f * i);
                    rf.set(cx - r, cy - s * 0.12f - r, cx + r, cy - s * 0.12f + r);
                    c.drawArc(rf, 145, 70, false, p);
                    c.drawArc(rf, -35, 70, false, p);
                }
                break;
            }
            case REINFORCE: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.09f);
                for (int i = 0; i < 2; i++) {
                    float y = cy - s * 0.02f + i * s * 0.24f;
                    path.reset();
                    path.moveTo(cx - s * 0.3f, y + s * 0.1f);
                    path.lineTo(cx, y - s * 0.18f);
                    path.lineTo(cx + s * 0.3f, y + s * 0.1f);
                    c.drawPath(path, p);
                }
                break;
            }
            case NUKE: {
                p.setStyle(Paint.Style.FILL);
                float r = s * 0.36f;
                rf.set(cx - r, cy - r, cx + r, cy + r);
                for (int i = 0; i < 3; i++) c.drawArc(rf, -120 + i * 120 - 30, 60, true, p);
                p.setColor(0xFF121212);
                c.drawCircle(cx, cy, s * 0.11f, p);
                p.setColor(color);
                c.drawCircle(cx, cy, s * 0.06f, p);
                break;
            }
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }
}
