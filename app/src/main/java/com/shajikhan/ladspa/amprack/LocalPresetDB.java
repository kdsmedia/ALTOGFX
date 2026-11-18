package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Offline preset store. Presets and favourites are kept as plain JSON files on the device
 * instead of being synchronised with a remote database.
 */
public class LocalPresetDB {
    String TAG = getClass().getSimpleName();
    static final String LOCAL_UID = "local";

    Context context;
    MainActivity mainActivity;
    File presetsDir;
    File favoritesDir;

    LocalPresetDB(MainActivity _mainActivity) {
        mainActivity = _mainActivity;
        context = mainActivity.getApplicationContext();
        presetsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "presets");
        favoritesDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "favoritePresets");
    }

    LocalPresetDB(Context _context) {
        context = _context;
        presetsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "presets");
        favoritesDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "favoritePresets");
    }

    private File dir() {
        if (!presetsDir.exists())
            presetsDir.mkdirs();
        return presetsDir;
    }

    private String fileName(String name) {
        String clean = name.replaceAll("[^a-zA-Z0-9._-]", "_").trim();
        if (clean.isEmpty())
            clean = "preset";
        return clean + ".json";
    }

    public void savePreset(String name, String desc, boolean shared, Map values,
                           AlertDialog dialog, MyPresets myPresets, String path) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("desc", desc);
        data.put("likes", 0);
        data.put("public", shared);
        data.put("controls", values);
        data.put("uid", LOCAL_UID);
        data.put("timestamp", System.currentTimeMillis());

        String file = path != null && !path.isEmpty() ? path : fileName(name);
        data.put("path", file);

        boolean ok = writePreset(file, data);
        if (dialog != null)
            dialog.dismiss();

        if (!ok) {
            Toast.makeText(context, "Could not save preset", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(context, path != null ? "Preset updated successfully" : "Patch saved successfully",
                Toast.LENGTH_LONG).show();

        if (mainActivity != null) {
            mainActivity.lastPresetLoadedPath = file;
            mainActivity.lastPresetLoadedUID = LOCAL_UID;
        }
        if (myPresets != null && myPresets.myPresetsAdapter != null) {
            if (path != null)
                myPresets.myPresetsAdapter.removePreset(data);
            myPresets.myPresetsAdapter.addPreset(data);
        }
    }

    public void savePresets(JSONObject jsonObject, boolean shared, AlertDialog dialog, MyPresets myPresets) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject jo;
            try {
                jo = jsonObject.getJSONObject(key);
            } catch (JSONException e) {
                Log.e(TAG, "savePresets: skipping " + key, e);
                continue;
            }

            if (!jo.has("controls") || jo.isNull("controls"))
                continue;

            String name;
            try {
                name = jo.getString("name");
            } catch (JSONException e) {
                continue;
            }

            Map<String, Object> data = new Gson().fromJson(jo.toString(), HashMap.class);
            data.put("uid", LOCAL_UID);
            data.put("public", shared);
            if (!data.containsKey("likes"))
                data.put("likes", 0);
            if (!data.containsKey("timestamp"))
                data.put("timestamp", System.currentTimeMillis());
            data.put("path", fileName(name));

            if (writePreset((String) data.get("path"), data) && myPresets != null && myPresets.myPresetsAdapter != null)
                myPresets.myPresetsAdapter.addPreset(data);
        }

        if (dialog != null)
            dialog.dismiss();
        Toast.makeText(context, "Presets saved", Toast.LENGTH_LONG).show();
    }

    public void loadUserPresets(MyPresetsAdapter presetsAdapter, boolean shared, boolean quick) {
        if (presetsAdapter.progressBar != null)
            presetsAdapter.progressBar.setVisibility(View.VISIBLE);

        if (mainActivity != null && quick && mainActivity.rack != null
                && mainActivity.rack.quickPatchProgress != null)
            mainActivity.rack.quickPatchProgress.setVisibility(View.GONE);

        File[] files = quick ? favoritesDir.listFiles() : dir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".json"))
                    continue;
                Map<String, Object> preset = readPreset(file);
                if (preset == null)
                    continue;
                presetsAdapter.addPreset(preset);
            }
        }

        if (presetsAdapter.progressBar != null)
            presetsAdapter.progressBar.setVisibility(View.GONE);
    }

    void deletePreset(Map preset, ArrayList<Map> presets, MyPresetsAdapter myPresetsAdapter, int position) {
        String path = preset.get("path") == null ? null : preset.get("path").toString();
        if (path == null) {
            Log.e(TAG, "deletePreset: preset path is null");
            return;
        }

        File f = new File(dir(), path);
        if (f.exists() && !f.delete()) {
            MainActivity.toast("Preset could not be deleted");
            return;
        }

        MainActivity.toast("Preset successfully deleted");
        if (position >= 0 && position < presets.size()) {
            presets.remove(position);
            myPresetsAdapter.notifyItemRemoved(position);
        }
    }

    boolean checkAuth() {
        return true;
    }

    void getFavorites(MyPresetsAdapter presetsAdapter, boolean shared, boolean quick) {
        Map<String, Object> favorites = new HashMap<>();
        File[] files = favoritesDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json"))
                    favorites.put(file.getName(), file.getName());
            }
        }
        presetsAdapter.favoritePresets = favorites;
        loadUserPresets(presetsAdapter, shared, quick);
    }

    void addPresetToCollection(String collection, Map preset) {
        addFavorite(preset);
    }

    void likePreset(Map preset) {
    }

    void addAndLike(Map preset) {
        addFavorite(preset);
    }

    void removeAndUnlike(Map preset) {
        if (mainActivity != null)
            mainActivity.removeFavoritePreset(preset);
    }

    private void addFavorite(Map preset) {
        if (mainActivity == null)
            return;
        mainActivity.addFavoritePreset(preset);
        MainActivity.toast("Preset added to favourites");
    }

    private boolean writePreset(String file, Map<String, Object> data) {
        try (FileWriter writer = new FileWriter(new File(dir(), file))) {
            writer.write(new JSONObject(data).toString());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writePreset: unable to write " + file, e);
            return false;
        }
    }

    @Nullable
    private Map<String, Object> readPreset(@NonNull File file) {
        try (FileReader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1)
                sb.append(buffer, 0, read);

            Map<String, Object> preset = new Gson().fromJson(sb.toString(), HashMap.class);
            if (preset == null || !preset.containsKey("name"))
                return null;

            preset.put("path", file.getName());
            if (!preset.containsKey("uid"))
                preset.put("uid", LOCAL_UID);
            if (!preset.containsKey("desc") || preset.get("desc") == null)
                preset.put("desc", "");
            return preset;
        } catch (IOException e) {
            Log.e(TAG, "readPreset: unable to read " + file.getName(), e);
            return null;
        }
    }
}
