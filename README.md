# 📱 RuLaFHub (Versi Android)

RuLaFHub adalah sebuah sistem repositori sumber terbuka (*open-source*) dan portal pengurusan pendidikan masa nyata (*real-time*) yang dibangunkan khusus untuk mengintegrasikan pendekatan Pembelajaran Teradun (*Blended Learning*), Gamifikasi, dan Kecerdasan Buatan (AI) di dalam kelas. Aplikasi ini bertujuan untuk memperkasakan literasi tulisan Jawi dan pengurusan akademik melalui arsitektur yang aman dan berpusat pada privasi pengguna.

## 🌟 Fitur Utama

* **Antarmuka Berbasis Peran (*Role-Based Access*):** Sistem navigasi bawah yang disesuaikan secara dinamis, memisahkan fungsionalitas antara peran Murid (berfokus pada prestasi, permainan RPG, dan Rvids) dan Guru/Admin (berfokus pada Dashboard, RBox, Cipta Video, dan Repositori).


* **RuLaFBox (RBox) & Pemindaian Hibrida:** Pusat manajemen lapangan (R&D) bagi guru untuk memindai kartu siswa fisik. Menggunakan jalur cepat NFC sebagai metode utama dan pemindaian Kode QR sebagai sandaran (*fallback*) inklusif jika sensor perangkat bermasalah.


* **Tokenisasi *Privacy-First*:** Kartu NFC dan kode QR dienkripsi menggunakan Token ID acak (contoh: `RULAF-SEC-xxxxxx`) dan tidak menyimpan data sensitif seperti Nomor MyKid atau telepon orang tua. Sistem juga memiliki generator Kode QR internal (*native in-app*) untuk mencegah kebocoran pelacakan dari pihak ketiga.


* **Repositori Open-BBM:** Manajemen folder Bahan Bantu Mengajar dengan hierarki tiga tingkat (Darjah ➔ Subjek ➔ Fail) yang memungkinkan pengunggahan file PDF langsung ke Supabase Storage dari ponsel pintar guru.


* **Modul Rvids (*Micro-Learning*):** Aliran umpan video vertikal singkat (bergaya TikTok/Shorts) untuk menyajikan tips pelafalan bahasa Arab/Jawi dan sorotan papan peringkat permainan.


* **Arked Permainan RPG & Pemantauan Prestasi:** Sistem kuis Jawi/UPKK interaktif yang terhubung langsung dengan papan peringkat (*Leaderboard Top 10*) dan klasifikasi otomatis kumpulan (RuLaF Jim, Ba, Alif) berdasarkan skor kumulatif 60% akademik dan 40% sahsiah.


* **Keamanan Ekstra:** Dilengkapi fitur autentikasi biometrik (sidik jari), peralihan antarmuka dwitema (*Dark/Light*), penguncian versi aplikasi wajib (*Force Update*), dan menu pengaturan profil yang dapat diperluas (*expandable*).



## 🛠️ Teknologi & Arsitektur

* **Platform Utama:** Android Asli (*Native App*) yang dibangunkan menggunakan **Kotlin** dan **Jetpack Compose**.


* **Basis Data Lokal:** Menggunakan **Room Database** untuk menjamin kelancaran sistem luring dan penghematan memori peranti.


* **Integrasi Awan (*Cloud Backend*):** Memanfaatkan **Supabase** (PostgreSQL, Storage, dan Auth) yang diakses melalui titik akhir (*endpoint*) API menggunakan **Retrofit**.


* **Kepatuhan Keselamatan:** Menargetkan **API Level 36 (Android 16)** sesuai dengan mandat kebijakan terbaru Google Play Store untuk pengoptimalan kinerja dan keamanan.



## 💻 Panduan Instalasi Lokal (*Local Setup*)

Untuk menjalankan dan mengembangkan RuLaFHub versi Android di mesin lokal Anda:

1. **Klon Repositori:**
```bash
git clone https://github.com/Barudy/rulaf-android.git
cd rulaf-android

```


*(Catatan: Pastikan Anda menyesuaikan nama direktori repositori dengan yang sebenarnya)*

2. **Konfigurasi Rahasia Lingkungan (*Environment Secrets*):**
Jangan pernah menyimpan kunci API publik secara langsung di dalam repositori (*hardcode*). Buat atau buka file `local.properties` di *root* proyek Anda dan tambahkan kredensial Supabase Anda:


```properties
SUPABASE_URL=https://[PROYEK-ID-ANDA].supabase.co/
SUPABASE_ANON_KEY=[KUNCI_JWT_ANON_ANDA]

```


3. **Sinkronisasi & Bangun (*Build*):**
Buka folder proyek ini di **Android Studio**, lakukan sinkronisasi Gradle (tekan *Sync Now*), dan bangun proyek melalui menu `Build > Make Project` atau tekan *Run* pada emulator.



## 🤝 Kontribusi Komunitas

Inisiatif RuLaFHub menyambut baik segala bentuk kontribusi *Pull Request* (PR) dari pengembang perangkat lunak, guru, maupun peneliti pendidikan yang mendukung ekosistem "Open Educational Ecosystem". Kontribusi diharapkan disertai dengan bukti pengalaman di kelas atau tinjauan kajian akademik yang solid sebelum digabungkan.

## 📄 Lisensi

Perangkat lunak RuLaFHub disebarkan di bawah naungan **MIT License**, yang memberikan kebebasan mutlak kepada siapa saja untuk menggunakan, memodifikasi, dan menyebarkan kembali kode asalkan tidak melanggar hak cipta dasar platform.
