# Smoke test — ALTO EFECT 1.1

Jalankan di perangkat Android nyata. Tandai **PASS/FAIL** dan catat model
perangkat, versi Android, serta headset/adaptor yang dipakai.

| # | Uji | Hasil yang diharapkan | PASS/FAIL |
|---|---|---|---|
| 1 | Instal APK/AAB internal testing | Terpasang, label **ALTO EFECT** | |
| 2 | Buka aplikasi | Pedalboard tampil tanpa crash | |
| 3 | Tekan POWER tanpa izin mikrofon | Dialog **Microphone access** muncul lebih dulu | |
| 4 | Tekan **Not now** | POWER kembali mati; UI tetap bisa dipakai | |
| 5 | Tekan POWER → Continue → izinkan mikrofon | Mesin audio menyala; status/sample rate tampil | |
| 6 | Sambungkan headset | Tidak ada feedback; output terdengar di headset | |
| 7 | Pilih 8 preset pabrik | Setiap preset mengubah suara/pedal | |
| 8 | Aktifkan/nonaktifkan tiap pedal | Tombol on/off dan lampu status bekerja | |
| 9 | Putar potensiometer | Parameter berubah dan suara ikut berubah | |
| 10 | Simpan preset baru, muat ulang | Preset tersimpan dan dapat dimuat | |
| 11 | Hapus preset buatan sendiri | Preset hilang; preset pabrik tetap ada | |
| 12 | Buka Audio Setup | Daftar input/output tampil; pilihan tersimpan | |
| 13 | Background/foreground aplikasi | Audio/status kembali dengan benar | |
| 14 | Cabut headset saat berjalan | Audio berpindah ke speaker; tidak crash | |
| 15 | Periksa izin aplikasi | Hanya mikrofon; tidak ada INTERNET | |
| 16 | Matikan layar saat audio berjalan | Perilaku sesuai kebijakan audio Android | |

## Uji negatif

- Tolak izin mikrofon: tidak ada crash; pesan jelas; POWER tetap mati.
- Cabut adaptor USB saat berjalan: aplikasi tidak crash; audio kembali ke
  perangkat default atau berhenti dengan pesan.
- Tekan POWER berulang cepat: tidak ada crash/ANR.
- Buka-tutup aplikasi 5 kali: preset dan pengaturan tetap tersimpan.

## Catatan

Smoke test ini belum dijalankan di sandbox karena tidak ada perangkat Android
dan KVM tidak tersedia. Wajib dijalankan sebelum rollout production.
