package com.altomedia.altoefect;

public final class ControlSpec {
    public final String name;
    public final String label;
    public final String unit;
    public final float defaultNorm;
    public final boolean log;
    public final String[] enumLabels;

    public ControlSpec(String name, String label, String unit, float defaultNorm) {
        this(name, label, unit, defaultNorm, false, null);
    }

    public ControlSpec(String name, String label, String unit, float defaultNorm, boolean log) {
        this(name, label, unit, defaultNorm, log, null);
    }

    public ControlSpec(String name, String label, String unit, float defaultNorm, boolean log, String[] enumLabels) {
        this.name = name;
        this.label = label;
        this.unit = unit;
        this.defaultNorm = defaultNorm;
        this.log = log;
        this.enumLabels = enumLabels;
    }
}
