package com.altomedia.altoefect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A stompbox drawn entirely with Canvas primitives. Knobs and the footswitch
 * are real interactive targets; the pedal body opens the large editor.
 */
public class PedalView extends View {
    public interface Listener {
        void onControlChanged(PedalView view, RuntimeControl control);

        void onPedalToggled(PedalView view, PedalState state);

        void onOpenEditor(PedalView view, PedalState state);
    }

    private static final class KnobHit {
        final RuntimeControl control;
        final float cx;
        final float cy;
        final float radius;

        KnobHit(RuntimeControl control, float cx, float cy, float radius) {
            this.control = control;
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bodyRect = new RectF();
    private final RectF footRect = new RectF();
    private final List<KnobHit> knobs = new ArrayList<>();
    private final float density;
    private final int touchSlop;

    private PedalState state;
    private Listener listener;
    private boolean large;
    private Typeface displayTypeface;
    private Typeface bodyTypeface;

    private int activeKnob = -1;
    private float touchStartX;
    private float touchStartY;
    private float startNorm;
    private boolean footPressed;
    private boolean bodyPressed;
    private boolean moved;
    private float pressOffset;

    public PedalView(Context context) {
        this(context, null);
    }

    public PedalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        displayTypeface = ResourcesCompat.getFont(context, R.font.major_mono_display);
        bodyTypeface = ResourcesCompat.getFont(context, R.font.montserrat_alternates);
        if (displayTypeface == null) {
            displayTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }
        if (bodyTypeface == null) {
            bodyTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        }
        setClickable(true);
        setFocusable(true);
    }

    public void setPedal(PedalState state) {
        this.state = state;
        if (getWidth() > 0 && getHeight() > 0) {
            layoutGeometry(getWidth(), getHeight());
        }
        refresh();
    }

