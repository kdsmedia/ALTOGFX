# Instalasi APK untuk QA — ALTO EFECT 1.0

APK release sudah ditandatangani dengan upload key yang sama dengan AAB. Gunakan
APK hanya untuk sideload/QA, bukan untuk unggahan production Play Store.

## Prasyarat

- Android SDK platform-tools (`adb`) tersedia.
- Ponsel Android 7.0+ dengan USB debugging aktif.
- Headset/earphone dan (opsional) adaptor gitar/audio interface.

## Perintah

```bash
cd ALTOMEDIA
ADB=/opt/android-sdk/platform-tools/adb

$ADB devices
$ADB install -r ARTIFACTS/ALTO-EFECT-1.0-release.apk
$ADB shell pm grant com.altomedia.altoefect android.permission.RECORD_AUDIO
$ADB shell am start -n com.altomedia.altoefect/.PedalboardActivity
```

Jika muncul `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, hapus versi lama terlebih
dahulu:

```bash
$ADB uninstall com.altomedia.altoefect
```

## Verifikasi paket

```bash
# Nama paket, versi, dan label
/opt/android-sdk/build-tools/35.0.0/aapt2 dump badging \
  ARTIFACTS/ALTO-EFECT-1.0-release.apk | head -5

# Izin: hanya RECORD_AUDIO + izin internal AndroidX; tidak ada INTERNET
/opt/android-sdk/build-tools/35.0.0/aapt2 dump permissions \
  ARTIFACTS/ALTO-EFECT-1.0-release.apk
```

## Catatan

- APK berukuran sekitar 241 MB; instalasi dapat memakan waktu.
- Jangan mengubah APK setelah ditandatangani; checksum ada di
  `../CHECKSUMS.sha256`.
- Untuk update Play Store, gunakan AAB dan naikkan `versionCode`.
