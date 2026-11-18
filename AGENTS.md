# AGENTS.md

Amp Rack — Android LADSPA/LV2 audio effects processor (Java + Kotlin + C++ NDK).

## Toolchain

- JDK 17 is unavailable on Debian 13; **OpenJDK 21** works with AGP 8.13 / Gradle 8.13.
- Android SDK at `/opt/android-sdk` (cmdline-tools, platform 35, build-tools 35.0.0, NDK 26.1.10909125, CMake 3.18.1).
- Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`, `ANDROID_HOME`/`ANDROID_SDK_ROOT=/opt/android-sdk`.
- `local.properties` must contain `sdk.dir=/opt/android-sdk`.

## Build notes

- `app/google-services.json` was **removed** — the app is fully offline and no longer uses the
  Google Services / Firebase plugins, so the file is no longer needed.
- Resource tasks that work without native code: `:app:mergeDebugResources`, `:app:processDebugResources`.
- `:app:assembleDebug` builds successfully (Java + resources + NDK). The NDK step previously failed
  with `lilv/lilv.h` not finding `lv2/core/lv2.h`; fixed by adding `${CMAKE_CURRENT_SOURCE_DIR}` to
  `target_include_directories(amprack ...)` in `app/src/main/cpp/CMakeLists.txt`.
- The `mybuild` script uses wrong task casing; the real task is `assembleRelease` (camelCase).

## Offline-only build

- The app is intentionally fully offline: no INTERNET/ACCESS_NETWORK_STATE permissions, no Firebase
  Auth/Firestore/Crashlytics/Analytics, no AdMob, no Play Billing, no ExoPlayer-based remote URLs.
- Presets/favourites are stored locally via `LocalPresetDB.java` (replaces the old Firestore
  `FirestoreDB`). `MyPresets`/`MyPresetsAdapter`/`Presets` use `LocalPresetDB`.
- `AndroidManifest.xml` uses `tools:node="remove"` to strip INTERNET and ACCESS_NETWORK_STATE that
  ExoPlayer (`exoplayer-core`) pulls in transitively. Keep this if ExoPlayer stays a dependency.
- Fonts `comfortaa.ttf` and `montserrat_alternates.ttf` are bundled locally (previously downloadable
  Google Fonts requiring Play Services); `font_certs.xml` was deleted.
- Removing the Firebase deps also removed `androidx.legacy:legacy-support-core-ui`, which transitively
  supplied `androidx.legacy.widget.Space`. Layouts still referencing that class (`rack.xml`,
  `activity_onboard.xml`) then failed to inflate at launch with `InflateException` / `ClassNotFoundException`.
  Use the framework `android.widget.Space` (`<Space>`), available since API 14, instead.
- Verify a build is still offline with:
  `aapt2 dump xmltree app/build/outputs/apk/debug/app-debug.apk --file AndroidManifest.xml | grep permission`

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

## App identity

- App name: `ALTOGFX` (`app_name` string). Application ID / namespace: `com.altomedia.altogfx`.
- Java/Kotlin sources live under `app/src/{main,androidTest,test}/java/com/altomedia/altogfx/`.
- JNI symbol prefix is `Java_com_altomedia_altogfx_*`; `findClass` uses `com/altomedia/altogfx/...`.
- FileProvider authority: `com.altomedia.altogfx.fileprovider`.

## Signing

- Keystore `ALTOMEDIA.jks` (alias `kdsmedia`) and `keystore.properties` are committed to the repo
  at the maintainer's request. The repository is public, so the signing key and password are public.
- `app/build.gradle` reads the `release` signing config from `keystore.properties` at the repo root
  (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without that file the release build
  produces an unsigned APK.
- Build a signed release with `:app:assembleRelease` / `:app:bundleRelease`; outputs are
  `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`.
- The APK (~240 MB) and AAB (~176 MB) exceed GitHub's 100 MB per-file git limit, so they are
  published as assets on a GitHub Release rather than committed to the tree.

## Build environment

- A fresh sandbox has **no** JDK or Android SDK; the toolchain must be reinstalled (runtime may be
  reset between sessions even mid-task).
- Install JDK + Android SDK (Debian trixie ships `openjdk-21`, not 17):
  ```
  sudo apt-get update && sudo apt-get install -y openjdk-21-jdk-headless unzip
  # Android cmdline-tools -> /opt/android-sdk/cmdline-tools/latest
  sudo -E /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
    "ndk;26.1.10909125" "cmake;3.18.1"
  ```
  `local.properties` already pins `sdk.dir=/opt/android-sdk`.
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/opt/android-sdk ./gradlew ...`.
  Gradle deps are not cached after a reset, so the first build downloads them (needs network).
- Long builds must run in the background (`nohup ... > /tmp/build.log 2>&1 &`) and be polled;
  foreground commands are capped near 1080 s.

## Offline preset store (`LocalPresetDB`)

- Replaced the removed Firestore backend. Presets are JSON files under
  `getExternalFilesDir(DOWNLOADS)/presets`; favourites under `.../favoritePresets`.
- `path` is the **file name only** (e.g. `MyPreset.json`) — it no longer looks like a Firestore
  document path (`presets/<id>`). Any code doing `path.split("/")[1]` is a leftover bug.
- All local presets share `uid = "local"`; do not gate the delete/favourite buttons on the uid.
- Favourite files must end in `.json` (the loader lists `*.json` only).

## Pushing

- The preconfigured git credential helper (`GITHUB_TOKEN`, authenticates as `kdsmedia`) is read-only
  (empty OAuth scopes) and push returns 403.
- The `PUSH_TOKEN` environment variable holds a write-capable token. Push with:
  `git -c credential.helper= push "https://x-access-token:${PUSH_TOKEN}@github.com/kdsmedia/ALTOGFX.git" HEAD:main`
