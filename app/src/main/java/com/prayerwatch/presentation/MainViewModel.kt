package com.prayerwatch.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
        val currentTime: String
    ) : PrayerUiState()
    data class Error(val message: String) : PrayerUiState()
    object LocationPermissionRequired : PrayerUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrayerRepository(application)

    private val _uiState = MutableStateFlow<PrayerUiState>(PrayerUiState.Loading)
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        scheduleBackgroundWork()
    }

    /** Called when location permission is granted */
    @SuppressLint("MissingPermission")
    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = PrayerUiState.Loading

            // Check for cached location first for faster startup
            val cachedLocation = repository.getCachedLocation()
            if (cachedLocation != null) {
                fetchAndUpdateTimes(cachedLocation.first, cachedLocation.second)
            }

            // Then try to get the current location
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        repository.saveLocation(location.latitude, location.longitude)
                        viewModelScope.launch {
                            fetchAndUpdateTimes(location.latitude, location.longitude)
                        }
                    } else if (cachedLocation == null) {
                        _uiState.value = PrayerUiState.Error("Unable to determine location.")
                    }
                }.addOnFailureListener { e ->
                    if (cachedLocation == null) {
                        _uiState.value = PrayerUiState.Error(
                            "Location error: ${e.localizedMessage}"
                        )
                    }
                }
            } catch (e: SecurityException) {
                _uiState.value = PrayerUiState.LocationPermissionRequired
            }
        }
    }

    private suspend fun fetchAndUpdateTimes(latitude: Double, longitude: Double) {
        val method = repository.getSavedMethod()
        val result = repository.getPrayerTimes(latitude, longitude, method)

        result.fold(
            onSuccess = { dailyTimes ->
                startCountdownTicker(dailyTimes)
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to fetch prayer times", e)
                if (_uiState.value is PrayerUiState.Loading) {
                    _uiState.value = PrayerUiState.Error(
                        e.localizedMessage ?: "Failed to load prayer times"
                    )
                }
            }
        )
    }

    private fun startCountdownTicker(dailyTimes: DailyPrayerTimes) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                updateState(dailyTimes)
                // Update every second when < 60 s remain, otherwise every minute
                val prayers = dailyTimes.asList()
                val nextInfo = TimeUtils.getNextPrayerInfo(prayers.map { it.timeInMillis })
                val remainingMs = nextInfo?.second ?: Long.MAX_VALUE
                delay(if (remainingMs < 60_000L) 1_000L else 30_000L)
            }
        }
    }

    private fun updateState(dailyTimes: DailyPrayerTimes) {
        val prayers = dailyTimes.asList()
        val timeMillis = prayers.map { it.timeInMillis }
        val nextInfo = TimeUtils.getNextPrayerInfo(timeMillis)

        val (nextPrayer, nextPrayerIndex, countdown) = if (nextInfo != null) {
            Triple(
                prayers[nextInfo.first],
                nextInfo.first,
                TimeUtils.formatCountdown(nextInfo.second)
            )
        } else {
            // All prayers have passed – show Fajr of tomorrow
            Triple(prayers[0], 0, "Tomorrow")
        }

        _uiState.value = PrayerUiState.Success(
            dailyPrayerTimes = dailyTimes,
            nextPrayer = nextPrayer,
            nextPrayerIndex = nextPrayerIndex,
            countdown = countdown,
            currentTime = TimeUtils.getCurrentTimeString()
        )
    }

    fun setLocationPermissionDenied() {
        if (_uiState.value is PrayerUiState.Loading) {
            _uiState.value = PrayerUiState.LocationPermissionRequired
        }
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
