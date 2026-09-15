# Adsterra App (WebView + Auto-login)

## Cara paling gampang dapetin APK: GitHub Actions (gak perlu Android Studio)
1. Bikin repo baru di GitHub (bisa private), upload/drag-drop SEMUA isi folder ini ke repo itu (termasuk folder `.github` yang isinya workflow build).
2. Buka tab **Actions** di repo tersebut. Kalau belum otomatis jalan, klik workflow "Build Debug APK" → tombol **Run workflow**.
3. Tunggu proses build selesai (~2-5 menit, ada tanda centang hijau kalau sukses).
4. Klik run yang barusan selesai → scroll ke bagian **Artifacts** → download `adsterra-app-debug-apk` (isinya file `app-debug.apk`).
5. Transfer APK itu ke HP (lewat kabel, Google Drive, WhatsApp ke diri sendiri, dll), lalu install. HP mungkin minta izinkan "Install dari sumber tidak dikenal" karena ini bukan dari Play Store — itu normal untuk app buatan sendiri.

## Cara alternatif: build manual pakai Android Studio
1. Buka **Android Studio** → `Open` → pilih folder project ini (folder yang isinya ada `settings.gradle.kts`).
2. Tunggu Android Studio selesai "Gradle Sync" (otomatis, akan generate gradle wrapper sendiri kalau belum ada).
3. Klik Run ▶️ ke HP/emulator kamu.
4. Pertama kali dibuka, app akan minta email & password Adsterra → disimpan terenkripsi di HP (pakai `EncryptedSharedPreferences`, bukan plain text).
5. Setelah itu, tiap buka app langsung ke `beta.publishers.adsterra.com` dan otomatis coba isi + submit form login.

## Kalau auto-login gak jalan
Adsterra dashboard-nya web app modern (React/Vue) yang form login-nya di-render pakai JavaScript, jadi saya nggak bisa cek langsung nama field aslinya dari sini. Script yang saya buat coba beberapa pola selector umum (`input[type="email"]`, `input[name="password"]`, dll), tapi kalau ternyata gak cocok:

1. Buka https://beta.publishers.adsterra.com/ di **Chrome desktop**.
2. Klik kanan di field email → **Inspect**.
3. Lihat atribut `id` atau `name` di tag `<input>` itu (dan yang password juga).
4. Kasih tau saya nama-nama itu → saya update selector di `injectAutoLogin()` (di `MainActivity.kt`) biar pasti kena.

## Kalau ada 2FA / captcha di akun Adsterra kamu
Auto-login gak akan bisa nembus itu otomatis — itu memang di luar kendali app WebView biasa, harus diisi manual tiap kali.

## Ubah email/password yang disimpan
Tekan tombol menu (⋮) di app → "Ubah Email/Password".
