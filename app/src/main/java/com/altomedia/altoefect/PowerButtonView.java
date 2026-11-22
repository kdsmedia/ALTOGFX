package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class PowerButtonView extends View {
    public interface Listener {
        void onPowerToggled(boolean on);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final float density;
    private boolean on;
    private boolean pressed;
    private Listener listener;

    public PowerButtonView(Context context) {
        this(context, null);
    }

    public PowerButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setClickable(true);
        setFocusable(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setOn(boolean on) {
        this.on = on;
        invalidate();
    }

    public boolean isOn() {
        return on;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(resolveSize(Math.round(dp(58)), widthMeasureSpec),
                resolveSize(Math.round(dp(58)), heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - dp(2);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        canvas.drawCircle(cx, cy + dp(2.5f), r, paint);

        paint.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.4f,
                new int[]{0xFFF4F4F4, 0xFFB9B9B9, 0xFF5C5C5C, 0xFF242424},
                new float[]{0f, 0.42f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(cx - r * 0.25f, cy - r * 0.3f, r * 0.95f,
                new int[]{0xFF4C4C4C, 0xFF1E1E1E, 0xFF060606},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy + (pressed ? dp(1.2f) : 0), r * 0.7f, paint);
        paint.setShader(null);

        float iconR = r * 0.42f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(2.2f));
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setColor(on ? 0xFFFFB35C : 0xFF7A7A7A);
        arc.set(cx - iconR, cy - iconR, cx + iconR, cy + iconR);
        canvas.drawArc(arc, -48, 276, false, stroke);
        canvas.drawLine(cx, cy - iconR * 1.18f, cx, cy - iconR * 0.18f, stroke);

        // LED.
        float ledR = dp(2.2f);
        float ledY = cy - r * 0.74f;
        if (on) {
            paint.setShader(new RadialGradient(cx, ledY, ledR * 4.5f,
                    new int[]{0xCCFF5A3C, 0x44FF5A3C, 0x00FF5A3C},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, ledY, ledR * 4.5f, paint);
            paint.setShader(null);
        }
        paint.setColor(on ? 0xFFFF5A3C : 0xFF2A2A2A);
        canvas.drawCircle(cx, ledY, ledR, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                pressed = false;
                on = !on;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                invalidate();
                if (listener != null) {
                    listener.onPowerToggled(on);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private float dp(float value) {
        return value * density;
    }
}
