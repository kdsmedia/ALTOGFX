package com.altomedia.altoefect;

import java.util.Locale;

public final class RuntimeControl {
    public final int pluginIndex;
    public final int controlIndex;
    public final String name;
    public final String label;
    public final String unit;
    public final float min;
    public final float max;
    public final boolean logarithmic;
    public final boolean integer;
    public final String[] enumLabels;

    private float normalized;

    public RuntimeControl(int pluginIndex, int controlIndex, String name, ControlSpec spec,
                          float min, float max, boolean integer) {
        this.pluginIndex = pluginIndex;
        this.controlIndex = controlIndex;
        this.name = name;
        this.label = spec.label;
        this.unit = spec.unit;
        this.min = min;
        this.max = max;
        this.logarithmic = spec.log;
        this.integer = integer || spec.enumLabels != null;
        this.enumLabels = spec.enumLabels;
        this.normalized = spec.defaultNorm;
    }

    public void setNormalized(float value, boolean apply) {
        normalized = Math.max(0f, Math.min(1f, value));
        if (apply) {
            AudioEngine.setPluginControl(pluginIndex, controlIndex, realValue());
        }
    }

    public float getNormalized() {
        return normalized;
    }

    public float realValue() {
        float t = normalized;
        float value;
        if (logarithmic) {
            if (min > 0f) {
                value = (float) (min * Math.pow(max / min, t));
            } else {
                value = (float) (max * Math.pow(t, 3));
            }
        } else {
            value = min + t * (max - min);
        }
        if (integer) {
            value = Math.round(value);
        }
        return Math.max(min, Math.min(max, value));
    }

    public String displayValue() {
        float value = realValue();
        if (enumLabels != null && enumLabels.length > 0) {
            int i = Math.round(value);
            if (i >= 0 && i < enumLabels.length) {
                return enumLabels[i];
            }
        }
        switch (unit) {
            case "dB":
                return String.format(Locale.US, "%.1f dB", value);
            case "ms": {
                float ms = max <= 10f ? value * 1000f : value;
                if (ms < 10f) {
                    return String.format(Locale.US, "%.1f ms", ms);
                }
                return String.format(Locale.US, "%d ms", Math.round(ms));
            }
            case "Hz":
                if (value < 10f) {
                    return String.format(Locale.US, "%.1f Hz", value);
                }
                return String.format(Locale.US, "%d Hz", Math.round(value));
            case "%": {
                float pct = max <= 1f ? value * 100f : value;
                return String.format(Locale.US, "%d%%", Math.round(pct));
            }
            case ":1":
                return String.format(Locale.US, "%.1f:1", value);
            default:
                return String.format(Locale.US, "%.1f", normalized * 10f);
        }
    }

    public String positionLabel() {
        return String.format(Locale.US, "%.1f", normalized * 10f);
    }
}
