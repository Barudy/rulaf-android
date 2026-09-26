package com.albabacademy.rulafhub.data.remote

import retrofit2.Response
import com.albabacademy.rulafhub.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

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
    val no_tel: String? = "",
    val merit: Int? = 0,
    val demerit: Int? = 0
)

data class LeaderboardDto(
    val id: Long? = null,
    val mykid: String,
    val nama_murid: String,
    val skor: Int,
    val level_capai: Int,
    val jawapan_betul: Int,
    val jawapan_salah: Int,
    val tarikh: String? = null,
    // Aggregated fields (from v_leaderboard_terkumpul view)
    val total_skor: Int? = null,
    val level_tertinggi: Int? = null,
    val total_betul: Int? = null,
    val total_salah: Int? = null,
    val bilangan_sesi: Int? = null,
    val tarikh_terkini: String? = null
)

data class UserProfileDto(
    val email: String? = null,
    val nama: String? = null,
    val umur: Int? = null,
    val jantina: String? = null,
    val peranan: String? = "Murid",
    val mykid: String? = null,
    val profile_picture_url: String? = null,
    val token_id: String? = null,
    val kelas: String? = null,
    val no_tel_ibu: String? = null,
    val no_tel_bapa: String? = null
)

data class RulafVideoDto(
    val id: Long? = null,
    val tajuk_video: String = "",
    val pautan_video: String = "",
    val kategori: String? = "Jawi",
    val pencipta: String? = "",
    val is_aktif: Boolean = true,
    val created_at: String? = null
)

data class RulafBoxEdaranDto(
    val id: Long? = null,
    val token_id: String? = "",
    val mykid: String? = "",
    val nama_murid: String? = "",
    val bahan_bbm_id: Long? = null,
    val bahan_bbm_tajuk: String? = "",
    val tarikh_edaran: String? = null,
    val status_siap_modul: Boolean = false,
    val kaedah: String? = "nfc",
    val diimbas_oleh: String? = "",
    val created_at: String? = null
)

data class LoginBody(
    val email: String,
    val password: String
)

data class UpdatePasswordBody(
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

data class VersiAppDto(
    val id: Int,
    val min_version_code: Int,
    val latest_version_name: String,
    val is_force_update: Boolean,
    val update_url: String,
    val changelog: String?
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

    @GET("rest/v1/data_murid")
    suspend fun getStudentByMykid(
        @Query("mykid") mykidQuery: String,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<StudentDto>

    @GET("rest/v1/profil_pengguna")
    suspend fun getUserProfile(
        @Query("email") emailQuery: String,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<UserProfileDto>

    @GET("rest/v1/profil_pengguna")
    suspend fun getUserProfileByTokenId(
        @Query("token_id") tokenQuery: String,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<UserProfileDto>

    @GET("rest/v1/rulaf_videos?select=*&is_aktif=eq.true&order=created_at.desc")
    suspend fun getRulafVideos(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<RulafVideoDto>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_videos")
    suspend fun publishRulafVideo(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: RulafVideoDto
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulafbox_edaran")
    suspend fun logRulafBoxEdaran(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: RulafBoxEdaranDto
    ): Response<Unit>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body body: LoginBody,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY
    ): Response<AuthResponseDto>

    @Headers("Content-Type: application/json")
    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") auth: String,
        @Body body: UpdatePasswordBody,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY
    ): Response<UserDto>

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

    @GET("rest/v1/rekod_kerajinan_harian")
    suspend fun getRekodKehadiranHariIni(
        @Query("tarikh") tarikhQuery: String,
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

    // Leaderboard RPG - Individual entries (existing)
    @GET("rest/v1/rulaf_leaderboard?order=skor.desc&limit=10")
    suspend fun getLeaderboard(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<LeaderboardDto>

    // Leaderboard Terkumpul (Aggregated) - Carta Juara RuLaF
    @GET("rest/v1/v_leaderboard_terkumpul?order=total_skor.desc&limit=10")
    suspend fun getLeaderboardTerkumpul(
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

    @Headers(
        "Content-Type: application/json",
        "Prefer: return=representation"
    )
    @POST("rest/v1/rulaf_notifikasi")
    suspend fun createNotification(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body body: NotifikasiDto
    ): Response<NotifikasiDto>

    @GET("rest/v1/rulaf_versi_app?id=eq.1&select=*")
    suspend fun getAppVersion(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<VersiAppDto>

    @Multipart
    @POST("storage/v1/object/modul-rulaf/profil-pengguna/{fileName}")
    suspend fun uploadProfilePicture(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Part fileName: String,
        @Part("file") file: MultipartBody.Part
    ): Response<Unit>
}

// =====================================================================
// 🚀 RETROFIT CLIENT SINGLETON
// =====================================================================

object RetrofitClient {
    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        builder.build()
    }

    val api: SyncApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)
    }
}