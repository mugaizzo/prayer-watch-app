package com.prayerwatch.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prayerwatch.data.model.ApiSettings
import com.prayerwatch.data.repository.PrayerRepository
import com.prayerwatch.domain.model.DailyPrayerTimes
import com.prayerwatch.domain.model.PrayerTime
import com.prayerwatch.utils.TimeUtils
import com.prayerwatch.workers.PrayerTimeUpdateWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class PrayerUiState {
    object Loading : PrayerUiState()
    data class Success(
        val dailyPrayerTimes: DailyPrayerTimes,
        val nextPrayer: PrayerTime?,
        val nextPrayerIndex: Int,
        val countdown: String,
        val currentTime: String,
        val settings: ApiSettings
    ) : PrayerUiState()
    data class Error(val message: String) : PrayerUiState()
    object NotConfigured : PrayerUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrayerRepository(application)

    private val _uiState = MutableStateFlow<PrayerUiState>(PrayerUiState.Loading)
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        scheduleBackgroundWork()
        loadPrayerTimes()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = PrayerUiState.Loading

            if (!repository.isConfigured()) {
                _uiState.value = PrayerUiState.NotConfigured
                return@launch
            }

            val settings = repository.getSavedSettings()
            val result = repository.getPrayerTimes(settings)

            result.fold(
                onSuccess = { dailyTimes ->
                    startCountdownTicker(dailyTimes, settings)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to fetch prayer times", e)
                    _uiState.value = PrayerUiState.Error(
                        e.localizedMessage ?: "Failed to load prayer times"
                    )
                }
            )
        }
    }

    private fun startCountdownTicker(dailyTimes: DailyPrayerTimes, settings: ApiSettings) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                updateState(dailyTimes, settings)
                val nextInfo = TimeUtils.getNextPrayerInfo(dailyTimes.asList().map { it.timeInMillis })
                val remainingMs = nextInfo?.second ?: Long.MAX_VALUE
                delay(if (remainingMs < 60_000L) 1_000L else 30_000L)
            }
        }
    }

    private fun updateState(dailyTimes: DailyPrayerTimes, settings: ApiSettings) {
        val prayers = dailyTimes.asList()
        val nextInfo = TimeUtils.getNextPrayerInfo(prayers.map { it.timeInMillis })

        val (nextPrayer, nextPrayerIndex, countdown) = if (nextInfo != null) {
            Triple(prayers[nextInfo.first], nextInfo.first, TimeUtils.formatCountdown(nextInfo.second))
        } else {
            Triple(prayers[0], 0, "Tomorrow")
        }

        _uiState.value = PrayerUiState.Success(
            dailyPrayerTimes = dailyTimes,
            nextPrayer = nextPrayer,
            nextPrayerIndex = nextPrayerIndex,
            countdown = countdown,
            currentTime = TimeUtils.getCurrentTimeString(),
            settings = settings
        )
    }

    private fun scheduleBackgroundWork() {
        val workRequest = PeriodicWorkRequestBuilder<PrayerTimeUpdateWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                PrayerTimeUpdateWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
