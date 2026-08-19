# 📱 RuLaFHub Android (v2.0-alpha)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.2-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**RuLaFHub Android** ialah aplikasi mudah alih rasmi berasaskan **Jetpack Compose** dan **Room Database** yang bertindak sebagai gerbang pengurusan, pentaksiran, dan gamifikasi interaktif untuk ekosistem Pendidikan Islam. Dibangunkan khas untuk mengimplementasikan **Rangka Kerja Pembelajaran Terbeza dan Pentaksiran Pelbagai Tahap (RuLaF)** di bilik darjah secara manusiawi, mampan, dan adaptif.

---

## 📖 Latar Belakang & Falsafah (RuLaF & REDF)

Aplikasi ini diilhamkan daripada protokol intervensi digital untuk mengatasi krisis kemerosotan literasi Jawi dan asas 3M. Berpandukan kepada **RuLaF Educational Development Framework (REDF)**, sistem ini menyokong pengagihan murid secara hibrid (nisbah 2:2:1) ke dalam kumpulan:
*   **RuLaF Khas** - Pemulihan tegar kognitif & psikomotor.
*   **RuLaF Alif** - Murid tahap lemah (bimbingan cantum suku kata).
*   **RuLaF Ba** - Murid tahap sederhana (latihan konteks & isi tempat kosong).
*   **RuLaF Ta** - Murid cemerlang yang bertindak sebagai mentor rakan sebaya.

---

## 🌟 Ciri-Ciri Utama (Key Features)

### 1. 🔑 Sistem Log Masuk Kalis Bolos (Secure Role-Based Auth)
*   **Autentikasi Supabase:** Log masuk selamat menggunakan gandingan **E-mel** dan **Kata Laluan** bertopeng.
*   **Penapisan Silang Peranan (*Role Protection*):** Sistem menyemak jadual `profil_pengguna` Supabase secara automatik. Murid tidak dibenarkan mengakses panel pendidik, dan sebaliknya.
*   **Mod Sandaran Luar Talian (*Secure Offline Fallback*):** Untuk keselamatan fasa pembangunan, kelayakan luar talian dihadkan secara eksklusif kepada akaun demo rasmi sahaja (`guru@rulafhub.com` / `murid@rulafhub.com` dengan kata laluan `rulaf2026`).

### 2. 🎮 Auto-Sync Misi Arked
*   Enjin permainan Jawi (contoh: *Solat Jumaat, Solat Hari Raya, Gerhana Matahari & Bulan*) akan **menyegerak secara automatik** di latar belakang menggunakan pautan raw GitHub rasmi sebaik sahaja tab dibuka, tanpa memerlukan murid memicu muat turun secara manual.

### 3. 📁 Repositori Open-BBM Ala GitHub
*   Penyusunan Bahan Bantu Mengajar (BBM) yang disegerakkan dari Supabase mengikut **struktur pokok folder (*Directory Tree*)**.
*   Pendidik boleh melayari folder sub-kategori (Subjek ➡️ Darjah ➡️ Fail) berserta butang **Muat Tunun** yang pantas dan mesra tempatan.

### 4. 💬 Forum Komuniti Interaktif
*   Sistem perbincangan dua hala yang membolehkan pendidik mengetuk mana-mana topik forum, membaca komen komuniti, dan menghantar komen baharu secara masa nyata terus ke pangkalan data awan Supabase.

### 5. 🎨 Navigasi YouTube Style (Minimalis Dwi-Tema)
*   Menggunakan penataan navigasi bawah yang minimalis dengan mematangkan estetik Material 3 (penunjuk bulatan aktif tebal yang menyemak telah dibuang sepenuhnya untuk paparan sifar-pepijat visual).
*   Sokongan penuh dwi-tema (*Dark/Light Mode*) yang dioptimumkan secara visual dan responsif.

---

## 🛠️ Arsitektur & Spesifikasi Teknikal

