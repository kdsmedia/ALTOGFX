# ALTO EFECT — Paket Rilis Play Store 1.1

Paket ini berisi bahan rilis **ALTO EFECT** untuk Google Play Store: App Bundle
yang sudah ditandatangani, APK untuk pengujian, metadata listing, grafik toko,
dokumen kepatuhan, dan panduan QA.

**Status paket:** AAB/APK sudah ditandatangani dengan upload key ALTOMEDIA.
Build release berhasil, unit test 7/7 lulus, dan AAB lolos validasi bundletool.
Screenshot toko belum disertakan karena sandbox tidak memiliki KVM/perangkat
Android; gunakan `PLAYSTORE/SCREENSHOTS/capture-screenshots.sh` pada ponsel nyata.

## Identitas aplikasi

| Item | Nilai |
|---|---|
| Nama aplikasi | ALTO EFECT |
| Package name | `com.altomedia.altoefect` |
| Versi | 1.1 (versionCode 2) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 16) |
| Kategori | Musik & Audio |
| Harga | Gratis, tanpa iklan, tanpa pembelian dalam aplikasi |
| Mode | 100% offline; tidak meminta izin INTERNET |

## Isi folder

```
ALTOMEDIA/
├── README.md                     ← file ini
├── BUILD-INFO.txt                ← detail build, hash, dan sertifikat
├── CHECKSUMS.sha256              ← checksum SHA-256 artefak
├── ARTIFACTS/
│   ├── ALTO-EFECT-1.1-release.aab   ← unggah ini ke Play Console
│   ├── ALTO-EFECT-1.1-release.apk   ← untuk sideload/QA
│   └── output-metadata.json
├── SIGNING/
│   └── README.md                 ← informasi upload key dan Play App Signing
├── PLAYSTORE/
│   ├── README.md                 ← checklist unggah Play Console
│   ├── LISTING-id-ID.md          ← judul, deskripsi singkat, deskripsi lengkap
│   ├── LISTING-en-US.md          ← versi Inggris
│   ├── DATA-SAFETY.md            ← jawaban Data safety
│   ├── CONTENT-RATING.md         ← panduan kuesioner rating
│   ├── APP-ACCESS.md             ← akses aplikasi untuk reviewer
│   ├── PRIVACY-POLICY.md         ← kebijakan privasi (Markdown)
│   ├── PRIVACY-POLICY.html       ← kebijakan privasi siap hosting
│   ├── PRIVACY-POLICY-URL.txt    ← rekomendasi URL publik
│   ├── RELEASE-NOTES-id-ID.txt
│   ├── RELEASE-NOTES-en-US.txt
│   ├── GRAPHICS/
│   │   ├── icon-512.png
│   │   ├── feature-graphic-1024x500.png
│   │   └── feature-graphic-1024x500.jpg
│   └── SCREENSHOTS/
│       ├── README.md
│       └── capture-screenshots.sh
├── BLOG/
│   ├── README.md                 ← metadata & panduan publikasi
│   ├── ALTODFX-blog-id.md        ← naskah artikel panjang
│   ├── ALTODFX-blog-id.html      ← versi HTML siap hosting
│   ├── CHECKSUMS.sha256          ← checksum naskah & aset
│   └── assets/                   ← hero, ikon, kartu sosial
├── QA/
│   ├── INSTALL-APK.md
│   └── SMOKE-TEST.md
└── LEGAL/
    ├── LICENSE.md
    ├── PLUGINS.txt
    └── THIRD-PARTY-NOTICES.md
```

## Mulai cepat

1. Buka **Play Console → Create app**. Pilih nama `ALTO EFECT`, bahasa default
   Indonesia, tipe **App**, gratis.
2. Aktifkan **Play App Signing**. Simpan upload key `ALTOMEDIA.jks` (ada di root
   repo, tidak disalin ke paket ini).
3. Upload `ARTIFACTS/ALTO-EFECT-1.1-release.aab` ke **Internal testing** lebih
   dulu, lalu uji di perangkat nyata.
4. Salin isi `PLAYSTORE/LISTING-id-ID.md` ke Store listing. Unggah grafik dari
   `PLAYSTORE/GRAPHICS/` dan screenshot yang Anda ambil sendiri.
5. Isi **Data safety**, **Content rating**, **App access**, dan **Privacy policy**
   mengikuti dokumen di folder `PLAYSTORE/`.
6. Setelah lolos review internal, promosikan ke production.

## Catatan penting

- **Jangan unggah APK ke production.** Play Store memakai AAB; APK hanya untuk
  sideload dan QA.
- **Screenshot wajib dari aplikasi asli.** Jangan memakai mockup. Jalankan
  `PLAYSTORE/SCREENSHOTS/capture-screenshots.sh` dengan ponsel yang terhubung
  lewat ADB.
- **Email dukungan** dan **URL privacy policy** masih berupa placeholder di
  dokumen listing. Isi dengan data akun Play Console Anda sebelum submit.
- **Izin mikrofon** adalah fungsi inti. Aplikasi sekarang menampilkan
  penjelasan singkat sebelum meminta izin sistem (prominent disclosure).
- Aplikasi tidak memiliki izin INTERNET; tidak ada iklan, analytics, akun,
  atau pengiriman audio ke server.

## Verifikasi cepat

```bash
sha256sum -c CHECKSUMS.sha256
/opt/android-sdk/build-tools/35.0.0/apksigner verify --print-certs \
  ARTIFACTS/ALTO-EFECT-1.1-release.apk
/usr/lib/jvm/java-21-openjdk-amd64/bin/jarsigner -verify \
  ARTIFACTS/ALTO-EFECT-1.1-release.aab
```

Detail lengkap ada di `BUILD-INFO.txt`.
