package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Random;

public class StageBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bitmapRect = new RectF();
    private final Bitmap background;
    private final float[] noiseX = new float[900];
    private final float[] noiseY = new float[900];
    private final int[] noiseAlpha = new int[900];

    public StageBackgroundView(Context context) {
        this(context, null);
    }

    public StageBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Bitmap decoded = null;
        try {
            decoded = BitmapFactory.decodeResource(getResources(), R.drawable.altomedia_background);
        } catch (OutOfMemoryError error) {
            Log.w("StageBackgroundView", "Could not decode stage artwork", error);
        }
        background = decoded;
        Random random = new Random(7);
        for (int i = 0; i < noiseX.length; i++) {
            noiseX[i] = random.nextFloat();
            noiseY[i] = random.nextFloat();
            noiseAlpha[i] = 6 + random.nextInt(14);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (background != null) {
            float scale = Math.max(w / (float) background.getWidth(),
                    h / (float) background.getHeight());
            float drawWidth = background.getWidth() * scale;
            float drawHeight = background.getHeight() * scale;
            float left = (w - drawWidth) * 0.5f;
            float top = (h - drawHeight) * 0.5f;
            bitmapRect.set(left, top, left + drawWidth, top + drawHeight);
            canvas.drawBitmap(background, null, bitmapRect, bitmapPaint);
        } else {
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    new int[]{0xFF07070B, 0xFF0D0A09, 0xFF15100C, 0xFF08080A},
                    new float[]{0f, 0.38f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
        }

        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xB307070B, 0x9907070B, 0x9907070B, 0xD907070B},
                new float[]{0f, 0.34f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(new RadialGradient(w * 0.5f, h * 0.12f, w * 0.95f,
                new int[]{0x22FF9A3C, 0x0CFF6B2C, 0x00000000},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(new RadialGradient(w * 0.5f, h * 0.55f, w * 0.8f,
                new int[]{0x00000000, 0x00000000, 0x77000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setStrokeWidth(dp(1));
        for (int i = -h; i < w; i += (int) dp(46)) {
            paint.setColor(0x0AFFFFFF);
            canvas.drawLine(i, 0, i + h, h, paint);
        }

        for (int i = 0; i < noiseX.length; i++) {
            paint.setColor(Color.argb(noiseAlpha[i], 255, 240, 220));
            canvas.drawPoint(noiseX[i] * w, noiseY[i] * h, paint);
        }

        paint.setShader(new LinearGradient(0, h * 0.72f, 0, h,
                new int[]{0x00000000, 0x66000000}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.72f, w, h, paint);
        paint.setShader(null);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
