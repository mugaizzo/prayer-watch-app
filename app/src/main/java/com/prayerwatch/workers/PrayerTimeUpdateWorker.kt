package com.prayerwatch.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerwatch.data.repository.PrayerRepository

/**
 * Background worker that refreshes prayer times once per day.
 * Scheduled by WorkManager in MainViewModel.
 */
class PrayerTimeUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = PrayerRepository(applicationContext)
        if (!repository.isConfigured()) return Result.success() // nothing to refresh yet

        return try {
            val result = repository.getPrayerTimes(forceRefresh = true)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "prayer_time_update"
    }
}
