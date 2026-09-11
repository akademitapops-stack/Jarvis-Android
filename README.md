# JARVIS v2.3 — Hermes Agent Core

Project Android lengkap (58 file sesuai panduan instalasi) — siap dibuka
langsung di **AndroidIDE**, atau di-build otomatis jadi **APK jadi** lewat
**GitHub Actions** tanpa perlu install apa-apa di HP.

## 🚀 Cara paling gampang: Build APK via GitHub (dari HP, browser saja)

1. Buat akun GitHub kalau belum punya (gratis) → **github.com**
2. Buat repository baru (nama bebas, misal `jarvis-app`), pilih **Public**
   atau **Private** — dua-duanya boleh.
3. Upload SEMUA isi folder `JARVIS` (hasil extract zip ini) ke repo tsb.
   - Termudah dari HP: buka repo → **Add file → Upload files** → pilih
     semua file/folder dari hasil extract → **Commit changes**.
   - Pastikan struktur foldernya utuh (folder `.github`, `app`, `gradle`
     ikut ter-upload, termasuk file yang diawali titik seperti
     `.gitignore` — kadang perlu di-drag manual karena file tersembunyi).
4. Setelah ter-upload, buka tab **Actions** di repo tersebut.
5. Akan otomatis muncul workflow **"Build JARVIS APK"** yang sedang
   berjalan (atau jalankan manual: **Actions → Build JARVIS APK → Run
   workflow**).
6. Tunggu sekitar 3–8 menit sampai muncul tanda ✅ hijau.
7. Klik run yang selesai tsb → scroll ke bawah ke bagian **Artifacts**
   → download **`JARVIS-debug-apk`** (berupa file .zip berisi .apk).
8. Extract, dapat file `app-debug.apk` → pindahkan ke HP → install
   (aktifkan "Izinkan dari sumber ini" kalau diminta).

**Tidak perlu AndroidIDE, tidak perlu SDK, tidak perlu laptop.** GitHub
yang build-kan APK-nya di server mereka, bos tinggal download hasil jadi.

## Cara alternatif: Build manual di AndroidIDE (di HP)

1. Extract file `JARVIS.zip` ini.
2. Buka **AndroidIDE** → menu (☰) → **Open Project** → arahkan ke folder
   `JARVIS` hasil extract (folder yang berisi `settings.gradle`).
3. Tunggu **Sync Gradle** selesai (pertama kali bisa 10–20 menit).
4. Menu → **Build → Build APK(s)** → tunggu proses build.
5. Setelah sukses, ketuk **Install** pada notifikasi/tombol yang muncul.
6. Buka aplikasi **JARVIS**, berikan semua izin yang diminta.
7. Isi API key di ⚙️ **Settings API** (lihat Bagian 7 panduan asal:
   pilih preset Groq/Gemini gratis → tempel API key → Simpan).

## Isi package

- `.github/workflows/build.yml` — konfigurasi build otomatis GitHub Actions
- `app/build.gradle`, root `build.gradle`, `settings.gradle`, `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties` — versi Gradle terkunci (8.5)
- `AndroidManifest.xml` lengkap (semua activity, service, receiver terdaftar)
- `res/values` (colors, strings, themes)
- `res/drawable` (10 file: background bubble chat, ikon mic/send)
- `res/layout` (6 file: main, item pesan, settings, HUD, vision, dashboard)
- `res/menu/main_menu.xml`
- `res/mipmap-*` — ikon aplikasi placeholder (bisa diganti sendiri nanti)
- 36 file Java di `com.hermes.jarvis` (model, utils, adapter, core, ai, voice,
  service, automation, ui) — termasuk `MainActivity.java` dan
  `SettingsActivity.java`

Total: **58 file kode** sesuai daftar pada panduan instalasi asli, plus
kerangka Gradle/manifest/CI yang sudah dirakit sehingga project bisa langsung
di-build tanpa langkah manual tambahan.

## Catatan

- APK hasil GitHub Actions adalah **APK debug** (belum ditandatangani untuk
  rilis Play Store) — cukup untuk dipakai sendiri di HP, tapi kalau nanti
  mau dipublikasikan perlu proses signing release terpisah.
- Ikon aplikasi (`ic_launcher`) adalah placeholder sederhana (lingkaran cyan
  di atas latar gelap) — ganti kapan saja lewat AndroidIDE image asset jika mau.
- Jika build gagal (baik di GitHub Actions maupun AndroidIDE), buka log
  error-nya (di GitHub Actions: klik step yang gagal, merah), kirim pesan
  errornya ke saya — saya bantu perbaiki filenya.

