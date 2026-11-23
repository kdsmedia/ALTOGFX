#!/usr/bin/env bash
# Capture Play Store screenshots from a real Android device over ADB.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APK="${ROOT}/ARTIFACTS/ALTO-EFECT-1.0-release.apk"
ADB="${ADB:-adb}"
PKG="com.altomedia.altoefect"
ACTIVITY="${PKG}/.PedalboardActivity"
OUT="${ROOT}/PLAYSTORE/SCREENSHOTS/screenshots"

if [[ ! -f "${APK}" ]]; then
  echo "APK not found: ${APK}" >&2
  exit 1
fi

"${ADB}" wait-for-device
"${ADB}" shell input keyevent KEYCODE_WAKEUP || true
"${ADB}" shell wm dismiss-keyguard || true
"${ADB}" shell settings put system screen_off_timeout 600000 || true

echo "Installing ${APK} ..."
"${ADB}" install -r "${APK}"

echo "Granting microphone permission ..."
"${ADB}" shell pm grant "${PKG}" android.permission.RECORD_AUDIO || true

echo "Launching ${ACTIVITY} ..."
"${ADB}" shell am force-stop "${PKG}" || true
"${ADB}" shell am start -n "${ACTIVITY}"
sleep 5

mkdir -p "${OUT}"
capture() {
  "${ADB}" exec-out screencap -p > "${OUT}/$1"
  echo "Wrote ${OUT}/$1"
}

# Parse "Physical size: 1080x2400" from wm size.
SIZE="$("${ADB}" shell wm size | tr -d '\r' | awk -F': ' '/Physical size/ {print $2; exit}')"
W="${SIZE%x*}"
H="${SIZE#*x}"
if [[ -z "${W}" || -z "${H}" || "${W}" == "${H}" ]]; then
  W=1080
  H=2400
fi
NAV_Y=$((H - 90))
tap() { "${ADB}" shell input tap "$1" "$2"; sleep 2; }

capture "01-pedalboard.png"

# Bottom navigation: four equal tabs (Quick / Effects / Presets / Recordings).
tap $((W * 1 / 8)) "${NAV_Y}"
capture "02-presets.png"

tap $((W * 3 / 8)) "${NAV_Y}"
capture "03-effects.png"

tap $((W * 5 / 8)) "${NAV_Y}"
capture "04-audio-setup.png"

# Return to the pedalboard. POWER requires a manual tap because its position
# varies with device density and status bar height.
tap $((W * 7 / 8)) "${NAV_Y}"

cat <<'EOF'
Done. Inspect every PNG before uploading. For a POWER-on shot:
  1. Tap POWER in the app.
  2. Tap Continue, grant microphone, connect headphones.
  3. Tap Start anyway if the headphone warning appears.
  4. Run: adb exec-out screencap -p > screenshots/05-power-on.png
EOF
