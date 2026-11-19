# Frequently Asked Questions

### What is ALTOGFX?
ALTOGFX is a guitar and voice effects processor for Android. You can use it to put effects on your guitar, voice or any other instrument.

### What is ALTOGFX really?
ALTOGFX is a LADSPA / LV2 plugin host for Android. It supports LADSPA SDK v1.1 and LV2 plugins. Because Android does not easily support loading dynamic modules, a large number of plugins are bundled inside the APK.

ALTOGFX takes in audio input, runs it through the loaded plugins, and sends the output to an audio device.

We use the [Oboe](https://github.com/google/oboe) framework by Google, and ALTOGFX achieves low latency through native code and careful performance tuning.

### How do I connect my guitar to my phone?
See [Connecting your guitar](connect).

### How do I put effects on my guitar?
Tap the power button to start the effects engine, then add effects to the chain from the plugin browser.

### Where are my presets saved?
Presets are saved as JSON files in the app's private storage. You can create, load and share them from the Presets tab.

### Does ALTOGFX need special hardware?
No. You can use the built-in microphone, a headset microphone, or any USB audio interface. See [Connecting your guitar](connect) for details.
