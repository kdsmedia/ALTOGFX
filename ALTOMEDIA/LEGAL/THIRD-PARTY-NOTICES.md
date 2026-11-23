# Third-party notices — ALTO EFECT 1.0

ALTO EFECT memakai komponen pihak ketiga. Daftar lengkap plugin yang dibundel
ada di `PLUGINS.txt` (di-generate dari aplikasi). File ini adalah catatan
transparansi, **bukan nasihat hukum**. Sebelum distribusi komersial, verifikasi
lisensi setiap komponen dan penuhi kewajiban atribusi/source-code yang berlaku.

## Komponen utama

| Komponen | Fungsi | Lisensi umum |
|---|---|---|
| AndroidX / Material Components | UI Android | Apache License 2.0 |
| Oboe / AAudio | Audio low-latency | Apache License 2.0 |
| ExoPlayer (media3) | Pemutaran audio lokal | Apache License 2.0 |
| Gson | JSON preset | Apache License 2.0 |
| Kotlin / Kotlin Coroutines | Runtime | Apache License 2.0 |
| LV2 / LADSPA | Format plugin audio | ISC/BSD-style (LV2), LGPL/BSD (LADSPA) |
| lilv, serd, sord, sratom | Host LV2 | ISC/BSD-style |
| Guitarix / Gx plugins | Amp, drive, cab, efek | GPL-2.0-or-later (sebagian) |
| Calf Studio Gear (C*) | Compressor, EQ, chorus | LGPL-2.1-or-later (sebagian) |
| TAP plugins | Delay, reverb, EQ | GPL-2.0-or-later (sebagian) |
| MDA plugins | Efek legacy | Bervariasi; lihat upstream |
| Zita convolver / zita-rev1 | Reverb/convolution | GPL-2.0-or-later (sebagian) |
| SndFile / libsamplerate / FFTW | I/O dan DSP | LGPL/BSD/GPL; lihat upstream |
| LAME / Opus | Encoder opsional | LGPL/BSD-style; lihat upstream |

## Plugin audio

Folder `app/src/main/assets/lv2/` berisi plugin LV2/LADSPA dari proyek
Guitarix, Calf, TAP, MDA, Invada, dan lainnya. Metadata lisensi dan penulis
biasanya tersedia di file `.ttl`/`.json` masing-masing plugin. Beberapa plugin
mewajibkan penyediaan source code atau notice tertentu jika didistribusikan.

Sebelum merilis di Play Store:

1. Inventarisasi lisensi setiap plugin yang benar-benar dipakai.
2. Sertakan notice/atribusi yang diminta.
3. Jika ada komponen GPL/LGPL, pastikan kewajiban source code dan relinking
   dipenuhi untuk distribusi Anda.
4. Jangan menghapus notice lisensi dari file plugin.

## Tidak ada garansi

Komponen pihak ketiga disediakan “AS IS”, tanpa garansi apa pun sejauh diizinkan
hukum. ALTOMEDIA tidak mengklaim kepemilikan atas komponen pihak ketiga.

## Tautan

- LV2: https://lv2plug.in/
- LADSPA: https://www.ladspa.org/
- Guitarix: https://guitarix.org/
- Calf Studio Gear: https://calf-studio-gear.org/
- TAP plugins: https://tap-plugins.sourceforge.net/
- Oboe: https://github.com/google/oboe
- AndroidX: https://developer.android.com/jetpack/androidx