Aplikasi ini dibina berlandaskan konsistensi standard **JVM 17** bagi menjamin kestabilan penyusunan semula (*compilation*):
*   **Bahasa Pengaturcaraan:** Kotlin 1.9.22
*   **Kerangka Kerja UI:** Jetpack Compose (BOM 2023.10.01 / compiler 1.5.8)
*   **Pangkalan Data Tempatan:** Room SQLite Database 2.6.1
*   **Rangkaian & API:** Retrofit 2.9.0 & OkHttp 4.12.0
*   **Integrasi Awan:** Supabase Cloud API Engine

---

## 🔐 Pemasangan & Persediaan Setempat (Local Setup)

Untuk memastikan kunci API sensitif anda selamat daripada pencerobohan bots di GitHub, projek ini menggunakan teknik suntikan dinamis **`local.properties`** (bertindak sebagai `.env.local` Android):

### Langkah 1: Cipta Fail `local.properties`
Di dalam direktori utama (*root*) projek anda, cipta fail bernama `local.properties` (fail ini telah dikecualikan secara automatik dalam `.gitignore` anda):
```properties
# Peringkat Laluan SDK Komputer Anda
sdk.dir=/home/your-username/Android/Sdk

# 🔐 Kunci Selamat Supabase Anda
supabase.url="https://your-supabase-project.supabase.co/"
supabase.anon.key="your-actual-supabase-anon-key-here"
```

### Langkah 2: Aktifkan BuildConfig di `app/build.gradle.kts`
Suntik pembaca properties di dalam fail gradle peringkat aplikasi anda:
```kotlin
import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val supabaseUrl = localProperties.getProperty("supabase.url") ?: "\"https://hmbzxmougiaubiooqqk.supabase.co/\""
val supabaseAnonKey = localProperties.getProperty("supabase.anon.key") ?: "\"YOUR_FALLBACK_KEY\""

android {
    ...
    defaultConfig {
        ...
        buildConfigField("String", "SUPABASE_URL", supabaseUrl)
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseAnonKey)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Langkah 3: Bersihkan Cache dan Jalankan Projek
Buka terminal dalaman Android Studio anda dan taip:
```bash
# Windows
gradlew clean

# Mac / Linux
./gradlew clean
```
Tekan butang **Sync Project with Gradle Files** (ikon gajah), kemudian tekan butang **Run (Segitiga Hijau)** untuk memulakan aplikasi!

---

## 🔒 Polisi Keselamatan & Perlindungan Privasi Data Murid

Projek ini mengamalkan **Toleransi Sifar (Zero-Tolerance Policy)** terhadap pendedahan data peribadi murid. 
*   **Larangan Keras PII:** Sebarang data murid sebenar (Nama Pelajar sebenar, No. MyKid, Alamat Rumah, No. Telefon) **DILARANG SEKERAS-KERASNYA** ditulis secara terus (*hardcoded*) di dalam kod sumber.
*   Data sandaran luar talian di dalam kod hanya dibenarkan menggunakan data contoh yang disanitasi sepenuhnya (contoh: *Pelajar Contoh Alif*, MyKid: *000000000001*).

---

## 🤝 Sumbangan (Contributing)

Kami mengalu-alukan sumbangan daripada para pendidik, pembangun, dan pengkaji bebas! 
1.  **Fork** repositori ini.
2.  Cipta branch kemas kini anda (`git checkout -b feature/InovasiBaru`).
3.  Ubah suai kod dan pastikan tiada ralat kompilasi.
4.  Hantar **Pull Request** berserta laporan/bukti kajian bilik darjah yang menyokong.

---

## 📄 Lesen (License)

Projek ini dilesenkan di bawah **Lesen MIT** - lihat fail [LICENSE](LICENSE) untuk maklumat lanjut.

**"Membetulkan yang biasa, membiasakan yang betul — Memacu Pendidikan Jawi ke Era Digital."** 🚀 Jom martabatkan Jawi!
