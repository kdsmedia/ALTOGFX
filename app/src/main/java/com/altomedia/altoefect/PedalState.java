package com.altomedia.altoefect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PedalState {
    public final PedalSpec spec;
    public final int pluginIndex;
    public final List<RuntimeControl> controls = new ArrayList<>();
    public boolean enabled;
    public boolean available;

    public PedalState(PedalSpec spec, int pluginIndex, boolean available) {
        this.spec = spec;
        this.pluginIndex = pluginIndex;
        this.available = available;
        this.enabled = available;
        if (available) {
            bindControls();
        }
    }

    private void bindControls() {
        int count = AudioEngine.getPluginControls(pluginIndex);
        List<RuntimeControl> curated = new ArrayList<>();
        for (ControlSpec controlSpec : spec.controls) {
            for (int i = 0; i < count; i++) {
                String name = AudioEngine.getControlName(pluginIndex, i);
                if (name == null || !name.equalsIgnoreCase(controlSpec.name)) {
                    continue;
                }
                float[] values = AudioEngine.getPluginControlValues(pluginIndex, i);
                if (values == null || values.length < 4) {
                    continue;
                }
                int type = Math.round(values[3]);
                if (type != 0 && type != 1) {
                    continue;
                }
                RuntimeControl control = new RuntimeControl(pluginIndex, i, name, controlSpec,
                        values[1], values[2], type == 1);
                control.setNormalized(controlSpec.defaultNorm, true);
                curated.add(control);
                break;
            }
        }

        if (curated.size() < 2) {
            curated.clear();
            for (int i = 0; i < count; i++) {
                float[] values = AudioEngine.getPluginControlValues(pluginIndex, i);
                if (values == null || values.length < 4) {
                    continue;
                }
                int type = Math.round(values[3]);
                String name = AudioEngine.getControlName(pluginIndex, i);
                if (type != 0 && type != 1) {
                    continue;
                }
                if (name == null || name.toLowerCase().contains("bypass")) {
                    continue;
                }
                ControlSpec fallback = new ControlSpec(name, name.toUpperCase(), "", 0.5f);
                curated.add(new RuntimeControl(pluginIndex, i, name, fallback,
                        values[1], values[2], type == 1));
            }
        }

        controls.addAll(curated);
        for (RuntimeControl control : controls) {
            control.setNormalized(control.getNormalized(), true);
        }
    }

    public RuntimeControl control(String name) {
        for (RuntimeControl control : controls) {
            if (control.name.equalsIgnoreCase(name)) {
                return control;
            }
        }
        return null;
    }

    public void setControl(String name, float normalized) {
        RuntimeControl control = control(name);
        if (control != null) {
            control.setNormalized(normalized, true);
        }
    }

    public Map<String, Float> snapshot() {
        Map<String, Float> values = new HashMap<>();
        for (RuntimeControl control : controls) {
            values.put(control.name, control.getNormalized());
        }
        return values;
    }

    public void apply(Map<String, Float> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            setControl(entry.getKey(), entry.getValue());
        }
    }

    public void setEnabled(boolean on) {
        enabled = on && available;
        if (available) {
            AudioEngine.togglePlugin(pluginIndex, enabled);
        }
    }

    public void resetToDefaults() {
        for (RuntimeControl control : controls) {
            for (ControlSpec spec : this.spec.controls) {
                if (spec.name.equalsIgnoreCase(control.name)) {
                    control.setNormalized(spec.defaultNorm, true);
                    break;
                }
            }
        }
    }
}
