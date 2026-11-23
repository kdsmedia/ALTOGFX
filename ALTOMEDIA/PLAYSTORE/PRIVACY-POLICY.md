# Kebijakan Privasi — ALTO EFECT

**Terakhir diperbarui:** 23 November 2026
**Pengembang:** ALTOMEDIA
**Aplikasi:** ALTO EFECT (`com.altomedia.altoefect`)

## Ringkasan

ALTO EFECT adalah prosesor efek gitar yang bekerja sepenuhnya offline. Kami tidak
mengumpulkan, menjual, menyewakan, atau membagikan data pribadi Anda. Aplikasi
tidak memiliki izin INTERNET dan tidak mengirim audio, preset, atau informasi
perangkat ke server mana pun.

## Mikrofon dan audio

ALTO EFECT meminta izin `RECORD_AUDIO` hanya untuk menerima sinyal gitar atau
audio dari mikrofon/headset jack dan memprosesnya secara real-time di perangkat.
Audio diproses di memori perangkat dan dibuang setelah diproses. ALTO EFECT
tidak merekam, menyimpan, atau mengunggah audio dari mikrofon.

Jika Anda menghubungkan antarmuka audio USB, Android menangani akses perangkat
tersebut. ALTO EFECT hanya membaca aliran audio yang dipilih melalui API audio
Android.

## Data yang disimpan di perangkat

- Preset, pengaturan efek, pilihan tema, dan preferensi aplikasi disimpan secara
  lokal di penyimpanan aplikasi.
- Data lokal tersebut tidak dapat diakses oleh ALTOMEDIA dan tidak dikirim ke
  server.
- Anda dapat menghapus data lokal dengan menghapus preset di aplikasi, memilih
  **Clear storage** pada pengaturan Android, atau menghapus instalasi aplikasi.

## Backup Android

Aplikasi mengizinkan backup data aplikasi melalui mekanisme Android. Jika Anda
mengaktifkan backup Android, sistem operasi atau penyedia backup Anda dapat
menyalin data lokal (misalnya preset dan pengaturan) ke penyimpanan backup.
ALTOMEDIA tidak menerima salinan tersebut dan tidak memiliki akses ke sana.
Anda dapat menonaktifkan backup di pengaturan Android.

## Izin aplikasi

| Izin | Fungsi |
|---|---|
| `RECORD_AUDIO` | Menerima input gitar/audio secara real-time. Tidak untuk merekam atau mengunggah audio. |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Izin internal AndroidX untuk receiver aplikasi; bukan akses data pengguna. |

Aplikasi **tidak** meminta izin INTERNET, lokasi, kontak, kamera, penyimpanan
eksternal, atau akses ke akun.

## Iklan, analytics, dan pihak ketiga

- Tidak ada iklan.
- Tidak ada analytics, crash reporting, atau pelacakan.
- Tidak ada akun, login, atau profil pengguna.
- Tidak ada pembelian dalam aplikasi.
- Aplikasi memakai pustaka AndroidX, Oboe/AAudio, dan plugin audio LV2/LADSPA
  yang berjalan secara lokal. Pustaka tersebut tidak mengirim data ke ALTOMEDIA.

## Anak-anak

ALTO EFECT bukan aplikasi yang ditujukan untuk anak di bawah 13 tahun. Kami tidak
secara sengaja mengumpulkan data dari anak-anak. Karena aplikasi tidak
mengumpulkan data pribadi, tidak ada data anak yang kami simpan.

## Keamanan

Karena tidak ada data yang dikirim ke server, tidak ada data pribadi yang
disimpan di server ALTOMEDIA. Data lokal berada di perangkat Anda dan tunduk pada
keamanan perangkat serta sistem operasi Android.

## Perubahan kebijakan

Jika kebijakan ini berubah, versi terbaru akan dipublikasikan di URL ini dengan
tanggal pembaruan baru. Perubahan material akan diumumkan melalui halaman
aplikasi atau catatan rilis.

## Kontak

Pertanyaan tentang privasi dapat dikirim melalui:

- GitHub Issues: https://github.com/kdsmedia/ALTOGFX/issues
- Pengembang: ALTOMEDIA

---

## English summary

ALTO EFECT is a fully offline guitar effects processor. We do not collect, sell,
or share personal data. The microphone permission is used only to receive live
guitar/audio input; audio is processed on-device in real time and is not recorded,
stored, or uploaded. Presets and settings are stored locally and are not sent to
ALTOMEDIA. The app has no INTERNET permission, ads, analytics, accounts, or
in-app purchases. Contact: https://github.com/kdsmedia/ALTOGFX/issues
