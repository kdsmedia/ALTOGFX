# Screenshot Play Store — ALTO EFECT 1.1

## Status

Belum ada file screenshot di folder ini. Sandbox build tidak memiliki
perangkat Android dan tidak mendukung emulator x86_64 (KVM tidak tersedia),
jadi screenshot **harus diambil dari aplikasi asli di ponsel/tablet nyata**.
Jangan memakai mockup, render, atau screenshot aplikasi lain.

## Kebutuhan Play Store

- Minimal **2**, maksimal **8** screenshot ponsel.
- Format PNG atau JPEG.
- Sisi terpendek minimal 320 px; sisi terpanjang maksimal 3840 px.
- Rasio 16:9 atau 9:16 (portrait aplikasi ini).
- Tidak boleh berisi informasi pribadi, notifikasi, atau UI sistem yang
  mengganggu.
- Screenshot tablet opsional; aplikasi berorientasi portrait.

## Cara otomatis (ADB)

```bash
cd ALTOMEDIA/PLAYSTORE/SCREENSHOTS
chmod +x capture-screenshots.sh
./capture-screenshots.sh
```

Script akan memasang APK, memberikan izin mikrofon, membuka aplikasi, dan
mengambil beberapa screenshot ke folder `screenshots/`. Periksa hasilnya secara
manual; koordinat tombol dapat berbeda antarperangkat.

## Cara manual

1. Pasang APK release di ponsel.
2. Buka aplikasi, tunggu pedalboard tampil.
3. Ambil screenshot pedalboard.
4. Buka tab **Presets**, ambil screenshot daftar preset.
5. Buka tab **Effects**/library, ambil screenshot pilihan efek.
6. Buka tab **Audio Setup**, ambil screenshot perangkat input/output.
7. Nyalakan POWER, sambungkan headset, dan ambil screenshot saat pedal aktif
   (opsional, paling meyakinkan).
8. Pindahkan file ke folder ini dengan nama `01-pedalboard.png`,
   `02-presets.png`, `03-effects.png`, `04-audio-setup.png`.

## Rekomendasi urutan

1. Pedalboard dengan beberapa pedal aktif.
2. Preset pabrik (menunjukkan 8 preset).
3. Efek/pedal individual dengan potensiometer.
4. Audio Setup (input/output headset).
5. Layar POWER aktif dengan meter.

## Catatan

- Gunakan headset saat mengambil screenshot audio agar tidak ada feedback.
- Jangan menampilkan notifikasi pribadi; aktifkan mode jangan ganggu.
- Jangan mengedit isi UI; crop hanya bagian status bar jika perlu.
