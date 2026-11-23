# Data safety — ALTO EFECT 1.1

Jawaban di bawah mengikuti formulir **Data safety** di Play Console. Semua
pernyataan harus konsisten dengan `PRIVACY-POLICY.md` dan perilaku aplikasi.

## Bagian 1 — Data collection and security

| Pertanyaan | Jawaban |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **N/A** — tidak ada data yang dikirim keluar perangkat |
| Do you provide a way for users to request that their data is deleted? | **N/A** — tidak ada data di server. Data lokal dapat dihapus dengan Clear storage atau uninstall. |

## Bagian 2 — Data types

Tidak ada tipe data yang dipilih sebagai **Collected** atau **Shared**.

Data berikut **diakses secara lokal tetapi tidak dikumpulkan/dibagikan**:

| Data type | Status | Penjelasan |
|---|---|---|
| Audio (microphone) | Diproses sementara (ephemeral) | Input gitar/audio diproses real-time di perangkat; tidak direkam, disimpan, atau diunggah. |
| App activity / preferences | Hanya lokal | Preset, pengaturan efek, dan tema disimpan di penyimpanan aplikasi; tidak dikirim ke ALTOMEDIA. |
| Device or other IDs | Tidak diakses | Tidak ada advertising ID, instalasi ID, atau ID perangkat yang dikumpulkan. |
| Location, contacts, calendar, photos, files, messages | Tidak diakses | Aplikasi tidak meminta izin untuk data tersebut. |

## Bagian 3 — Data usage and handling

- **Purpose:** microphone audio digunakan hanya untuk fitur inti pemrosesan efek
  gitar secara real-time.
- **Ephemeral processing:** audio diproses di memori dan dibuang setelah diproses.
- **No collection:** tidak ada audio yang dikirim ke server, pihak ketiga, atau
  ALTOMEDIA.
- **No sharing:** tidak ada data yang dijual, disewakan, atau dibagikan.
- **No advertising or analytics SDK:** tidak ada AdMob, Firebase Analytics,
  Crashlytics, atau pelacakan pihak ketiga.
- **No account:** tidak ada login, profil, atau penyimpanan data di cloud.

## Bagian 4 — Permissions

| Permission | Alasan |
|---|---|
| `android.permission.RECORD_AUDIO` | Menerima input gitar/audio real-time. Bukan untuk merekam atau mengunggah audio. |
| `com.altomedia.altoefect.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Izin internal AndroidX; bukan akses data pengguna. |

Tidak ada `INTERNET` atau `ACCESS_NETWORK_STATE`.

## Bagian 5 — Backup

Aplikasi memiliki `android:allowBackup="true"`. Jika pengguna mengaktifkan
backup Android, sistem operasi/penyedia backup dapat menyalin data lokal
(preset/pengaturan) ke backup. ALTOMEDIA tidak menerima salinan tersebut dan
tidak memiliki akses ke sana. Ini adalah fitur OS, bukan pengumpulan data oleh
aplikasi.

## Bagian 6 — Catatan untuk reviewer

- Aplikasi dapat dipakai tanpa akun.
- Mikrofon hanya aktif setelah pengguna menekan POWER dan menyetujui dialog
  penjelasan serta izin sistem.
- Tanpa izin mikrofon, pengguna tetap dapat membuka UI, melihat preset, dan
  mengubah pengaturan, tetapi audio tidak berjalan.
- Tidak ada iklan atau pembelian dalam aplikasi.

## Jangan diisi

Jangan mencentang "Data collected" hanya karena aplikasi memakai mikrofon.
Google mendefinisikan **collection** sebagai pengiriman data keluar dari
perangkat. ALTO EFECT memproses audio secara lokal dan tidak mengirimkannya.
