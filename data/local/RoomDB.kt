package com.albabacademy.rulafhub.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// =====================================================================
// ENTITY 1: STUDENT DATA MASTER
// =====================================================================
@Entity(tableName = "data_murid")
data class StudentEntity(
    @PrimaryKey
    val mykid: String, // 12 Digit Nombor MyKid sebagai Kunci Utama
    val namaMurid: String,
    val jantina: String,
    val kelasId: String
)

// =====================================================================
// ENTITY 2: GRADES & SAHSIAH RECORDS (COMPOSITE SCORE FOR 60:40)
// =====================================================================
@Entity(
    tableName = "markah_murid",
    primaryKeys = ["mykid", "bulanTahun"]
)
data class GradeEntity(
    val mykid: String,
    val bulanTahun: String, // Contoh: "Ogos 2026"
    val markahJawi: Int,    // Skor Akademik (Wajaran 60%)
    val kehadiran: Int,     // Skor Sahsiah / Kehadiran (Wajaran 40%)
    val bacaanQuran: String, // Status Bacaan (Lancar/Sederhana/Lemah)
    val hafazan: String,    // Gred Ujian Hafazan (A, B, C, D)
    val isSynced: Boolean = false // Status penyelarasan ke Supabase
)

// =====================================================================
// DATA ACCESS OBJECT (DAO) FOR OFFLINE-FIRST OPERATIONS
// =====================================================================
@Dao
interface RuLaFDao {

    // --- PENGURUSAN DATA MURID (MASTER SENARAI) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenaraiMurid(senarai: List<StudentEntity>)

    @Query("SELECT * FROM data_murid WHERE kelasId = :kelasId ORDER BY namaMurid ASC")
    fun dapatkanMuridIkutKelas(kelasId: String): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM data_murid")
    suspend fun dapatkanJumlahMurid(): Int

    // --- PENGURUSAN DATA PENTAKSIRAN (MARKAH & SAHSIAH) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpanMarkahOffline(grade: GradeEntity)

    @Query("""
        SELECT * FROM markah_murid 
        WHERE mykid = :mykid AND bulanTahun = :bulanTahun 
        LIMIT 1
    """)
    fun semakPentaksiranTempatan(mykid: String, bulanTahun: String): Flow<GradeEntity?>

    // --- ENJIN PENYELARASAN SUPABASE (SYNC UTILITY) ---
    @Query("SELECT * FROM markah_murid WHERE isSynced = 0")
    suspend fun dapatkanRekodBelumSync(): List<GradeEntity>

    @Query("UPDATE markah_murid SET isSynced = 1 WHERE mykid = :mykid AND bulanTahun = :bulanTahun")
    suspend fun setelSyncSatuMurid(mykid: String, bulanTahun: String)
}

// =====================================================================
// ROOM DATABASE INSTANCE (SINGLETON WITH THREAD-SAFETY)
// =====================================================================
@Database(
    entities = [StudentEntity::class, GradeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RuLaFDatabase : RoomDatabase() {

    abstract fun rulafDao(): RuLaFDao

    companion object {
        @Volatile
        private var INSTANCE: RuLaFDatabase? = null

        fun getDatabase(context: Context): RuLaFDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RuLaFDatabase::class.java,
                    "rulaf_offline_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
