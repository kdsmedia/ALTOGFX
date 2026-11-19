### ALTOGFX — ALTOGFX Audio Effects Processor for Android

<a href='https://play.google.com/store/apps/details?id=com.altomedia.altogfx'><img width=150 alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>

ALTOGFX is a real-time guitar and voice effects processor for Android. It is a LADSPA / LV2 plugin host with a low latency native audio engine, and it ships with more than 200 high quality plugins that can be arranged in any order to build your own tone.

***

### Features

- Low latency native audio engine with floating point precision processing
- Distortion, overdrive, cabinet emulation, delay, echo, reverb, chorus, flanger, phaser, compressor, limiter, sustain, vocoder and tube emulator plugins
- Neural Amp Modeler and AIDA-X model support
- IR cabinet loader with convolution reverb
- Fully skinnable UI with multiple themes and backgrounds
- Unlimited effects in any order in the effect chain
- Customizable parameters per effect, with saveable presets
- MIDI control support
- Supports built-in mic, headset mic and USB audio interfaces

### Build

Requirements:

- JDK 21
- Android SDK with NDK and CMake 3.18.1 or newer

```
./gradlew :app:assembleRelease
```

The native audio engine is built from `app/src/main/cpp` through CMake.

### License

© ALTOMEDIA. All rights reserved. ALTOGFX is a product of ALTOMEDIA.
