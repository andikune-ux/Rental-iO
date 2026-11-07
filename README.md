# Rental.iO 🎮🔒

**Rental.iO** adalah aplikasi Android Home Launcher & Kiosk Lockdown profesional khusus bisnis rental perangkat gaming HP / tablet dengan estetika futuristik Cyberpunk RGB Neon, 3D Gyroscope Parallax Wallpaper, sistem proteksi Device Owner (LockTask Mode), dan manajemen Multi-Tenant (Owner – Admin – User).

---

## 🌟 Fitur Utama

- **Kiosk Lockdown & Anti-Bypass**: Mengunci perangkat dalam mode Kiosk (`LockTaskMode`), menonaktifkan status bar, tombol Home/Recent Apps, dan pengaturan sistem.
- **3D Gyroscope Parallax Background**: Wallpaper gaming responsif terhadap orientasi/kemiringan fisik perangkat (Gyroscope & Accelerometer) dengan pergeseran matriks mulus (*low-pass filtered interpolation*).
- **Multi-Tenant Hierarchy (Owner - Admin - User)**:
  - **Owner (Super Admin)**: Mengatur lisensi Admin pusat, memantau armada perangkat, dan mengelola rilis GitHub.
  - **Admin (Pengelola Rental)**: Mengatur durasi sewa, memilih aplikasi game whitelist, menambah waktu sewa (Online Firestore atau Offline Manual), dan membuka kunci dengan PIN Master.
  - **User (Penyewa / Guest)**: Terkunci di launcher, hanya dapat membuka game dari daftar Whitelist, dan melihat sisa waktu bermain secara real-time.
- **Persistent Kiosk Foreground Service**: Menghitung mundur durasi sewa dengan Foreground Service kebal dari background killer sistem Android.
- **Dual Engine Persistence (Cloud Firestore + Room Database Offline Fallback)**:
  - Jika kuota Firebase habis atau tanpa sinyal internet, aplikasi beralih secara instan ke Room Database lokal.
- **Game Over Alert System**: Ketika timer `00:00:00`, layar terkunci seketika dengan overlay merah peringatan, alarm audio berulang, getaran haptik, dan form PIN Admin Neon.
- **In-App Auto-Update via GitHub Releases**: Pengecekan otomatis rilis terbaru dari `https://api.github.com/repos/andikune-ux/rental-io/releases/latest`, unduh ke `rentalio/pembaruan aplikasi/apk/app-release.apk`, dan pasang via Android `FileProvider`.

---

## 🏗️ Arsitektur Proyek & Direktori

```
rental-io/
├── .github/
│   └── workflows/
│       └── android_release.yml         # CI/CD Automated Build & GitHub Release
├── app/
│   ├── build.gradle.kts                # Dependencies, SigningConfigs & Namespace
│   ├── proguard-rules.pro              # Obfuscation & Security Rules
│   └── src/
│       └── main/
│           ├── java/dev/andikune/rentalio/
│           │   ├── data/
│           │   │   ├── local/          # Room Database (LocalRentalState, DAO)
│           │   │   ├── model/          # Models (Owner, Admin, Device, WhitelistApp)
│           │   │   ├── remote/         # Retrofit GitHub API & Firestore Client
│           │   │   └── repository/     # RentalRepository (Offline-First sync)
│           │   ├── receiver/           # AdminReceiver (DeviceAdminReceiver)
│           │   ├── service/            # KioskForegroundService & UpdateManager
│           │   ├── ui/
│           │   │   ├── components/     # Neon Timer, Game Carousel, Admin Dialog, Overlays
│           │   │   ├── GyroscopeHelper.kt # Sensor translation filter for 3D Parallax
│           │   │   ├── MainActivity.kt # Main Landscape Kiosk Launcher
│           │   │   └── MainViewModel.kt# Launcher State Management
│           │   └── utils/              # DeviceOwnerManager & Security Helpers
│           ├── res/                    # Drawables, Landscape Layout, Values
│           └── AndroidManifest.xml
├── LICENSE                             # MIT License
└── README.md                           # Dokumentasi & Panduan Setup
```

---

## ⚡ Panduan Setup Device Owner (Kiosk Lockdown) via ADB

Agar Rental.iO dapat mengaktifkan **LockTask Mode penuh** (tanpa dialog konfirmasi dan mematikan tombol sistem), aplikasi harus didaftarkan sebagai **Device Owner**:

