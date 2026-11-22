package com.altomedia.altoefect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PedalSpec {
    public final String id;
    public final String model;
    public final String subtitle;
    public final String library;
    public final int pluginIndex;
    public final int bodyColor;
    public final int accentColor;
    public final ControlSpec[] controls;

    public PedalSpec(String id, String model, String subtitle, String library, int pluginIndex,
                     int bodyColor, int accentColor, ControlSpec[] controls) {
        this.id = id;
        this.model = model;
        this.subtitle = subtitle;
        this.library = library;
        this.pluginIndex = pluginIndex;
        this.bodyColor = bodyColor;
        this.accentColor = accentColor;
        this.controls = controls;
    }

    public static final List<PedalSpec> BOARD;

    static {
        List<PedalSpec> pedals = new ArrayList<>();

        pedals.add(new PedalSpec(
                "wah", "WAH", "Vintage Wah",
                "libgx_colwah.lv2.so", 0,
                0xFF3A1E5C, 0xFFB98CFF,
                new ControlSpec[]{
                        new ControlSpec("WAH", "WAH", "%", 0.50f),
                        new ControlSpec("FREQ", "FREQ", "Hz", 0.45f, true),
                        new ControlSpec("WET_DRY", "MIX", "%", 0.70f)
                }));

        pedals.add(new PedalSpec(
                "comp", "COMP", "Studio Compressor",
                "libgx_compressor.lv2.so", 0,
                0xFF14524E, 0xFF5BE0C8,
                new ControlSpec[]{
                        new ControlSpec("THRESHOLD", "THRESH", "dB", 0.55f),
                        new ControlSpec("RATIO", "RATIO", ":1", 0.30f),
                        new ControlSpec("ATTACK", "ATTACK", "ms", 0.35f, true),
                        new ControlSpec("RELEASE", "RELEASE", "ms", 0.30f, true),
                        new ControlSpec("KNEE", "KNEE", "dB", 0.40f)
                }));

        pedals.add(new PedalSpec(
                "drive", "DRIVE", "Super Overdrive",
                "libGxSD1.lv2.so", 0,
                0xFF3F6B2A, 0xFFA8E063,
                new ControlSpec[]{
                        new ControlSpec("DRIVE", "DRIVE", "", 0.50f),
                        new ControlSpec("TONE", "TONE", "Hz", 0.60f, true),
                        new ControlSpec("LEVEL", "LEVEL", "dB", 0.65f)
                }));

        pedals.add(new PedalSpec(
                "dist", "DIST", "British Distortion",
                "libGxGuvnor.lv2.so", 0,
                0xFF711C1C, 0xFFFF6B5A,
                new ControlSpec[]{
                        new ControlSpec("GAIN", "GAIN", "", 0.55f),
                        new ControlSpec("BASS", "BASS", "", 0.50f),
                        new ControlSpec("MID", "MID", "", 0.55f),
                        new ControlSpec("TREBLE", "TREBLE", "", 0.55f),
                        new ControlSpec("LEVEL", "LEVEL", "", 0.50f)
                }));

        pedals.add(new PedalSpec(
                "chorus", "CHORUS", "Analog Chorus",
                "libgx_chorus.lv2.so", 0,
                0xFF173C6B, 0xFF6FB6FF,
                new ControlSpec[]{
                        new ControlSpec("Depth", "DEPTH", "%", 0.45f),
                        new ControlSpec("Freq", "RATE", "Hz", 0.30f, true),
                        new ControlSpec("Delay", "DELAY", "ms", 0.35f, true),
                        new ControlSpec("Level", "LEVEL", "%", 0.50f)
                }));

        pedals.add(new PedalSpec(
                "phaser", "PHASER", "Optical Phaser",
                "libgx_phaser.lv2.so", 0,
                0xFF8A3B0B, 0xFFFFA45B,
                new ControlSpec[]{
                        new ControlSpec("SPEED", "SPEED", "Hz", 0.35f, true),
                        new ControlSpec("Dry/Wet", "DEPTH", "%", 0.45f),
                        new ControlSpec("LEVEL", "LEVEL", "dB", 0.65f)
                }));

        pedals.add(new PedalSpec(
                "delay", "DELAY", "Stereo Echo",
                "libgx_delay.lv2.so", 0,
                0xFF14525C, 0xFF62E0E8,
                new ControlSpec[]{
                        new ControlSpec("L Time", "TIME", "ms", 0.65f, true),
                        new ControlSpec("L Level", "LEVEL", "dB", 0.35f),
                        new ControlSpec("LFO", "MOD", "Hz", 0.30f, true),
                        new ControlSpec("Mode", "MODE", "", 0.00f, false,
                                new String[]{"LINEAR", "PINGPONG"}),
                        new ControlSpec("Link (L+R)", "LINK", "", 1.00f, false,
                                new String[]{"OFF", "ON"})
                }));

        pedals.add(new PedalSpec(
                "reverb", "REVERB", "Studio Reverb",
                "libgx_reverb.lv2.so", 0,
                0xFF2D2360, 0xFFA99BFF,
                new ControlSpec[]{
                        new ControlSpec("Roomsize", "ROOM", "%", 0.45f),
                        new ControlSpec("Damp", "DAMP", "%", 0.45f),
                        new ControlSpec("Dry/Wet", "MIX", "%", 0.25f),
                        new ControlSpec("LFO", "MOD", "Hz", 0.25f, true)
                }));

        pedals.add(new PedalSpec(
                "cab", "CAB", "Cabinet Simulator",
                "libGxUltraCab.lv2.so", 0,
                0xFF453526, 0xFFE0B97A,
                new ControlSpec[]{
                        new ControlSpec("GAIN", "GAIN", "", 0.55f),
                        new ControlSpec("MIDS", "MIDS", "", 0.50f),
                        new ControlSpec("PUNCH", "PUNCH", "", 0.50f),
                        new ControlSpec("RESONANCE", "RESONANCE", "", 0.50f),
                        new ControlSpec("SIZE", "SIZE", "", 0.50f),
                        new ControlSpec("TOP", "TOP", "", 0.55f)
                }));

        BOARD = Collections.unmodifiableList(pedals);
    }

    public static PedalSpec byId(String id) {
        for (PedalSpec spec : BOARD) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
