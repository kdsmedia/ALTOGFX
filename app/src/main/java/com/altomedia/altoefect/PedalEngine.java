package com.altomedia.altoefect;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PedalEngine implements EngineBridge.Listener {
    private static final String TAG = "PedalEngine";
    private static final String PREFS = "altoefect";
    private static PedalEngine instance;

    static {
        System.loadLibrary("amprack");
    }

    public interface Listener {
        void onMeter(float value, boolean input);

        void onSampleRate(int sampleRate, boolean lowLatency);

        void onAudioStateChanged(boolean running);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final List<PedalState> pedals = new ArrayList<>();
    private final List<Preset> presets = new ArrayList<>();
    private Listener listener;
    private Preset currentPreset;
    private boolean initialized;
    private boolean started;
    private int loadedSampleRate = 48000;

    private PedalEngine(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized PedalEngine get(Context context) {
        if (instance == null) {
            instance = new PedalEngine(context);
        }
        return instance;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        pedals.clear();
        EngineBridge.attach(context);
        EngineBridge.setListener(this);
        try {
            AudioEngine.setDefaultStreamValues(context);
        } catch (Exception ignored) {
            // Some devices do not expose the native stream properties.
        }
        if (!AudioEngine.create()) {
            throw new IllegalStateException("Audio engine could not be created");
        }
        AudioEngine.setMainActivityClassName("com/altomedia/altoefect/EngineBridge");
        AudioEngine.setLibraryPath(context.getApplicationInfo().nativeLibraryDir);
        AudioEngine.setLazyLoad(true);
        AudioEngine.setTunerEnabled(false);
        loadedSampleRate = 48000;
        AudioEngine.setSampleRate(loadedSampleRate);
        AudioEngine.setInputVolume(prefs.getFloat("input_gain", 1f));
        AudioEngine.setOutputVolume(prefs.getFloat("output_volume", 1f));
        AudioEngine.setMasterGain(prefs.getFloat("master_gain", 1.8f));
        AudioEngine.setNoiseGateEnabled(prefs.getBoolean("noise_gate", true));
        AudioEngine.setNoiseGateThreshold(prefs.getFloat("gate_threshold_db", -55f));
        AudioEngine.setHumFilterEnabled(prefs.getBoolean("hum_filter", true));

        for (PedalSpec spec : selectedSpecs()) {
            addSpec(spec);
        }

        presets.clear();
        presets.addAll(PresetStore.loadAll(context));
        if (!presets.isEmpty()) {
            Preset first = presets.get(0);
            applyPreset(first, false);
            for (PedalState pedal : pedals) {
                if (!first.pedals.containsKey(pedal.spec.id)) {
                    pedal.setEnabled(true);
                }
            }
        } else {
            for (PedalState pedal : pedals) {
                pedal.setEnabled(false);
            }
        }
        initialized = true;
    }

    private List<PedalSpec> selectedSpecs() {
        List<PedalSpec> all = PluginCatalog.get(context);
        List<PedalSpec> selected = new ArrayList<>();
        String saved = prefs.getString("board_ids", "");
        if (!saved.isEmpty()) {
            for (String id : saved.split(",")) {
                for (PedalSpec spec : all) {
                    if (spec.id.equals(id)) {
                        selected.add(spec);
                        break;
                    }
                }
            }
        }
        if (selected.isEmpty()) {
            selected.addAll(PedalSpec.BOARD);
        }
        return selected;
    }

    private void addSpec(PedalSpec spec) {
        int before = AudioEngine.getActivePlugins();
        int result = spec.lv2
                ? AudioEngine.addPluginLazyLV2(spec.library, spec.pluginIndex)
                : AudioEngine.addPluginLazy(spec.library, spec.pluginIndex);
        int after = AudioEngine.getActivePlugins();
        boolean available = after > before && result >= 0;
        int pluginIndex = available ? after - 1 : -1;
        if (!available) {
            Log.e(TAG, "Unable to load pedal " + spec.id + " from " + spec.library);
        }
        pedals.add(new PedalState(spec, pluginIndex, available));
    }

    public List<String> getSelectedIds() {
        List<String> ids = new ArrayList<>();
        if (!pedals.isEmpty()) {
            for (PedalState pedal : pedals) {
                ids.add(pedal.spec.id);
            }
            return ids;
        }
        String saved = prefs.getString("board_ids", "");
        if (!saved.isEmpty()) {
            Collections.addAll(ids, saved.split(","));
        }
        if (ids.isEmpty()) {
            for (PedalSpec spec : PedalSpec.BOARD) {
                ids.add(spec.id);
            }
        }
        return ids;
    }

    public synchronized void setBoard(List<PedalSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        initialize();
        boolean wasStarted = started;
        if (wasStarted) {
            stop();
        }
        AudioEngine.setEffectOn(false);
        int guard = 0;
        while (AudioEngine.getActivePlugins() > 0 && guard++ < 64) {
            AudioEngine.deletePlugin(AudioEngine.getActivePlugins() - 1);
        }
        pedals.clear();
        StringBuilder ids = new StringBuilder();
        for (PedalSpec spec : specs) {
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append(spec.id);
            addSpec(spec);
        }
        prefs.edit().putString("board_ids", ids.toString()).apply();
        presets.clear();
        presets.addAll(PresetStore.loadAll(context));
        if (!presets.isEmpty()) {
            Preset first = presets.get(0);
            applyPreset(first, false);
            for (PedalState pedal : pedals) {
                if (!first.pedals.containsKey(pedal.spec.id)) {
                    pedal.setEnabled(true);
                }
            }
        }
        if (wasStarted) {
            start();
        }
        if (listener != null) {
            listener.onAudioStateChanged(started);
        }
    }

    public List<PedalState> getPedals() {
        return Collections.unmodifiableList(pedals);
    }

    public PedalState getPedal(String id) {
        for (PedalState pedal : pedals) {
            if (pedal.spec.id.equals(id)) {
                return pedal;
            }
        }
        return null;
    }

    public List<Preset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }

    public void reloadPresets() {
        presets.clear();
        presets.addAll(PresetStore.loadAll(context));
    }

    public void applyPreset(Preset preset, boolean showToast) {
        if (preset == null) {
            return;
        }
        for (PedalState pedal : pedals) {
            Preset.PedalPreset entry = preset.pedals.get(pedal.spec.id);
            if (entry == null) {
                pedal.setEnabled(false);
                continue;
            }
            pedal.apply(entry.controls);
            pedal.setEnabled(entry.on);
        }
        currentPreset = preset;
        if (listener != null) {
            listener.onAudioStateChanged(started);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onMeter(float value, boolean input) {
        Listener current = listener;
        if (current != null) {
            current.onMeter(value, input);
        }
    }

    @Override
    public void onSampleRate(int sampleRate, boolean lowLatency) {
        Listener current = listener;
        if (current != null) {
            current.onSampleRate(sampleRate, lowLatency);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public boolean start() {
        initialize();
        if (started) {
            return true;
        }
        applyAudioSettings();
        boolean ok = AudioEngine.setEffectOn(true);
        if (ok) {
            AudioEngine.toggleMixer(true);
            started = true;
            if (listener != null) {
                listener.onAudioStateChanged(true);
            }
        }
        return ok;
    }

    public void stop() {
        if (!started) {
            return;
        }
        AudioEngine.toggleMixer(false);
        AudioEngine.setEffectOn(false);
        started = false;
        if (listener != null) {
            listener.onAudioStateChanged(false);
        }
    }

    private void applyAudioSettings() {
        AudioEngine.setRecordingDeviceId(prefs.getInt("input_device", -1));
        AudioEngine.setPlaybackDeviceId(prefs.getInt("output_device", -1));
        AudioEngine.setLowLatency(prefs.getBoolean("low_latency", true));
        AudioEngine.setSampleRate(getSampleRate());
        AudioEngine.setInputVolume(prefs.getFloat("input_gain", 1f));
        AudioEngine.setOutputVolume(prefs.getFloat("output_volume", 1f));
        AudioEngine.setMasterGain(prefs.getFloat("master_gain", 1.8f));
        AudioEngine.setNoiseGateEnabled(prefs.getBoolean("noise_gate", true));
        AudioEngine.setNoiseGateThreshold(prefs.getFloat("gate_threshold_db", -55f));
        AudioEngine.setHumFilterEnabled(prefs.getBoolean("hum_filter", true));
    }

    public int getInputDevice() {
        return prefs.getInt("input_device", -1);
    }

    public int getOutputDevice() {
        return prefs.getInt("output_device", -1);
    }

    public void setInputDevice(int id) {
        prefs.edit().putInt("input_device", id).apply();
        restartIfRunning();
    }

    public void setOutputDevice(int id) {
        prefs.edit().putInt("output_device", id).apply();
        restartIfRunning();
    }

    public boolean selectConnectedHeadsetIfUnset() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        if (getInputDevice() == -1) {
            int input = findHeadsetInput();
            if (input != -1) {
                editor.putInt("input_device", input);
                changed = true;
            }
        }
        if (getOutputDevice() == -1) {
            int output = findHeadsetOutput();
            if (output != -1) {
                editor.putInt("output_device", output);
                changed = true;
            }
        }
        if (changed) {
            editor.apply();
            restartIfRunning();
        }
        return changed;
    }

    public void setDevices(int inputId, int outputId) {
        SharedPreferences.Editor editor = prefs.edit();
        if (inputId != Integer.MIN_VALUE) {
            editor.putInt("input_device", inputId);
        }
        if (outputId != Integer.MIN_VALUE) {
            editor.putInt("output_device", outputId);
        }
        editor.apply();
        restartIfRunning();
    }

    public boolean isLowLatency() {
        return prefs.getBoolean("low_latency", true);
    }

    public void setLowLatency(boolean lowLatency) {
        prefs.edit().putBoolean("low_latency", lowLatency).apply();
        restartIfRunning();
    }

    public int getSampleRate() {
        return initialized ? loadedSampleRate : prefs.getInt("sample_rate", 48000);
    }

    public void setSampleRate(int sampleRate) {
        prefs.edit().putInt("sample_rate", sampleRate).apply();
    }

    public float getInputGain() {
        return prefs.getFloat("input_gain", 1f);
    }

    public void setInputGain(float gain) {
        prefs.edit().putFloat("input_gain", gain).apply();
        AudioEngine.setInputVolume(gain);
    }

    public float getOutputVolume() {
        return prefs.getFloat("output_volume", 1f);
    }

    public void setOutputVolume(float volume) {
        prefs.edit().putFloat("output_volume", volume).apply();
        AudioEngine.setOutputVolume(volume);
    }

    public float getMasterGain() {
        return prefs.getFloat("master_gain", 1.8f);
    }

    public void setMasterGain(float gain) {
        prefs.edit().putFloat("master_gain", gain).apply();
        AudioEngine.setMasterGain(gain);
    }

    public boolean isNoiseGateEnabled() {
        return prefs.getBoolean("noise_gate", true);
    }

    public void setNoiseGateEnabled(boolean enabled) {
        prefs.edit().putBoolean("noise_gate", enabled).apply();
        AudioEngine.setNoiseGateEnabled(enabled);
    }

    public float getGateThresholdDb() {
        return prefs.getFloat("gate_threshold_db", -55f);
    }

    public void setGateThresholdDb(float db) {
        prefs.edit().putFloat("gate_threshold_db", db).apply();
        AudioEngine.setNoiseGateThreshold(db);
    }

    public boolean isHumFilterEnabled() {
        return prefs.getBoolean("hum_filter", true);
    }

    public void setHumFilterEnabled(boolean enabled) {
        prefs.edit().putBoolean("hum_filter", enabled).apply();
        AudioEngine.setHumFilterEnabled(enabled);
    }

    private void restartIfRunning() {
        if (!started) {
            return;
        }
        AudioEngine.setEffectOn(false);
        applyAudioSettings();
        boolean ok = AudioEngine.setEffectOn(true);
        started = ok;
        if (ok) {
            AudioEngine.toggleMixer(true);
        }
        if (listener != null) {
            listener.onAudioStateChanged(ok);
        }
    }

    public void saveCurrentAsPreset(String name, String tag, String description) {
        Preset preset = PresetStore.fromCurrent(name, tag, description, pedals);
        PresetStore.saveUser(context, preset);
        reloadPresets();
        currentPreset = preset;
    }

    public void deleteUserPreset(String name) {
        PresetStore.deleteUser(context, name);
        reloadPresets();
    }

    public List<AudioDeviceInfo> inputDevices() {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        List<AudioDeviceInfo> result = new ArrayList<>();
        if (manager != null) {
            Collections.addAll(result, manager.getDevices(AudioManager.GET_DEVICES_INPUTS));
        }
        return result;
    }

    public List<AudioDeviceInfo> outputDevices() {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        List<AudioDeviceInfo> result = new ArrayList<>();
        if (manager != null) {
            Collections.addAll(result, manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS));
        }
        return result;
    }

    public int findHeadsetInput() {
        for (AudioDeviceInfo device : inputDevices()) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                return device.getId();
            }
        }
        return -1;
    }

    public int findHeadsetOutput() {
        for (AudioDeviceInfo device : outputDevices()) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                return device.getId();
            }
        }
        return -1;
    }

    public static String deviceTypeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "Built-in mic";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "Built-in speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "Earpiece";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "Wired headset";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "Wired headphones";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB audio";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB headset";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth A2DP";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "Line in";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "Digital in";
            default:
                return "Audio device";
        }
    }
}
