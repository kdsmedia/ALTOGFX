# Play Store — Checklist Unggah ALTO EFECT 1.1

Dokumen ini adalah urutan kerja yang disarankan. Isi yang bertanda
`[ISI ...]` harus diganti dengan data akun Anda.

## 1. Buat aplikasi

- Play Console → **Create app**
- App name: **ALTO EFECT**
- Default language: **Indonesian (id-ID)**
- App or game: **App**
- Free or paid: **Free**
- Declarations: centang persetujuan yang diminta.

## 2. Play App Signing

- Pilih **Play App Signing**.
- Upload certificate dari `ALTOMEDIA.jks` (alias `kdsmedia`).
- Simpan file keystore dan password di tempat aman.
- Detail fingerprint ada di `../SIGNING/README.md`.

## 3. Internal testing lebih dulu

- **Testing → Internal testing → Create new release**
- Upload `../ARTIFACTS/ALTO-EFECT-1.1-release.aab`.
- Tambahkan tester (email Anda sendiri).
- Review dan roll out.
- Pasang dari link internal testing di ponsel nyata, lalu jalankan
  `../QA/SMOKE-TEST.md`.

Jangan mengunggah APK ke production. Play Store production memakai AAB.

## 4. Store listing

Ambil teks dari `LISTING-id-ID.md` (utama) dan `LISTING-en-US.md` (opsional).

| Field | Isi |
|---|---|
| App name | ALTO EFECT |
| Short description | Dari `LISTING-id-ID.md` (maks. 80 karakter) |
| Full description | Dari `LISTING-id-ID.md` (maks. 4000 karakter) |
| App icon | `GRAPHICS/icon-512.png` (512×512 PNG) |
| Feature graphic | `GRAPHICS/feature-graphic-1024x500.png` (1024×500, tanpa alpha) |
| Phone screenshots | Minimal 2, maksimal 8; ambil dari aplikasi asli |
| Tablet screenshots | Opsional; aplikasi berorientasi portrait |
| Category | Music & Audio |
| Contact email | `[ISI EMAIL DUKUNGAN ANDA]` |
| Privacy policy | URL publik dari `PRIVACY-POLICY-URL.txt` |
| Ads | No |
| In-app purchases | No |

## 5. Screenshot

Sandbox build tidak memiliki KVM/perangkat Android, jadi screenshot tidak bisa
diambil di sini. Gunakan ponsel nyata:

```bash
cd PLAYSTORE/SCREENSHOTS
./capture-screenshots.sh
```

Atau ikuti langkah manual di `SCREENSHOTS/README.md`. Jangan memakai mockup
atau gambar yang bukan tampilan aplikasi.

## 6. App content

Isi menu **Policy → App content**:

- **Privacy policy**: tempel URL dari `PRIVACY-POLICY-URL.txt`.
- **Ads**: No ads.
- **App access**: All functionality is available without special access.
  Lihat `APP-ACCESS.md`.
- **Content rating**: ikuti `CONTENT-RATING.md`; hasil yang diharapkan
  **Everyone / 3+**.
- **Target audience**: 13+ (aplikasi alat musik, bukan aplikasi anak).
- **News app**: No.
- **Data safety**: ikuti `DATA-SAFETY.md`; tidak ada data yang dikumpulkan
  atau dibagikan.
- **Government app**: No.
- **Financial features**: None.
- **Health**: No.

## 7. Release notes

Tempel isi `RELEASE-NOTES-id-ID.txt` (dan `RELEASE-NOTES-en-US.txt` jika
menambahkan locale Inggris).

## 8. Review dan rollout

- Periksa **Pre-launch report** jika tersedia.
- Pastikan tidak ada error instalasi/launch pada perangkat uji.
- Rollout production bertahap (misalnya 20% → 50% → 100%).
- Untuk update berikutnya, naikkan `versionCode` di `app/build.gradle`, build
  ulang, lalu unggah AAB baru. Jangan mengganti upload key.

## Catatan kepatuhan mikrofon

ALTO EFECT meminta `RECORD_AUDIO` hanya untuk input gitar/audio secara real-time.
Aplikasi sekarang menampilkan dialog penjelasan sebelum izin sistem diminta.
Audio diproses di perangkat dan tidak direkam/diunggah. Tidak ada izin INTERNET.
Pastikan privacy policy dan Data safety konsisten dengan perilaku ini.
