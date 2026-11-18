# AGENTS.md

Amp Rack — Android LADSPA/LV2 audio effects processor (Java + Kotlin + C++ NDK).

## Toolchain

- JDK 17 is unavailable on Debian 13; **OpenJDK 21** works with AGP 8.13 / Gradle 8.13.
- Android SDK at `/opt/android-sdk` (cmdline-tools, platform 35, build-tools 35.0.0, NDK 26.1.10909125, CMake 3.18.1).
- Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`, `ANDROID_HOME`/`ANDROID_SDK_ROOT=/opt/android-sdk`.
- `local.properties` must contain `sdk.dir=/opt/android-sdk`.

## Build notes

- `app/google-services.json` is gitignored and required by the Google Services plugin. A dummy file
  (valid JSON, matching `applicationId` `com.shajikhan.ladspa.amprack`) lets resource/Java tasks run
  for validation; delete it before committing.
- Resource tasks that work without native code: `:app:mergeDebugResources`, `:app:processDebugResources`.
- `:app:assembleDebug` currently fails in the NDK step: `lilv/lilv.h` cannot find `lv2/core/lv2.h`
  (pre-existing include-path issue in `app/src/main/cpp/CMakeLists.txt`, unrelated to app resources).
- The `mybuild` script uses wrong task casing; the real task is `assembleRelease` (camelCase).

## Icons / branding

- Source art: `Logo.png` at repo root (1254x1254) — a circular emblem on a black background.
- Launcher icons live in `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}`:
  `ic_launcher.png` (legacy square), `ic_launcher_round.png` (legacy round),
  `ic_launcher_foreground.png` (adaptive foreground).
- Adaptive icons: `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`, background color in
  `app/src/main/res/values/ic_launcher_background.xml`.
- Keep the emblem inside the **66dp safe zone** of the 108dp foreground canvas so launcher masks
  never crop it. Preserve the 1:1 aspect ratio (never stretch).
- `app/src/main/res/drawable/logo.png` is the in-app logo (notifications, `rack.xml`, onboarding,
  purchase, bug-report layouts).
- `app/src/main/assets/themes/*/logo.png` are per-theme amp artwork — **not** the app logo; do not replace.

## Pushing

- The preconfigured git credential helper authenticates as `kdsmedia` but is read-only (empty OAuth
  scopes) and push returns 403. Use a `repo`-scoped token via
  `git -c credential.helper= push "https://x-access-token:<token>@github.com/kdsmedia/ALTOGFX.git" HEAD:main`.
