package com.albabacademy.rulafhub.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.albabacademy.rulafhub.data.local.RuLaFDatabase
import com.albabacademy.rulafhub.data.remote.SupabaseGradeDto
import com.albabacademy.rulafhub.data.remote.SyncApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // Kunci Kredensial Supabase (Wajib ganti dengan kredensial projek Vercel/Supabase anda)
    private val SUPABASE_URL = "https://hmbzxmougiaubiooqqk.supabase.co/" // Fallback URL
    private val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY" // Sila gantikan dengan Anon Key anda

    override suspend fun doWork(): Result {
        val database = RuLaFDatabase.getDatabase(applicationContext)
        val dao = database.rulafDao()

        // 1. Tarik semua rekod tempatan yang belum diselaraskan (isSynced = false)
        val senaraiBelumSync = dao.dapatkanRekodBelumSync()

        if (senaraiBelumSync.isEmpty()) {
            Log.d("RuLaF_SyncWorker", "Tiada rekod baharu ditemui. Penyelarasan diabaikan.")
            return Result.success()
        }

        Log.d("RuLaF_SyncWorker", "Menjumpai ${senaraiBelumSync.size} rekod belum diselaraskan. Memulakan muat naik...")

        // 2. Sediakan Client Retrofit dengan OkHttp Interceptor untuk logging
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(SyncApiService::class.java)

        // 3. Petakan (Map) rekod local Room ke DTO Supabase Cloud
        val payload = senaraiBelumSync.map { localGrade ->
            SupabaseGradeDto(
                mykid = localGrade.mykid,
                nama_murid = "Murid RuLaF", // Akan dipadankan secara dinamik di pangkalan data
                kelas_id = "3 Murshid", // Default fallback, boleh dinamikkan
                bulan_tahun = localGrade.bulanTahun,
                markah_jawi = localGrade.markahJawi,
                kehadiran = localGrade.kehadiran,
                bacaan_quran = localGrade.bacaanQuran,
                hafazan = localGrade.hafazan
            )
        }

        return try {
            // 4. Hantar data secara pukal ke Supabase REST Endpoint
            val response = apiService.upsertMarkahPelajar(
                apiKey = SUPABASE_ANON_KEY,
                token = "Bearer $SUPABASE_ANON_KEY",
                payload = payload
            )

            if (response.isSuccessful) {
                Log.d("RuLaF_SyncWorker", "Penyelarasan berjaya! Mengemas kini status Room DB lokal...")

                // 5. Kemas kini status 'isSynced = true' di dalam Room DB bagi mengelakkan hantaran bertindih
                senaraiBelumSync.forEach { localGrade ->
                    dao.setelSyncSatuMurid(localGrade.mykid, localGrade.bulanTahun)
                }

                Result.success()
            } else {
                Log.e("RuLaF_SyncWorker", "Gagal menyelaraskan data: ${response.code()} - ${response.errorBody()?.string()}")
                Result.retry() // Cuba semula kemudian sekiranya terdapat isu server seketika
            }
        } catch (e: Exception) {
            Log.e("RuLaF_SyncWorker", "Ralat rangkaian dikesan semasa operasi penyelarasan", e)
            Result.retry() // Cuba semula apabila peranti mendapat isyarat internet yang stabil
        }
    }
}