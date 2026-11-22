package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LogoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pick = new Path();
    private final Path bolt = new Path();

    public LogoView(Context context) {
        this(context, null);
    }

    public LogoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(resolveSize(Math.round(dp(44)), widthMeasureSpec),
                resolveSize(Math.round(dp(44)), heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - dp(1);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.4f,
                new int[]{0xFF2B2118, 0xFF100D0B, 0xFF030303},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.2f));
        paint.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r,
                new int[]{0xFFFFC46B, 0xFFFF6B2C, 0xFFB23A12}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r - dp(2.2f), paint);
        paint.setShader(null);

        float pr = r * 0.54f;
        pick.reset();
        pick.moveTo(cx, cy - pr);
        pick.cubicTo(cx + pr * 0.95f, cy - pr * 0.65f, cx + pr * 0.8f, cy + pr * 0.55f, cx, cy + pr);
        pick.cubicTo(cx - pr * 0.8f, cy + pr * 0.55f, cx - pr * 0.95f, cy - pr * 0.65f, cx, cy - pr);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx, cy - pr, cx, cy + pr,
                new int[]{0xFFFFF4E2, 0xFFE8C9A2, 0xFFC79A68}, null, Shader.TileMode.CLAMP));
        canvas.drawPath(pick, paint);
        paint.setShader(null);

        float br = pr * 0.62f;
        bolt.reset();
        bolt.moveTo(cx + br * 0.15f, cy - br);
        bolt.lineTo(cx - br * 0.55f, cy + br * 0.08f);
        bolt.lineTo(cx - br * 0.05f, cy + br * 0.08f);
        bolt.lineTo(cx - br * 0.2f, cy + br);
        bolt.lineTo(cx + br * 0.58f, cy - br * 0.18f);
        bolt.lineTo(cx + br * 0.06f, cy - br * 0.18f);
        bolt.close();
        paint.setColor(0xFF1A0E06);
        canvas.drawPath(bolt, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(0.8f));
        paint.setColor(0x66FFFFFF);
        canvas.drawCircle(cx, cy, r - dp(0.8f), paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
