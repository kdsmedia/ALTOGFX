package com.altomedia.altoefect;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Catalogue of the effects packaged with ALTO EFECT. The nine curated pedals are
 * always present; the remaining entries come from the generated
 * {@code plugin_catalog.json} asset.
 */
public final class PluginCatalog {
    private static List<PedalSpec> catalog;

    private PluginCatalog() {
    }

    public static synchronized List<PedalSpec> get(Context context) {
        if (catalog == null) {
            catalog = load(context);
        }
        return catalog;
    }

    public static synchronized PedalSpec byId(Context context, String id) {
        for (PedalSpec spec : get(context)) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }

    private static List<PedalSpec> load(Context context) {
        List<PedalSpec> result = new ArrayList<>(PedalSpec.BOARD);
        Set<String> ids = new HashSet<>();
        for (PedalSpec spec : result) {
            ids.add(spec.id);
        }

        try (InputStream is = context.getAssets().open("plugin_catalog.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONArray array = root.getJSONArray("pedals");
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String id = item.optString("id", "");
                String library = item.optString("library", "");
                if (id.isEmpty() || library.isEmpty() || ids.contains(id)) {
                    continue;
                }
                String name = item.optString("name", library);
                String model = item.optString("model", name);
                if (model.length() > 10) {
                    model = model.substring(0, 10);
                }
                int body = (int) item.optLong("body", 0xFF33333D);
                int accent = (int) item.optLong("accent", 0xFFBFC4D8);
                int plugin = item.optInt("plugin", 0);
                boolean lv2 = item.optBoolean("lv2", library.endsWith(".lv2.so"));
                ids.add(id);
                result.add(new PedalSpec(id, model.toUpperCase(Locale.US), name, library, plugin,
                        body, accent, new ControlSpec[0], lv2));
            }
        } catch (Exception ignored) {
        }
        return Collections.unmodifiableList(result);
    }
}
