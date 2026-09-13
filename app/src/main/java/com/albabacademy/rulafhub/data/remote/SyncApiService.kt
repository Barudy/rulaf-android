package com.albabacademy.rulafhub.data.remote

import retrofit2.Response
import com.albabacademy.rulafhub.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import com.google.gson.JsonElement

// =====================================================================
// 🔑 KONFIGURASI SUPABASE (Gantikan nilai sebenar Anon Key anda)
// =====================================================================
const val SUPABASE_BASE_URL = BuildConfig.SUPABASE_URL
const val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

// =====================================================================
// 📦 SEMUA DTO (DATA TRANSFER OBJECTS)
// =====================================================================

data class StudentDto(
    val mykid: String,
    val nama_murid: String,
    val jantina: String,
    val kelas_id: String,
    val tahap: String? = "RuLaF Ba",
    val no_tel: String? = ""
)

data class LeaderboardDto(
    val id: Long? = null,
    val mykid: String,
    val nama_murid: String,
    val skor: Int,
    val level_capai: Int,
    val jawapan_betul: Int,
    val jawapan_salah: Int,
    val tarikh: String? = null
)

data class UserProfileDto(
    val email: String? = null,
    val nama: String? = null,
    val umur: Int? = null,
    val jantina: String? = null,
    val peranan: String? = "Murid",
    val mykid: String? = null
)

data class LoginBody(
    val email: String,
    val password: String
)

data class AuthResponseDto(
    val access_token: String? = null,
    val user: UserDto? = null
)

data class UserDto(
    val id: String? = null,
    val email: String? = null
)

data class MarkahMuridDto(
    val id: Long? = null,
    val mykid: String? = null,
    val nama_murid: String? = null,
    val kelas_id: String? = null,
    val bulan_tahun: String? = null,
    val markah_jawi: Double? = null,
    val ujian_bertulis: Double? = null,
    val kehadiran: Double? = null,
    val hari_hadir: Double? = null,
    val jumlah_hari_sekolah: Double? = null,
    val akhlak: Double? = null,
    val kerajinan_usaha: Double? = null,
    val kerjasama_kumpulan: Double? = null,
    val bacaan_quran: String? = null,
    val hafazan: String? = null,
    val tahap_rulaf: String? = null
)

data class RekodKerajinanDto(
    val tarikh: String,
    val mykid: String,
    val subjek: String = "Jawi",
    val tugasan_siap: Int,
    val status_hadir: Boolean = true,
    val status_kehadiran: String? = null,
    val catatan: String? = null
)

data class BbmDto(
    val id: Int,
    val tajuk: String,
    val pautan: String? = null,
    val penyumbang: String? = null,
    val subjek: String? = null,
    val darjah: String? = null,
    val topik: String? = null,
    val is_folder: Boolean? = false,
    val parent_id: Int? = null,
    val readme_text: String? = null
)

data class ForumDto(
    val id: Int,
    val tajuk: String,
    val soalan: String,
    val penulis: String,
    val kategori: String? = null,
    val tarikh: String? = null
)

data class CommentDto(
    val id: Int? = null,
    val forum_id: Int,
    val komen: String,
    val penulis: String,
    val tarikh: String? = null
)

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

data class NotifikasiDto(
    val id: Long,
    val tajuk: String,
    val mesej: String,
    val kategori: String? = "hebahan",
    val sasaran: String? = "Semua",
    val pautan_tindakan: String? = null,
    val is_aktif: Boolean = true,
    val created_at: String? = null
)

data class HantarKerajinanRequest(
    val tarikh: String,
    val mykid: String,
    val subjek: String = "Jawi",
    val tugasan_siap: Int,
    val status_hadir: Boolean = true,
    val status_kehadiran: String? = null,
    val catatan: String? = null
)

data class QuizDto(
    val id: Int,
    val tajuk: String,
    val subjek: String,
    val deskripsi: String?,
    val darjah: String?,
    val soalan: JsonElement? = null // JSON string mengandungi soalan bertahap
)

