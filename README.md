# 📄 Scanner Pro — All-in-One Android Document Scanner & Converter

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.4.1-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/studio/releases/gradle-plugin)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue.svg?style=flat)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-green.svg?style=flat)](https://developer.android.com/about/versions/15)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575.svg?style=flat&logo=materialdesign)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Scanner Pro** adalah aplikasi pemindai dokumen, OCR, penerjemah foto, konverter dokumen Office, dan kompresor file serba bisa untuk Android. Didesain dengan antarmuka **Material 3 Premium**, mendukung **Mode Gelap & Terang**, pemindaian kamera live via **CameraX**, serta pemrosesan dokumen tingkat lanjut secara offline.

---

## ✨ Fitur-Fitur Utama (Key Features)

### 📚 1. Pemindai Banyak Halaman & Dokumen (Multi-Page Batch Scanner)
- **Scan Buku & Tugas**: Pindai puluhan halaman buku (seperti hlm 90–110) atau tugas sekolah secara beruntun dengan cepat.
- **Scan KTP & Kartu Identitas**: Ambil foto KTP Sisi Depan dan Sisi Belakang secara terpandu, otomatis digabungkan secara presisi pada 1 lembar halaman PDF A4.
- **Scan Paspor & Berkas Resmi**: Dielengkapi *viewfinder frame* khusus untuk dokumen resmi.

### ✂️ 2. Editor Review & Potong Manual (Interactive Review & Crop)
- **Review Halaman**: Ditemani *thumbnail strip* interaktif di bagian bawah untuk menavigasi tiap halaman.
- **Potong Manual**: Geser 4 titik sudut interaktif untuk memotong dan memperjelas area dokumen sesuai kebutuhan.
- **Foto Ulang (Retake)**: Jika terdapat halaman yang buram/blur, Anda dapat memfoto ulang *halaman itu saja* tanpa mengulang halaman lainnya.
- **Putar & Hapus**: Rotasi 90° atau hapus halaman tertentu dengan satu kali ketuk.

### 🔍 3. Ekstrak Teks OCR (Text Recognition)
- Menggunakan **Google ML Kit Text Recognition** untuk mengekstrak teks cetak/tulisan dari foto kamera atau galeri secara presisi.
- Dilengkapi tombol untuk menyalin teks ke *clipboard* atau membagikannya langsung.

### 🌐 4. Penerjemah Foto (Photo Translator)
- Terjemahkan teks dari gambar secara instan ke berbagai bahasa (Indonesia, Inggris, Spanyol, Prancis, Jerman, Jepang, China, Korea, Arab, Rusia).
- Ditenagai oleh **Google ML Kit On-Device Translation**.

### 🔄 5. Konversi Dokumen Lengkap (Document Converter)
- **PDF ke Word (`.docx`)**: Mengonversi teks PDF menjadi dokumen Word Office Open XML.
- **PDF ke Excel (`.xlsx`)**: Mengonversi baris & tabel PDF menjadi lembar kerja Excel.
- **PDF ke PowerPoint (`.pptx`)**: Mengonversi halaman PDF menjadi slide presentasi PPT.
- **PDF ke Gambar (`.jpg`)**: Rendering tiap halaman PDF menjadi file gambar berkualitas tinggi.

### 🗜️ 6. Kompresi File & Arsip (File Compressor & Zip)
- **Kompres Foto**: Perkecil ukuran resolusi & kualitas foto JPEG.
- **Kompres PDF**: Re-sample halaman PDF untuk memperkecil ukuran file PDF.
- **Kompres ke ZIP**: Gabungkan dan kompres beberapa file dokumen menjadi arsip `.zip`.

### 🌙 7. Tema Mode Gelap & Terang (Dark & Light Theme)
- Beralih dengan mudah antara **Mode Terang (Light)**, **Mode Gelap (Dark)**, atau **Mengikuti Sistem**.
- Tema dikelola secara persisten dengan **Material 3 Color System**.

### 📁 8. Manajemen File Tersimpan (Saved Documents Manager)
- Akses cepat ke semua file hasil pemindaian dan konversi di folder lokal `ScannerPro`.
- Buka file, bagikan via WhatsApp/Email/Drive, atau hapus file.

---

## 🛠️ Arsitektur & Teknologi (Tech Stack)

- **Bahasa**: [Kotlin 2.0](https://kotlinlang.org)
- **Komponen UI**: Material Design 3, ViewBinding, ConstraintLayout, CoordinatorLayout, RecyclerView
- **Kamera In-App**: [AndroidX CameraX 1.4](https://developer.android.com/training/camerax) (Camera2, Lifecycle, View)
- **Machine Learning**: [Google ML Kit Vision](https://developers.google.com/ml-kit) (Text Recognition & Translation)
- **Pemrosesan PDF & Office**: Native Android `PdfDocument`, `PdfRenderer`, dan Generator Office Open XML Murni dalam Kotlin.
- **Manajemen Modul**: Gradle Version Catalog (`libs.versions.toml`)

---

## 📂 Struktur Proyek (Project Structure)

```text
com.example.scanner
├── model/
│   └── ScannedDocument.kt            # Model data untuk file & item terpilih
├── ui/
│   ├── MainActivity.kt               # Dashboard utama dengan banner & kartu fitur
│   ├── CameraScannerActivity.kt      # Kamera live in-app dengan garis scan cyan
│   ├── BatchScannerActivity.kt       # Kamera batch pemindai buku & KTP
│   ├── ScanReviewActivity.kt         # Editor review, potong manual & foto ulang
│   ├── OcrActivity.kt                # Layar ekstrak teks OCR
│   ├── TranslatorActivity.kt         # Layar penerjemah foto
│   ├── ImageToPdfActivity.kt         # Impor foto ke PDF
│   ├── PdfMergerActivity.kt          # Penggabung banyak PDF
│   ├── ConverterActivity.kt          # Konverter PDF ke Word, Excel, PPT & Gambar
│   ├── CompressorActivity.kt         # Kompresor foto, PDF & Zip
│   ├── SavedFilesActivity.kt         # Kelola file tersimpan
│   ├── adapters/                     # Adapter RecyclerView
│   └── views/
│       └── CropOverlayView.kt        # Custom View 4-titik sudut potong manual
└── utils/
    ├── OcrManager.kt                 # Wrapper ML Kit Text Recognition
    ├── TranslatorManager.kt          # Wrapper ML Kit Translation
    ├── PdfUtils.kt                   # Engine PDF (Image to PDF, Merge, Convert, Compress)
    ├── DocumentCropUtils.kt          # Algoritma potong & perjelas kontras dokumen
    ├── IdCardUtils.kt                # Generator KTP Depan+Belakang 1 A4 PDF
    ├── DocConverter.kt               # Engine Konverter PDF ke DOCX, XLSX, PPTX
    ├── ImageCompressor.kt            # Utility kompresi gambar
    ├── ZipUtils.kt                   # Utility kompresi ZIP
    ├── FileUtils.kt                  # Utility penyimpanan & FileProvider
    └── ThemeManager.kt               # Utility pengelola tema gelap & terang
```

---

## 🚀 Cara Menjalankan Proyek (Getting Started)

### Prasyarat
- Android Studio Ladybug (2024.2+) atau lebih baru
- JDK 17
- Android SDK 35 (Min SDK 24 / Android 7.0+)

### Langkah-Langkah Install & Build
1. **Clone repository ini**:
   ```bash
   git clone https://github.com/Dzacky1302/Scanner.git
   cd Scanner
   ```
2. **Buka proyek di Android Studio**:
   Pilih `File` ➔ `Open` ➔ Pilih folder `Scanner`.
3. **Sync Gradle**:
   Tunggu Android Studio mendownload seluruh dependensi Gradle secara otomatis.
4. **Jalankan Aplikasi**:
   Hubungkan HP Android via USB atau gunakan Emulator, lalu klik tombol **Run** (▶️).

---

## 📜 Lisensi (License)

Proyek ini terlisensi di bawah **[MIT License](LICENSE)**. Anda bebas menggunakan, memodifikasi, dan mendistribusikan ulang kode ini.

---

<p align="center">
  Dibuat dengan ❤️ menggunakan <b>Kotlin</b> & <b>Android Jetpack</b>.
</p>
