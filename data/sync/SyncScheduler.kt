package com.albabacademy.rulafhub.data.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val SYNC_WORK_TAG = "rulaf_sync_work_tag"

    fun jadualkanPenyelarasan(context: Context) {
        // Tetapkan sekatan (Constraints): Wajib ada internet aktif & bateri tidak terlalu lemah
        val sekatanInternet = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Bina tugasan berkala (Periodic Work Request) setiap 15 minit (Had minimum Android)
        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(sekatanInternet)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SYNC_WORK_TAG)
            .build()

        // Daftar tugasan ke WorkManager dengan polisi KEEP (Kekalkan penjadualan lama jika sedia ada)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "RuLaF_Periodic_Sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    // Fungsi untuk memaksa penyelarasan serta-merta (cth: Guru klik butang Manual Sync)
    fun paksaSyncSertaMerta(context: Context) {
        val sekatanSertaMerta = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(sekatanSertaMerta)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeSyncRequest)
    }
}