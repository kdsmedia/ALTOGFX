package com.altomedia.altoefect;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * Static bridge between the native audio engine and the UI.
 *
 * The native engine resolves this class by name for LV2 metadata and for
 * meter/tuner callbacks, so every method referenced from C++ must stay static
 * and keep its exact signature.
 */
public final class EngineBridge {
    private static final String TAG = "EngineBridge";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Context appContext;
    private static WeakReference<Listener> listener = new WeakReference<>(null);

    public interface Listener {
        void onMeter(float value, boolean input);

        void onSampleRate(int sampleRate, boolean lowLatency);
    }

    private EngineBridge() {
    }

    public static void attach(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void setListener(Listener l) {
        listener = new WeakReference<>(l);
    }

    public static String getLV2Info(String libraryName, String plugin) {
        Context context = appContext;
        if (context == null) {
            Log.e(TAG, "getLV2Info called before attach()");
            return "{}";
        }

        String pluginName;
        if (plugin.contains("#")) {
            pluginName = plugin.substring(plugin.indexOf('#') + 1);
        } else {
            String[] parts = plugin.split("/");
            pluginName = parts[parts.length - 1];
        }

        String asset = "lv2/" + libraryName + "/" + pluginName + ".json";
        try (InputStream is = context.getAssets().open(asset);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Unable to read " + asset, e);
            return "{}";
        }
    }

    public static void setMixerMeterSwitch(float value, boolean isInput) {
        MAIN.post(() -> {
            Listener l = listener.get();
            if (l != null) {
                l.onMeter(value, isInput);
            }
        });
    }

    public static void setSampleRateDisplay(int sampleRate, boolean lowLatency) {
        MAIN.post(() -> {
            Listener l = listener.get();
            if (l != null) {
                l.onSampleRate(sampleRate, lowLatency);
            }
        });
    }

    public static void setTuner(float[] data, int size) {
        // Tuner is intentionally not exposed in ALTO EFECT.
    }

    public static void pushSamples(float[] data, int size) {
        // Recording export is not used by the pedalboard UI.
    }
}
