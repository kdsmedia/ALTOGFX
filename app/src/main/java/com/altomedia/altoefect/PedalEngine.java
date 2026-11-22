package com.altomedia.altoefect;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PedalEngine {
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
        AudioEngine.create();
        AudioEngine.setMainActivityClassName("com/altomedia/altoefect/EngineBridge");
        AudioEngine.setLibraryPath(context.getApplicationInfo().nativeLibraryDir);
        AudioEngine.setLazyLoad(true);
        AudioEngine.setTunerEnabled(false);
        AudioEngine.setInputVolume(prefs.getFloat("input_gain", 1f));
        AudioEngine.setOutputVolume(prefs.getFloat("output_volume", 1f));

        for (PedalSpec spec : PedalSpec.BOARD) {
            int before = AudioEngine.getActivePlugins();
            int result = AudioEngine.addPluginLazyLV2(spec.library, spec.pluginIndex);
            int after = AudioEngine.getActivePlugins();
            boolean available = after > before && result >= 0;
            int pluginIndex = available ? after - 1 : -1;
            if (!available) {
                Log.e(TAG, "Unable to load pedal " + spec.id + " from " + spec.library);
            }
            PedalState state = new PedalState(spec, pluginIndex, available);
            pedals.add(state);
        }

        presets.clear();
        presets.addAll(PresetStore.loadAll(context));
        if (!presets.isEmpty()) {
            applyPreset(presets.get(0), false);
        } else {
            for (PedalState pedal : pedals) {
                pedal.setEnabled(false);
            }
        }
        initialized = true;
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
        AudioEngine.setSampleRate(prefs.getInt("sample_rate", 48000));
        AudioEngine.setInputVolume(prefs.getFloat("input_gain", 1f));
        AudioEngine.setOutputVolume(prefs.getFloat("output_volume", 1f));
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
        return prefs.getInt("sample_rate", 48000);
    }

    public void setSampleRate(int sampleRate) {
        prefs.edit().putInt("sample_rate", sampleRate).apply();
        restartIfRunning();
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
