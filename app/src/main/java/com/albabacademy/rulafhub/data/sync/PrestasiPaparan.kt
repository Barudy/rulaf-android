package com.albabacademy.rulafhub.data.sync

import com.albabacademy.rulafhub.data.remote.MarkahMuridDto

// Objek hasil paparan yang telah diproses (Mirip state muridDitemui pada JS)
data class PrestasiPaparan(
    val namaMurid: String = "Tiada Rekod",
    val kelasId: String = "-",
    val bulanTahun: String = "Ogos 2026",
    val tahapRulaf: String = "Belum Ditetapkan",
    val nilaiAkademik: String = "0.0%",
    val nilaiSahsiah: String = "0.0%",
    val skorAkhir: String = "0.00",
    val bacaanQuran: String = "-",
    val hafazan: String = "-"
)

// Fungsi menterjemah data mentah Supabase kepada skor holistik (60/40)
fun kiraPrestasiHolistik(data: MarkahMuridDto): PrestasiPaparan {
    // 1. PENGIRAAN AKADEMIK (JAWI & UJIAN BERTULIS)
    val ujianBertulis = data.ujian_bertulis ?: 0.0
    val markahJawi = data.markah_jawi ?: 0.0
    val purataAkademik = if (ujianBertulis > 0.0) {
        ((ujianBertulis + markahJawi) / 200.0) * 100.0
    } else {
        markahJawi // Fallback jika guru hanya masukkan markah Jawi sahaja
    }

    // 2. PENGIRAAN SAHSIAH HOLISTIK (SKALA 1-10)
    val akhlak = data.akhlak ?: 0.0
    val kerajinan = data.kerajinan_usaha ?: 0.0
    val kerjasama = data.kerjasama_kumpulan ?: 0.0
    val hariHadir = data.hari_hadir ?: data.kehadiran ?: 0.0
    val jumlahHari = (data.jumlah_hari_sekolah ?: 140.0).coerceAtLeast(1.0)

    val kehadiranSkala = ((hariHadir / jumlahHari) * 100.0) / 10.0
    val jumlahSahsiah = akhlak + kerajinan + kerjasama + kehadiranSkala
    val peratusSahsiah = (jumlahSahsiah / 40.0) * 100.0

    // 3. FORMULA GRED PURATA KUMULATIF (60/40)
    val skorKeseluruhan = (purataAkademik * 0.6) + (peratusSahsiah * 0.4)

    return PrestasiPaparan(
        namaMurid = data.nama_murid ?: "Tiada Rekod",
        kelasId = data.kelas_id ?: "-",
        bulanTahun = data.bulan_tahun ?: "Ogos 2026",
        tahapRulaf = data.tahap_rulaf ?: "Belum Ditetapkan",
        nilaiAkademik = String.format("%.1f%%", purataAkademik),
        nilaiSahsiah = String.format("%.1f%%", peratusSahsiah),
        skorAkhir = String.format("%.2f", skorKeseluruhan),
        bacaanQuran = data.bacaan_quran ?: "-",
        hafazan = data.hafazan ?: "-"
    )
}