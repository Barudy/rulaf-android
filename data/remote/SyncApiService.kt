package com.albabacademy.rulafhub.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// Data Transfer Object (DTO) untuk penhantaran data ke Supabase
data class SupabaseGradeDto(
    val mykid: String,
    val nama_murid: String,
    val kelas_id: String,
    val bulan_tahun: String,
    val markah_jawi: Int,
    val kehadiran: Int,
    val bacaan_quran: String,
    val hafazan: String
)

interface SyncApiService {

    @Headers(
        "Content-Type: application/json",
        "Prefer: resolution=merge-duplicates" // Mengaktifkan fasa Upsert automatik di Supabase
    )
    @POST("rest/v1/markah_murid")
    suspend fun upsertMarkahPelajar(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Body payload: List<SupabaseGradeDto>
    ): Response<Unit>
}