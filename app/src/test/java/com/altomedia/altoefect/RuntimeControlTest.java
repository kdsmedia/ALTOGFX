package com.altomedia.altoefect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeControlTest {
    private static RuntimeControl control(String name, String label, String unit,
                                          float defaultNorm, boolean log, float min, float max,
                                          boolean integer, String[] enums) {
        ControlSpec spec = new ControlSpec(name, label, unit, defaultNorm, log, enums);
        return new RuntimeControl(0, 0, name, spec, min, max, integer);
    }

    @Test
    public void linearDbControlMapsNormalizedPositionToRealValue() {
        RuntimeControl threshold = control("THRESHOLD", "THRESH", "dB",
                0.55f, false, -96f, 0f, false, null);
        assertEquals(-43.2f, threshold.realValue(), 0.2f);
        assertEquals("-43.2 dB", threshold.displayValue());
    }

    @Test
    public void logarithmicToneControlUsesMusicalTaper() {
        RuntimeControl tone = control("TONE", "TONE", "Hz",
                0.6f, true, 100f, 1000f, false, null);
        assertEquals(398f, tone.realValue(), 1f);
        assertEquals("398 Hz", tone.displayValue());
    }

    @Test
    public void percentControlScalesUnitRange() {
        RuntimeControl wah = control("WAH", "WAH", "%",
                0.5f, false, 0f, 1f, false, null);
        assertEquals("50%", wah.displayValue());
    }

    @Test
    public void enumControlShowsModeName() {
        RuntimeControl mode = control("Mode", "MODE", "",
                0f, false, 0f, 1f, true, new String[]{"LINEAR", "PINGPONG"});
        assertEquals("LINEAR", mode.displayValue());
        mode.setNormalized(1f, false);
        assertEquals("PINGPONG", mode.displayValue());
    }

    @Test
    public void normalizedPositionIsClamped() {
        RuntimeControl gain = control("GAIN", "GAIN", "",
                0.5f, false, 0f, 1f, false, null);
        gain.setNormalized(4f, false);
        assertEquals(1f, gain.getNormalized(), 0.0001f);
        gain.setNormalized(-2f, false);
        assertEquals(0f, gain.getNormalized(), 0.0001f);
    }

    @Test
    public void everyPedalHasCuratedControls() {
        assertEquals(9, PedalSpec.BOARD.size());
        for (PedalSpec pedal : PedalSpec.BOARD) {
            assertTrue(pedal.id, pedal.controls.length > 0);
            for (ControlSpec control : pedal.controls) {
                assertTrue(control.name, control.name != null && !control.name.isEmpty());
                assertTrue(control.label, control.label != null && !control.label.isEmpty());
            }
        }
    }
}
