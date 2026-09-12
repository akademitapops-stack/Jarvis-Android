package com.hermes.jarvis.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;

/** Lightweight JARVIS Digital Earth HUD. No network/assets required. */
public class DigitalEarthView extends View {
    public enum State { IDLE, SEARCHING, SPEAKING, ERROR }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(42);
    private float rotation, pulse;
    private State state = State.IDLE;
    private ValueAnimator animator;
    private float[] pts;

    public DigitalEarthView(Context c) { super(c); init(); }
    public DigitalEarthView(Context c, AttributeSet a) { super(c, a); init(); }
    public DigitalEarthView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        pts = new float[180];
        for (int i=0;i<pts.length;i++) pts[i] = random.nextFloat()*2f-1f;
        line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(1f);
        start();
    }
    public void start() {
        if (animator != null) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(7000); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { rotation=((Float)a.getAnimatedValue())*360f; pulse=(float)((Math.sin(rotation*Math.PI/90)+1)/2); invalidate(); });
        animator.start();
    }
    public void stop() { if(animator!=null){animator.cancel();animator=null;} }
    public void setState(State s){ state=s==null?State.IDLE:s; invalidate(); }
    public State getState(){ return state; }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c); float cx=getWidth()/2f, cy=getHeight()/2f;
        float r=Math.min(getWidth(),getHeight())*.34f;
        int blue = state==State.ERROR?0xFFFF5C6A:0xFF2499FF;
        int cyan = state==State.ERROR?0xFFFF9AA2:0xFF6FE3FF;
        glow.setShader(new RadialGradient(cx,cy,r*1.7f,new int[]{0x6646BFFF,0x221C8FEA,0x00000000},null,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r*1.7f,glow);
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(cx-r*.18f,cy-r*.18f,r,new int[]{0xFFBEEFFF,0xFF176DC0,0xFF061A2D},new float[]{0,.28f,1},Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r*(state==State.SPEAKING?1f+0.035f*pulse:1f),p); p.setShader(null);
        line.setColor(0x884FBFFF); line.setStrokeWidth(1.1f);
        for(int i=1;i<6;i++){ float rr=r*i/6f; c.drawOval(cx-rr,cy-r*.72f,cx+rr,cy+r*.72f,line); }
        c.drawOval(cx-r,cy-r*.72f,cx+r,cy+r*.72f,line);
        for(int i=0;i<18;i++){
            double a=(i*20+rotation*.35)*Math.PI/180; float x=cx+(float)Math.cos(a)*r*1.12f; float y=cy+(float)Math.sin(a)*r*.78f;
            p.setColor(cyan); p.setAlpha(90+(int)(100*pulse)); c.drawCircle(x,y,state==State.SEARCHING?2.5f:1.7f,p);
        }
        // constellation particles around Earth
        for(int i=0;i<pts.length;i+=2){ float x=pts[i], y=pts[i+1]; float rr=(float)Math.sqrt(x*x+y*y); if(rr<.45f||rr>1.0f) continue;
            float spread=state==State.SEARCHING?1.35f:1f; float x1=cx+x*r*spread*1.45f, y1=cy+y*r*spread*1.05f;
            p.setColor(blue); p.setAlpha(65+(int)(90*pulse)); c.drawCircle(x1,y1,state==State.SEARCHING?2f:1.2f,p);
        }
        if(state==State.SEARCHING){ line.setColor(0xCC6FE3FF); line.setStrokeWidth(2f); float sweep=(rotation%360); double a=sweep*Math.PI/180; c.drawLine(cx,cy,cx+(float)Math.cos(a)*r*1.45f,cy+(float)Math.sin(a)*r*1.1f,line); }
        p.setColor(cyan); p.setAlpha(180+(int)(60*pulse)); c.drawCircle(cx,cy,r*.08f,p);
    }
    @Override protected void onDetachedFromWindow(){ stop(); super.onDetachedFromWindow(); }
}
