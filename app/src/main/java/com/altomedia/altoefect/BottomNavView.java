package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class BottomNavView extends View {
    public interface Listener {
        void onSelected(int index);
    }

    private static final String[] LABELS = {"PEDALBOARD", "PRESETS", "LIBRARY", "SETUP"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final float density;
    private int selected;
    private Listener listener;

    public BottomNavView(Context context) {
        this(context, null);
    }

    public BottomNavView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        Typeface typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates);
        textPaint.setTypeface(typeface != null ? typeface : Typeface.SANS_SERIF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(7.4f));
        textPaint.setLetterSpacing(0.10f);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setSelected(int index) {
        if (index < 0 || index > 3) {
            return;
        }
        selected = index;
        invalidate();
    }

    public int getSelected() {
        return selected;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize((int) dp(300), widthMeasureSpec), (int) dp(66));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF0B0B0F, 0xFF050507}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setColor(0x22FFFFFF);
        canvas.drawRect(0, 0, w, dp(0.7f), paint);
        paint.setShader(new LinearGradient(0, 0, w, 0,
                new int[]{0x00FFB35C, 0x66FFB35C, 0x00FFB35C}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, dp(1.2f), paint);
        paint.setShader(null);

        float itemW = w / 4f;
        for (int i = 0; i < 4; i++) {
            float cx = itemW * (i + 0.5f);
            boolean active = i == selected;
            if (active) {
                paint.setShader(new RadialGradient(cx, h * 0.45f, itemW * 0.55f,
                        new int[]{0x33FFB35C, 0x00000000}, null, Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, h * 0.45f, itemW * 0.55f, paint);
                paint.setShader(null);
            }
            int color = active ? 0xFFFFB35C : 0xFF7C7C86;
            drawIcon(canvas, i, cx, h * 0.36f, color, active);
            textPaint.setColor(active ? 0xFFFFC982 : 0xFF7C7C86);
            canvas.drawText(LABELS[i], cx, h - dp(10), textPaint);
        }
    }

    private void drawIcon(Canvas canvas, int index, float cx, float cy, int color, boolean active) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1.6f));
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        float s = dp(11);
        if (index == 0) {
            rect.set(cx - s, cy - s * 0.9f, cx + s, cy + s * 0.9f);
            canvas.drawRoundRect(rect, dp(3), dp(3), stroke);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - s * 0.45f, cy - s * 0.3f, dp(1.8f), paint);
            canvas.drawCircle(cx + s * 0.45f, cy - s * 0.3f, dp(1.8f), paint);
            canvas.drawCircle(cx, cy + s * 0.42f, dp(2.2f), stroke);
        } else if (index == 1) {
            canvas.drawCircle(cx, cy, s * 0.82f, stroke);
            canvas.drawLine(cx, cy, cx, cy - s * 0.55f, stroke);
            for (int i = 0; i < 7; i++) {
                double angle = Math.toRadians(-135 + i * 45);
                canvas.drawLine(cx + (float) Math.cos(angle) * s * 0.95f,
                        cy + (float) Math.sin(angle) * s * 0.95f,
                        cx + (float) Math.cos(angle) * s * 1.12f,
                        cy + (float) Math.sin(angle) * s * 1.12f, stroke);
            }
        } else if (index == 2) {
            float box = s * 0.62f;
            for (int row = -1; row <= 1; row += 2) {
                for (int col = -1; col <= 1; col += 2) {
                    rect.set(cx + col * s * 0.35f - box, cy + row * s * 0.35f - box,
                            cx + col * s * 0.35f + box, cy + row * s * 0.35f + box);
                    canvas.drawRoundRect(rect, dp(1.6f), dp(1.6f), stroke);
                }
            }
        } else {
            for (int i = -1; i <= 1; i++) {
                float y = cy + i * dp(5.2f);
                canvas.drawLine(cx - s, y, cx + s, y, stroke);
                float knobX = cx + (i == -1 ? -s * 0.35f : i == 0 ? s * 0.4f : -s * 0.1f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                canvas.drawCircle(knobX, y, dp(2.1f), paint);
            }
        }

        if (active) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFB35C);
            canvas.drawCircle(cx, getHeight() - dp(3.2f), dp(1.6f), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int index = (int) (event.getX() / (getWidth() / 4f));
            if (index >= 0 && index <= 3 && index != selected) {
                selected = index;
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                invalidate();
                if (listener != null) {
                    listener.onSelected(index);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
