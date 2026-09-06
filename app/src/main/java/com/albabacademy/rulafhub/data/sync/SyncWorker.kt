package com.albabacademy.rulafhub.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.albabacademy.rulafhub.data.local.RuLaFDatabase
import com.albabacademy.rulafhub.data.remote.RetrofitClient
import com.albabacademy.rulafhub.data.remote.SupabaseGradeDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("RuLaF_Sync", "Penyelarasan automatik bermula...")

            val db = RuLaFDatabase.getDatabase(applicationContext)
            val dao = db.rulafDao()

            // 1. Dapatkan semua rekod tempatan yang belum diselaraskan
            val rekodBelumSync = dao.dapatkanRekodBelumSync()

            if (rekodBelumSync.isEmpty()) {
                Log.d("RuLaF_Sync", "Tiada data baharu untuk diselaraskan.")
                return@withContext Result.success()
            }

            // 2. Tukarkan format Entity kepada DTO untuk Supabase
            val payload = rekodBelumSync.map { entity ->
                SupabaseGradeDto(
                    mykid = entity.mykid,
                    nama_murid = "-", // Maklumat ini biasanya sudah ada di DB Supabase, kita hantar placeholder jika perlu
                    kelas_id = "-",
                    bulan_tahun = entity.bulanTahun,
                    markah_jawi = entity.markahJawi,
                    kehadiran = entity.kehadiran,
                    bacaan_quran = entity.bacaanQuran,
                    hafazan = entity.hafazan
                )
            }

            // 3. Hantar ke API Supabase menggunakan Upsert (Gabung/Kemaskini)
            val response = RetrofitClient.api.upsertMarkahPelajar(payload = payload)

            if (response.isSuccessful) {
                // 4. Jika berjaya, tandakan semua rekod tersebut sebagai 'Synced' di SQLITE
                rekodBelumSync.forEach { entity ->
                    dao.setelSyncSatuMurid(entity.mykid, entity.bulanTahun)
                }
                Log.d("RuLaF_Sync", "Penyelarasan berjaya! ${rekodBelumSync.size} rekod dikemaskini.")
                Result.success()
            } else {
                Log.e("RuLaF_Sync", "Ralat API: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("RuLaF_Sync", "Kegagalan kritikal penyelarasan: ${e.message}")
            Result.failure()
        }
    }
}