    public void setLarge(boolean large) {
        this.large = large;
        requestLayout();
        refresh();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public PedalState getPedal() {
        return state;
    }

    public void refresh() {
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(large ? dp(360) : dp(158));
        int desiredHeight = Math.round(large ? dp(438) : dp(314));
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutGeometry(w, h);
    }

    private void layoutGeometry(int w, int h) {
        float margin = dp(large ? 10 : 5);
        bodyRect.set(margin, margin, w - margin, h - margin);
        knobs.clear();
        if (state == null) {
            return;
        }

        float header = large ? dp(104) : dp(76);
        float footer = large ? dp(100) : dp(86);
        float left = bodyRect.left + dp(large ? 18 : 9);
        float right = bodyRect.right - dp(large ? 18 : 9);
        float top = bodyRect.top + header;
        float bottom = bodyRect.bottom - footer;

        int count = Math.max(1, state.controls.size());
        int columns;
        if (count <= 2) {
            columns = count;
        } else if (count == 3) {
            columns = 3;
        } else if (count == 4) {
            columns = 2;
        } else {
            columns = 3;
        }
        int rows = (int) Math.ceil(count / (float) columns);
        float cellW = (right - left) / columns;
        float cellH = (bottom - top) / rows;
        float maxKnob = large ? dp(96) : dp(40);
        float knob = Math.min(maxKnob, Math.min(cellW * 0.74f, cellH * 0.66f));
        knob = Math.max(knob, dp(large ? 40 : 20));

        for (int i = 0; i < state.controls.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int lastRowCount = count % columns == 0 ? columns : count % columns;
            float rowOffset = (row == rows - 1 && lastRowCount < columns)
                    ? (columns - lastRowCount) / 2f
                    : 0f;
            float cx = left + cellW * (col + rowOffset + 0.5f);
            float cy = top + cellH * (row + (rows == 1 ? 0.5f : 0.42f));
            knobs.add(new KnobHit(state.controls.get(i), cx, cy, knob / 2f));
        }

        float footSize = large ? dp(76) : dp(58);
        float footCx = w / 2f;
        float footCy = bodyRect.bottom - footer * 0.50f;
        footRect.set(footCx - footSize / 2f, footCy - footSize / 2f,
                footCx + footSize / 2f, footCy + footSize / 2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (state == null) {
            return;
        }

        drawEnclosure(canvas);
        drawHeader(canvas);
        drawKnobs(canvas);
        drawFootswitch(canvas);
        drawJacks(canvas);
    }

    private void drawEnclosure(Canvas canvas) {
        float radius = dp(large ? 18 : 13);
        float offset = dp(4);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        canvas.drawRoundRect(bodyRect.left, bodyRect.top + offset, bodyRect.right,
                bodyRect.bottom + offset, radius, radius, paint);

        int light = lighten(state.spec.bodyColor, 0.28f);
        int dark = darken(state.spec.bodyColor, 0.45f);
        paint.setShader(new LinearGradient(bodyRect.left, bodyRect.top, bodyRect.right,
                bodyRect.bottom, new int[]{light, state.spec.bodyColor, dark},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bodyRect, radius, radius, paint);
        paint.setShader(null);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setColor(0x88FFFFFF);
        canvas.drawRoundRect(bodyRect, radius, radius, strokePaint);
        strokePaint.setColor(0x66000000);
        strokePaint.setStrokeWidth(dp(2.2f));
        RectF inner = new RectF(bodyRect);
        inner.inset(dp(2), dp(2));
        canvas.drawRoundRect(inner, radius - dp(1), radius - dp(1), strokePaint);

        // Brushed-metal edge band.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(bodyRect.left, 0, bodyRect.right, 0,
                new int[]{0x44FFFFFF, 0x11FFFFFF, 0x44FFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(bodyRect.left + dp(3), bodyRect.top + dp(3),
                bodyRect.right - dp(3), bodyRect.top + dp(6), paint);
        paint.setShader(null);

        // Screws.
        float screwInset = dp(large ? 13 : 9);
        float screwRadius = dp(large ? 4.2f : 3.1f);
        drawScrew(canvas, bodyRect.left + screwInset, bodyRect.top + screwInset, screwRadius);
        drawScrew(canvas, bodyRect.right - screwInset, bodyRect.top + screwInset, screwRadius);
        drawScrew(canvas, bodyRect.left + screwInset, bodyRect.bottom - screwInset, screwRadius);
        drawScrew(canvas, bodyRect.right - screwInset, bodyRect.bottom - screwInset, screwRadius);
    }

    private void drawScrew(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx - radius * 0.4f, cy - radius * 0.4f, radius * 1.5f,
                new int[]{0xFFE8E8E8, 0xFF8A8A8A, 0xFF3A3A3A}, new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(0.8f));
        paint.setColor(0xCC111111);
        canvas.drawLine(cx - radius * 0.6f, cy - radius * 0.6f,
                cx + radius * 0.6f, cy + radius * 0.6f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHeader(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float top = bodyRect.top + dp(large ? 20 : 13);

        smallPaint.setTypeface(bodyTypeface);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setColor(withAlpha(state.spec.accentColor, 0.72f));
        smallPaint.setTextSize(sp(large ? 10 : 7));
        smallPaint.setLetterSpacing(0.28f);
        canvas.drawText("ALTO EFECT", centerX, top, smallPaint);

        textPaint.setTypeface(displayTypeface);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFFF6EFE4);
        textPaint.setTextSize(sp(large ? 38 : 23));
        textPaint.setLetterSpacing(0.04f);
        canvas.drawText(state.spec.model, centerX, top + dp(large ? 44 : 28), textPaint);

        smallPaint.setTypeface(bodyTypeface);
        smallPaint.setColor(0xB3F6EFE4);
        smallPaint.setTextSize(sp(large ? 10 : 7));
        smallPaint.setLetterSpacing(0.16f);
        canvas.drawText(state.spec.subtitle.toUpperCase(Locale.US), centerX,
                top + dp(large ? 62 : 40), smallPaint);

        // Accent rule.
        float ruleWidth = dp(large ? 120 : 74);
        accentPaint.setStyle(Paint.Style.FILL);
        accentPaint.setShader(new LinearGradient(centerX - ruleWidth / 2f, 0,
                centerX + ruleWidth / 2f, 0,
                new int[]{0x00FFFFFF, state.spec.accentColor, 0x00FFFFFF},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(centerX - ruleWidth / 2f, top + dp(large ? 72 : 47),
                centerX + ruleWidth / 2f, top + dp(large ? 73.2f : 48.1f), accentPaint);
        accentPaint.setShader(null);
    }

    private void drawKnobs(Canvas canvas) {
        for (KnobHit hit : knobs) {
            drawKnob(canvas, hit);
        }
    }

    private void drawKnob(Canvas canvas, KnobHit hit) {
        float cx = hit.cx;
        float cy = hit.cy;
        float r = hit.radius;
        boolean live = state.enabled && state.available;
        float alpha = live ? 1f : 0.5f;

        // Tick ring on the enclosure.
        for (int i = 0; i <= 10; i++) {
            double angle = Math.toRadians(-135 + i * 27);
            float inner = r * (large ? 1.18f : 1.06f);
            float outer = r * (large ? (i % 5 == 0 ? 1.36f : 1.28f)
                    : (i % 5 == 0 ? 1.22f : 1.15f));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(i % 5 == 0 ? 1.3f : 0.8f));
            strokePaint.setColor(withAlpha(0xFFF6EFE4, (live ? 0.55f : 0.2f)));
            canvas.drawLine(cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer, strokePaint);
        }

        // Drop shadow.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55000000);
        canvas.drawCircle(cx, cy + dp(2.5f), r * 1.02f, paint);

        // Chrome bezel.
        paint.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.55f,
                new int[]{0xFFF7F7F7, 0xFFC7C7C7, 0xFF6E6E6E, 0xFF2E2E2E},
                new float[]{0f, 0.42f, 0.78f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);

        // Knurled skirt.
        paint.setColor(withAlpha(0xFF151515, alpha));
        canvas.drawCircle(cx, cy, r * 0.88f, paint);
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians(i * 9);
            float inner = r * 0.76f;
            float outer = r * 0.86f;
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(0.7f));
            strokePaint.setColor(withAlpha(0xFF9A9A9A, 0.32f * alpha));
            canvas.drawLine(cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer, strokePaint);
        }

        // Cap.
        int capLight = lighten(state.spec.accentColor, 0.18f);
        int capDark = darken(state.spec.bodyColor, 0.58f);
        paint.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.1f,
                new int[]{withAlpha(capLight, alpha), withAlpha(state.spec.bodyColor, alpha),
                        withAlpha(capDark, alpha)},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 0.66f, paint);
        paint.setShader(null);

        // Pointer.
        double pointerAngle = Math.toRadians(-135 + hit.control.getNormalized() * 270);
        float px = cx + (float) Math.cos(pointerAngle) * r * 0.56f;
        float py = cy + (float) Math.sin(pointerAngle) * r * 0.56f;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(large ? 3.2f : 2.1f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(withAlpha(0xFFF9F4E9, alpha));
        canvas.drawLine(cx, cy, px, py, strokePaint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(withAlpha(0xFFE8E8E8, alpha));
        canvas.drawCircle(cx, cy, dp(large ? 2.4f : 1.5f), paint);

        // Label and value.
        smallPaint.setTypeface(bodyTypeface);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setLetterSpacing(0.12f);
        smallPaint.setColor(withAlpha(0xFFF6EFE4, live ? 0.92f : 0.4f));
        smallPaint.setTextSize(sp(large ? 11 : 7));
        canvas.drawText(hit.control.label, cx, cy + r + dp(large ? 14 : 10), smallPaint);

        accentPaint.setTypeface(displayTypeface);
        accentPaint.setTextAlign(Paint.Align.CENTER);
        accentPaint.setLetterSpacing(0f);
        accentPaint.setColor(withAlpha(state.spec.accentColor, live ? 1f : 0.35f));
        accentPaint.setTextSize(sp(large ? 11 : 7));
        canvas.drawText(hit.control.displayValue(), cx,
                cy + r + dp(large ? 28 : 22), accentPaint);
    }

    private void drawFootswitch(Canvas canvas) {
        float cx = footRect.centerX();
        float cy = footRect.centerY() + pressOffset;
        float r = footRect.width() / 2f;
        boolean live = state.enabled && state.available;

        // LED.
        float ledY = footRect.top - dp(large ? 8 : 6);
        drawLed(canvas, cx, ledY, dp(large ? 5.5f : 3.8f), live);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        canvas.drawCircle(cx, cy + dp(3), r, paint);

        paint.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.4f,
                new int[]{0xFFF3F3F3, 0xFFBDBDBD, 0xFF616161, 0xFF262626},
                new float[]{0f, 0.44f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(cx - r * 0.22f, cy - r * 0.3f, r * 0.9f,
                new int[]{0xFF5A5A5A, 0xFF242424, 0xFF080808},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 0.72f, paint);
        paint.setShader(null);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(0x55FFFFFF);
        canvas.drawCircle(cx, cy, r * 0.72f, strokePaint);

        smallPaint.setTypeface(bodyTypeface);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setLetterSpacing(0.18f);
        smallPaint.setTextSize(sp(large ? 10 : 7));
        smallPaint.setColor(live ? state.spec.accentColor : 0x88F6EFE4);
        String footLabel = !state.available ? "N/A" : live ? "ON" : "BYPASS";
        canvas.drawText(footLabel, cx, footRect.bottom + dp(large ? 10 : 11), smallPaint);
    }

    private void drawLed(Canvas canvas, float cx, float cy, float r, boolean live) {
        if (live) {
            paint.setShader(new RadialGradient(cx, cy, r * 2.6f,
                    new int[]{withAlpha(state.spec.accentColor, 0.85f),
                            withAlpha(state.spec.accentColor, 0.28f), 0x00000000},
                    new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r * 2.6f, paint);
            paint.setShader(null);
        }
        paint.setColor(live ? state.spec.accentColor : 0xFF1B1B1B);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(live ? 0xFFFFFFFF : 0xFF3A3A3A);
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.3f, r * 0.34f, paint);
    }

    private void drawJacks(Canvas canvas) {
        float y = bodyRect.centerY();
        drawJack(canvas, bodyRect.left + dp(2), y);
        drawJack(canvas, bodyRect.right - dp(2), y);

        smallPaint.setTypeface(bodyTypeface);
        smallPaint.setTextAlign(Paint.Align.CENTER);
        smallPaint.setTextSize(sp(large ? 8 : 6));
        smallPaint.setLetterSpacing(0.1f);
        smallPaint.setColor(0x99F6EFE4);
        canvas.save();
        canvas.rotate(-90, bodyRect.left + dp(10), y - dp(24));
        canvas.drawText("IN", bodyRect.left + dp(10), y - dp(24), smallPaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(90, bodyRect.right - dp(10), y - dp(24));
        canvas.drawText("OUT", bodyRect.right - dp(10), y - dp(24), smallPaint);
        canvas.restore();
    }

    private void drawJack(Canvas canvas, float cx, float cy) {
        float r = dp(large ? 7 : 5);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, cy, r,
                new int[]{0xFFE8E8E8, 0xFF8E8E8E, 0xFF303030}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);
        paint.setColor(0xFF050505);
        canvas.drawCircle(cx, cy, r * 0.5f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                activeKnob = findKnob(x, y);
                moved = false;
                touchStartX = x;
                touchStartY = y;
                if (activeKnob >= 0) {
                    RuntimeControl control = knobs.get(activeKnob).control;
                    startNorm = control.getNormalized();
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else if (footRect.contains(x, y)) {
                    footPressed = true;
                    pressOffset = dp(1.5f);
                    invalidate();
                } else if (bodyRect.contains(x, y)) {
                    bodyPressed = true;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (activeKnob >= 0) {
                    float sensitivity = dp(large ? 300 : 210);
                    float delta = (touchStartY - y) / sensitivity;
                    if (Math.abs(delta) > 0.001f) {
                        moved = true;
                    }
                    RuntimeControl control = knobs.get(activeKnob).control;
                    control.setNormalized(startNorm + delta, true);
                    invalidate();
                    if (listener != null) {
                        listener.onControlChanged(this, control);
                    }
                    return true;
                }
                if (Math.abs(y - touchStartY) > touchSlop
                        || Math.abs(x - touchStartX) > touchSlop) {
                    moved = true;
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (activeKnob >= 0) {
                    activeKnob = -1;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    return true;
                }
                if (footPressed) {
                    boolean inside = footRect.contains(x, y);
                    footPressed = false;
                    pressOffset = 0;
                    if (inside) {
                        state.setEnabled(!state.enabled);
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        invalidate();
                        if (listener != null) {
                            listener.onPedalToggled(this, state);
                        }
                    } else {
                        invalidate();
                    }
                    return true;
                }
                if (bodyPressed) {
                    bodyPressed = false;
                    if (!moved && listener != null) {
                        listener.onOpenEditor(this, state);
                    }
                    return true;
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                activeKnob = -1;
                footPressed = false;
                bodyPressed = false;
                pressOffset = 0;
                getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();
                return true;
            }
            default:
                return super.onTouchEvent(event);
        }
    }

    private int findKnob(float x, float y) {
        for (int i = 0; i < knobs.size(); i++) {
            KnobHit hit = knobs.get(i);
            float dx = x - hit.cx;
            float dy = y - hit.cy;
            float radius = hit.radius * 1.4f;
            if (dx * dx + dy * dy <= radius * radius) {
                return i;
            }
        }
        return -1;
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int lighten(int color, float amount) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r = Math.min(255, Math.round(r + (255 - r) * amount));
        g = Math.min(255, Math.round(g + (255 - g) * amount));
        b = Math.min(255, Math.round(b + (255 - b) * amount));
        return Color.rgb(r, g, b);
    }

    private static int darken(int color, float amount) {
        int r = Math.max(0, Math.round(Color.red(color) * (1f - amount)));
        int g = Math.max(0, Math.round(Color.green(color) * (1f - amount)));
        int b = Math.max(0, Math.round(Color.blue(color) * (1f - amount)));
        return Color.rgb(r, g, b);
    }
}
