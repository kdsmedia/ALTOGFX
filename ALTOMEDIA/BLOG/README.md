# Blog ALTODFX / ALTO EFECT

Folder ini berisi artikel blog panjang untuk memperkenalkan ALTODFX (nama di
Play Store: **ALTO EFECT**). Artikel ditulis dalam Bahasa Indonesia dan sudah
mencakup fitur, cara pakai, tips tone, batasan, privasi, dan FAQ.

## File

| File | Fungsi |
|---|---|
| `ALTODFX-blog-id.md` | Naskah utama Markdown, siap diedit/diimpor ke CMS |
| `ALTODFX-blog-id.html` | Versi HTML mandiri dengan desain gelap, daftar isi, dan SEO |
| `assets/altodfx-hero.png` | Gambar hero/artikel 1024×500 |
| `assets/altodfx-icon.png` | Ikon untuk header dan favicon |
| `assets/altodfx-social-1200x630.png` | Kartu sosial untuk Open Graph/Twitter |
| `CHECKSUMS.sha256` | Checksum naskah dan aset blog |

## Metadata SEO

| Item | Nilai |
|---|---|
| Title | ALTODFX: Pedalboard Gitar 9 Efek yang Bekerja 100% Offline di Android |
| Slug | `altodfx-pedalboard-gitar-offline` |
| Meta description | ALTODFX (ALTO EFECT) mengubah HP Android menjadi pedalboard gitar dengan 9 efek, 8 preset, kontrol potensiometer, dan audio native tanpa internet. |
| Author | ALTOMEDIA |
| Tanggal | 2026-11-23 |
| Bahasa | `id-ID` |
| Kata | ±3.250 kata |
| Estimasi baca | ±15 menit |
| Kategori | Musik & Audio / Guitar Tech |
| Tags | guitar effects, pedalboard, aplikasi gitar, efek gitar offline, ALTODFX, ALTO EFECT |

## Sebelum menerbitkan

Ganti placeholder berikut:

1. `[ISI URL BLOG ANDA]` — URL halaman artikel.
2. `[ISI URL GAMBAR ARTIKEL]` — URL absolut gambar `assets/altodfx-hero.png`
   atau `assets/altodfx-social-1200x630.png`.
3. `[ISI URL LOGO]` — URL absolut ikon aplikasi.
4. `[isi setelah rilis]` — tautan Play Store setelah aplikasi tersedia.

Untuk HTML, placeholder ada di `<link rel="canonical">`, meta Open Graph/Twitter,
JSON-LD, dan tautan brand. Untuk Markdown, placeholder ada di frontmatter dan
bagian penutup.

## Catatan nama

Aplikasi memakai nama **ALTO EFECT** di Play Store, sedangkan **ALTODFX** adalah
nama yang tampil pada emblem/ikon. Artikel menjelaskan hal ini di awal agar
pembaca tidak bingung. Jika nanti nama aplikasi resmi diubah menjadi ALTODFX,
cukup ganti penyebutan ALTO EFECT di frontmatter, judul, dan catatan nama.

## Saran distribusi

- Publikasikan sebagai artikel blog di situs ALTOMEDIA.
- Gunakan `altodfx-social-1200x630.png` sebagai gambar Open Graph.
- Tambahkan tautan ke halaman Play Store setelah rilis.
- Cantumkan disclaimer bahwa artikel bukan klaim emulasi resmi merek pedal.
- Untuk WordPress/CMS, gunakan versi Markdown atau salin bagian artikel dari
  versi HTML; jangan mengunggah file HTML mentah jika CMS sudah punya template.
