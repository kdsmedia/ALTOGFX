package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class MeterView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float input;
    private float output;
    private long lastUpdate;

    public MeterView(Context context) {
        this(context, null);
    }

    public MeterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        Typeface typeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates);
        textPaint.setTypeface(typeface != null ? typeface : Typeface.SANS_SERIF);
        textPaint.setTextSize(sp(8));
        textPaint.setLetterSpacing(0.12f);
    }

    public void setLevels(float input, float output) {
        this.input = Math.max(this.input * 0.55f, Math.min(1f, input));
        this.output = Math.max(this.output * 0.55f, Math.min(1f, output));
        lastUpdate = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize((int) dp(150), widthMeasureSpec), (int) dp(26));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if ((input > 0.001f || output > 0.001f)
                && System.currentTimeMillis() - lastUpdate > 120) {
            input *= 0.88f;
            output *= 0.88f;
            postInvalidateOnAnimation();
        }
        float labelWidth = dp(24);
        float rowHeight = dp(7);
        float gap = dp(3);
        float top = (getHeight() - rowHeight * 2 - gap) / 2f;

        drawRow(canvas, "IN", input, top, labelWidth, rowHeight);
        drawRow(canvas, "OUT", output, top + rowHeight + gap, labelWidth, rowHeight);
    }

    private void drawRow(Canvas canvas, String label, float level, float top,
                         float labelWidth, float rowHeight) {
        textPaint.setColor(0x99F6EFE4);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(label, 0, top + rowHeight * 0.86f, textPaint);

        float left = labelWidth;
        float right = getWidth() - dp(2);
        rect.set(left, top, right, top + rowHeight);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33FFFFFF);
        canvas.drawRoundRect(rect, dp(2), dp(2), paint);

        int segments = 22;
        float segGap = dp(1.2f);
        float segWidth = (right - left - segGap * (segments - 1)) / segments;
        int lit = Math.round(level * segments);
        for (int i = 0; i < segments; i++) {
            float x = left + i * (segWidth + segGap);
            int color;
            if (i < segments * 0.62f) {
                color = 0xFF57E08A;
            } else if (i < segments * 0.85f) {
                color = 0xFFFFC44D;
            } else {
                color = 0xFFFF5A4D;
            }
            paint.setColor(i < lit ? color : (color & 0x00FFFFFF) | 0x22000000);
            canvas.drawRect(x, top, x + segWidth, top + rowHeight, paint);
        }
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
