# Signing — ALTO EFECT 1.1

## Upload key

| Item | Nilai |
|---|---|
| Keystore | `ALTOMEDIA.jks` (di root repo, **tidak** disalin ke paket ini) |
| Alias | `kdsmedia` |
| Certificate owner | `CN=ALTOMEDIA, OU=Developer, O=ALTOMEDIA, L=Karawang, ST=Jawa Barat, C=ID` |
| SHA-256 | `09:FC:63:68:51:0F:7D:B4:AC:3C:41:AB:B0:61:AF:CC:5B:CF:CC:C0:30:27:0C:12:D9:12:2F:54:9D:4E:FD:04` |
| SHA-1 | `07:52:90:A9:6A:47:0B:EA:90:EF:4C:98:40:BC:5D:1F:90:6E:3E:6D` |
| Valid until | 2054-04-05 |

Password dan isi `keystore.properties` sengaja tidak ditulis di sini. Jangan pernah
mengunggah `ALTOMEDIA.jks` atau `keystore.properties` ke tempat publik.

## Play App Signing

Google Play menggunakan dua kunci:

1. **Upload key** — kunci Anda (`ALTOMEDIA.jks`) untuk menandatangani AAB yang
   diunggah.
2. **App signing key** — kunci yang disimpan Google dan dipakai menandatangani
   APK yang dikirim ke pengguna.

Saat membuat aplikasi di Play Console, pilih **Play App Signing** dan unggah
sertifikat upload key. Jika diminta, gunakan sertifikat yang sama dengan
fingerprint di atas. Setelah terdaftar, simpan `ALTOMEDIA.jks` dan password di
tempat aman; kehilangan upload key mempersulit update aplikasi.

## Verifikasi

```bash
cd ALTOMEDIA

# APK (sideload)
/opt/android-sdk/build-tools/35.0.0/apksigner verify --print-certs \
  ARTIFACTS/ALTO-EFECT-1.1-release.apk

# AAB (upload)
/usr/lib/jvm/java-21-openjdk-amd64/bin/jarsigner -verify \
  ARTIFACTS/ALTO-EFECT-1.1-release.aab

sha256sum -c CHECKSUMS.sha256
```

`jarsigner` dapat menampilkan peringatan “certificate chain is invalid” karena
sertifikat upload key bersifat self-signed. Yang penting adalah baris
`jar verified.` dan fingerprint yang sama dengan APK.
