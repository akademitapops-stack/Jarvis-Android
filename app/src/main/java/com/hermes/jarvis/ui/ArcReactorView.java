package com.hermes.jarvis.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ArcReactorView extends View {

    private static final int CYAN = 0xFF64FFDA;

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint seg  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF(), rf2 = new RectF();

    private ValueAnimator anim;
    private float angle = 0, pulse = 0;
    private float cx, cy, R;

    public ArcReactorView(Context c) { super(c); init(); }
    public ArcReactorView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        ring.setStyle(Paint.Style.STROKE); ring.setColor(CYAN); ring.setStrokeWidth(5f);
        seg.setStyle(Paint.Style.STROKE); seg.setColor(CYAN);
        seg.setStrokeWidth(14f); seg.setStrokeCap(Paint.Cap.ROUND);
        inner.setStyle(Paint.Style.STROKE); inner.setColor(0x9964FFDA); inner.setStrokeWidth(3f);
        core.setStyle(Paint.Style.FILL); core.setColor(CYAN);
    }

    public void start() {
        stop();
        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(5000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            angle = f * 360f;
            pulse = (float) ((Math.sin(f * Math.PI * 4) + 1) / 2);
            invalidate();
        });
        anim.start();
    }

    public void stop() {
        if (anim != null) { anim.cancel(); anim = null; }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        cx = w / 2f; cy = h / 2f;
        R = Math.min(w, h) / 2f * 0.9f;
        glow.setShader(new RadialGradient(cx, cy, R,
                Color.argb(120, 0x64, 0xFF, 0xDA),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (R <= 0) return;

        glow.setAlpha((int) (110 + 100 * pulse));
        cv.drawCircle(cx, cy, R, glow);
        cv.drawCircle(cx, cy, R * 0.92f, ring);

        float w1 = R * 0.75f;
        rf.set(cx - w1, cy - w1, cx + w1, cy + w1);
        cv.save(); cv.rotate(angle, cx, cy);
        for (int i = 0; i < 8; i++) cv.drawArc(rf, i * 45f, 20f, false, seg);
        cv.restore();

        cv.drawCircle(cx, cy, R * 0.45f, inner);

        float w2 = R * 0.28f;
        rf2.set(cx - w2, cy - w2, cx + w2, cy + w2);
        cv.save(); cv.rotate(-angle * 1.5f, cx, cy);
        for (int i = 0; i < 6; i++) cv.drawArc(rf2, i * 60f, 14f, false, seg);
        cv.restore();

        core.setAlpha((int) (160 + 90 * pulse));
        cv.drawCircle(cx, cy, R * 0.12f, core);
    }

    @Override
    protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
}