1. Pastikan perangkat belum memiliki akun Google yang login (atau factory reset / hapus akun di Settings > Accounts).
2. Hubungkan HP ke komputer dengan kabel USB dan aktifkan **USB Debugging**.
3. Buka terminal/command prompt, jalankan perintah berikut:
   ```bash
   adb shell dpm set-device-owner dev.andikune.rentalio/.receiver.AdminReceiver
   ```
4. Output berhasil:
   ```text
   Success: Device owner set to package dev.andikune.rentalio
   ```
5. Buka kembali aplikasi Rental.iO. Kini Kiosk LockTask aktif penuh!

Untuk melepas Device Owner jika diperlukan:
```bash
adb shell dpm remove-active-admin dev.andikune.rentalio/.receiver.AdminReceiver
```

---

## 🏠 Menjadikan Rental.iO sebagai Home Launcher Default

Rental.iO sekarang otomatis meminta peran **Home Launcher utama**:

- **Device Owner aktif** → peran Home dipasang **permanen tanpa dialog** memakai
  `DevicePolicyManager.addPersistentPreferredActivity`. Tombol Home selalu
  kembali ke kiosk Rental.iO.
- **Bukan Device Owner** → aplikasi menampilkan dialog sistem **"Home app"**
  (`RoleManager.ROLE_HOME` di Android 10+). Pengguna cukup memilih
  **Rental.iO → Always / Selalu**.

Konfigurasi manual jika dialog tidak muncul:

1. Buka **Settings → Apps → Default apps → Home app**.
2. Pilih **Rental.iO**.
3. Tekan tombol Home untuk memastikan kiosk langsung terbuka.

Alternatif lewat ADB (Android 10+), tanpa dialog:

```bash
adb shell cmd package set-home-activity dev.andikune.rentalio/.ui.MainActivity
```

Untuk hasil terbaik pada perangkat rental, tetap daftarkan sebagai **Device
Owner** (lihat panduan ADB di atas) agar peran Home tidak bisa diubah penyewa.

---

## 🚀 CI/CD GitHub Actions & Signing Keystore

Pipeline di `.github/workflows/android_release.yml` otomatis membangun APK rilis ketika Anda membuat tag `v*` (contoh: `git tag v1.0.0 && git push origin v1.0.0`):

### Konfigurasi GitHub Secrets:
1. `ANDIKUNE_KEYSTORE_BASE64`: File keystore `andikune.jks` yang telah di-encode base64:
   ```bash
   base64 -w 0 andikune.jks > keystore_base64.txt
   ```
2. `ANDIKUNE_STORE_PASSWORD`: Password keystore
3. `ANDIKUNE_KEY_PASSWORD`: Password key alias

---

## 🛡️ Hak Akses & Keamanan

- **Admin Master PIN Default**: `123456` (dapat diubah melalui menu Admin → Keamanan).
- **PIN tersimpan sebagai SHA-256** di Room Database. PIN lama yang masih
  plaintext tetap diterima saat upgrade, lalu otomatis dimigrasikan ketika
  admin mengganti PIN.
- **Anti-Force Close**: KioskForegroundService memanfaatkan `START_STICKY`, timer
  berbasis epoch (tahan process-death & reboot), dan Notification Channel
  prioritas tinggi dengan full-screen intent saat waktu habis.
- **Orientation Lock**: Terkunci permanen pada mode horizontal (Landscape) untuk kenyamanan gaming dock.

---

## ⚠️ Batasan Android yang Perlu Diketahui

- Android **tidak mengizinkan aplikasi biasa** mengganti Home Launcher secara
  senyap. Dialog sistem (RoleManager / chooser Home) adalah batas maksimal untuk
  aplikasi non-Device Owner. Untuk pengalaman kiosk penuh, daftarkan perangkat
  sebagai **Device Owner**.
- Full-screen intent saat waktu habis dapat dibatasi pada Android 14+ jika izin
  `USE_FULL_SCREEN_INTENT` dicabut pengguna. Notifikasi alarm tetap muncul.
- Mode Unlimited menghitung biaya berjalan Rp 100/menit dan dihentikan lewat
  tombol **Selesaikan Sewa Unit** (memerlukan PIN Admin).
