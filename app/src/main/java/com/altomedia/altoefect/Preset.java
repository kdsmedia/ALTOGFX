package com.altomedia.altoefect;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Preset {
    public static final class PedalPreset {
        public final boolean on;
        public final Map<String, Float> controls;

        public PedalPreset(boolean on, Map<String, Float> controls) {
            this.on = on;
            this.controls = controls;
        }
    }

    public final String name;
    public final String tag;
    public final String description;
    public final boolean factory;
    public final Map<String, PedalPreset> pedals;

    public Preset(String name, String tag, String description, boolean factory,
                  Map<String, PedalPreset> pedals) {
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.factory = factory;
        this.pedals = pedals;
    }

    public Preset copyAsUser() {
        return new Preset(name, tag, description, false, new LinkedHashMap<>(pedals));
    }
}