data class TambahBbmRequest(
    val tajuk: String,
    val pautan: String? = null,
    val penyumbang: String,
    val subjek: String? = "Jawi",
    val darjah: String? = "Darjah 3",
    val topik: String? = "",
    val is_folder: Boolean = false,
    val parent_id: Int? = null,
    val readme_text: String? = null,
    val status: String = "approved"
)

data class TambahForumRequest(
    val tajuk: String,
    val soalan: String,
    val penulis: String,
    val subjek: String = "Sistem",
    val darjah: String = "Semua",
    val kategori: String = "BUG"
)

// =====================================================================
// 🌐 INTERFACE API SUPABASE
// =====================================================================

interface SyncApiService {

    @GET("rest/v1/data_murid")
    suspend fun getStudents(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<StudentDto>

    @GET("rest/v1/profil_pengguna")
    suspend fun getUserProfile(
        @Query("email") emailQuery: String,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<UserProfileDto>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body body: LoginBody,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY
    ): Response<AuthResponseDto>

    @GET("rest/v1/markah_murid")
    suspend fun getStudentGrades(
        @Query("mykid") mykidQuery: String,
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: Int = 1,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<MarkahMuridDto>

    @GET("rest/v1/markah_murid?select=mykid,tahap_rulaf")
    suspend fun getAllStudentGrades(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<MarkahMuridDto>

    @GET("rest/v1/rekod_kerajinan_harian")
    suspend fun getRekodKerajinanMurid(
        @Query("mykid") mykidQuery: String,
        @Query("order") order: String = "tarikh.asc",
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<RekodKerajinanDto>

    @GET("rest/v1/rulaf_kuiz")
    suspend fun getQuizzes(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<QuizDto>

    @GET("rest/v1/rulaf_repo")
    suspend fun getBbmMaterials(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<BbmDto>

    @GET("rest/v1/rulaf_forum?select=*&order=created_at.desc")
    suspend fun getForumThreads(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<ForumDto>

    @GET("rest/v1/rulaf_komen?select=*&order=created_at.asc")
    suspend fun getComments(
        @Query("forum_id") forumIdFilter: String, // cth: "eq.1"
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<CommentDto>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_komen")
    suspend fun postComment(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body comment: CommentDto
    ): Response<Unit>

    @Headers(
        "Content-Type: application/json",
        "Prefer: resolution=merge-duplicates"
    )
    @POST("rest/v1/profil_pengguna")
    suspend fun upsertUserProfile(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body profile: UserProfileDto
    ): Response<Unit>

    @Headers(
        "Content-Type: application/json",
        "Prefer: resolution=merge-duplicates"
    )
    @POST("rest/v1/markah_murid")
    suspend fun upsertMarkahPelajar(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") token: String = "Bearer $SUPABASE_ANON_KEY",
        @Body payload: List<SupabaseGradeDto>
    ): Response<Unit>

    @Headers(
        "Content-Type: application/json",
        "Prefer: resolution=merge-duplicates"
    )
    @POST("rest/v1/rekod_kerajinan_harian?on_conflict=tarikh,mykid")
    suspend fun hantarRekodKerajinan(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: HantarKerajinanRequest
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_repo")
    suspend fun tambahBbm(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: TambahBbmRequest
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_forum")
    suspend fun postForumThread(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: TambahForumRequest
    ): Response<Unit>

    // Leaderboard RPG
    @GET("rest/v1/rulaf_leaderboard?order=skor.desc&limit=10")
    suspend fun getLeaderboard(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<LeaderboardDto>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_leaderboard")
    suspend fun postLeaderboard(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: LeaderboardDto
    ): Response<Unit>

    @GET("rest/v1/rulaf_notifikasi?select=*&order=created_at.desc&limit=5")
    suspend fun getActiveNotifications(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<NotifikasiDto>


}

// =====================================================================
// 🚀 RETROFIT CLIENT SINGLETON
// =====================================================================

object RetrofitClient {
    val api: SyncApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)
    }
}