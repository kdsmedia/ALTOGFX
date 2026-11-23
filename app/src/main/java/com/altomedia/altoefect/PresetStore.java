package com.altomedia.altoefect;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PresetStore {
    private static final String PREFS = "altoefect";
    private static final String KEY_USER_PRESETS = "user_presets";

    private PresetStore() {
    }

    public static List<Preset> loadAll(Context context) {
        List<Preset> presets = new ArrayList<>(loadFactory(context));
        presets.addAll(loadUser(context));
        return presets;
    }

    public static List<Preset> loadFactory(Context context) {
        List<Preset> presets = new ArrayList<>();
        try (InputStream is = context.getAssets().open("altoefect_presets.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONArray array = root.getJSONArray("presets");
            for (int i = 0; i < array.length(); i++) {
                Preset preset = parsePreset(array.getJSONObject(i), true);
                if (preset != null) {
                    presets.add(preset);
                }
            }
        } catch (Exception ignored) {
        }
        return presets;
    }

    public static List<Preset> loadUser(Context context) {
        List<Preset> presets = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_USER_PRESETS, null);
        if (json == null) {
            return presets;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                Preset preset = parsePreset(array.getJSONObject(i), false);
                if (preset != null) {
                    presets.add(preset);
                }
            }
        } catch (Exception ignored) {
        }
        return presets;
    }

    public static void saveUser(Context context, Preset preset) {
        List<Preset> user = loadUser(context);
        user.removeIf(p -> p.name.equalsIgnoreCase(preset.name));
        user.add(0, preset.copyAsUser());
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_PRESETS, serialize(user)).apply();
    }

    public static void deleteUser(Context context, String name) {
        List<Preset> user = loadUser(context);
        user.removeIf(p -> p.name.equalsIgnoreCase(name));
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_PRESETS, serialize(user)).apply();
    }

    public static Preset fromCurrent(String name, String tag, String description, List<PedalState> pedals) {
        Map<String, Preset.PedalPreset> map = new LinkedHashMap<>();
        for (PedalState pedal : pedals) {
            map.put(pedal.spec.id, new Preset.PedalPreset(pedal.enabled, pedal.snapshot()));
        }
        return new Preset(name, tag, description, false, map);
    }

    private static Preset parsePreset(JSONObject object, boolean factory) {
        try {
            String name = object.getString("name");
            String tag = object.optString("tag", factory ? "FACTORY" : "USER");
            String description = object.optString("description", "");
            JSONObject pedalObject = object.getJSONObject("pedals");
            Map<String, Preset.PedalPreset> pedals = new LinkedHashMap<>();
            for (java.util.Iterator<String> it = pedalObject.keys(); it.hasNext(); ) {
                String id = it.next();
                JSONObject entry = pedalObject.getJSONObject(id);
                boolean on = entry.optBoolean("on", false);
                Map<String, Float> controls = new LinkedHashMap<>();
                JSONObject controlsObject = entry.optJSONObject("controls");
                if (controlsObject != null) {
                    for (java.util.Iterator<String> cit = controlsObject.keys(); cit.hasNext(); ) {
                        String key = cit.next();
                        controls.put(key, (float) controlsObject.getDouble(key));
                    }
                }
                pedals.put(id, new Preset.PedalPreset(on, controls));
            }
            return new Preset(name, tag, description, factory, pedals);
        } catch (Exception e) {
            return null;
        }
    }

    private static String serialize(List<Preset> presets) {
        try {
            JSONArray array = new JSONArray();
            for (Preset preset : presets) {
                JSONObject object = new JSONObject();
                object.put("name", preset.name);
                object.put("tag", preset.tag);
                object.put("description", preset.description);
                JSONObject pedalObject = new JSONObject();
                for (Map.Entry<String, Preset.PedalPreset> entry : preset.pedals.entrySet()) {
                    JSONObject pedal = new JSONObject();
                    pedal.put("on", entry.getValue().on);
                    JSONObject controls = new JSONObject();
                    for (Map.Entry<String, Float> control : entry.getValue().controls.entrySet()) {
                        controls.put(control.getKey(), control.getValue());
                    }
                    pedal.put("controls", controls);
                    pedalObject.put(entry.getKey(), pedal);
                }
                object.put("pedals", pedalObject);
                array.put(object);
            }
            return array.toString();
        } catch (Exception e) {
            return "[]";
        }
    }
}
