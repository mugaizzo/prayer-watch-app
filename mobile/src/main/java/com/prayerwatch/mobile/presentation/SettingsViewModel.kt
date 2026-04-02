package com.prayerwatch.mobile.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prayerwatch.mobile.data.ApiSettings
import com.prayerwatch.mobile.data.MobilePrayerRepository
import com.prayerwatch.mobile.workers.PrayerSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MobileUiState(
    val settings: ApiSettings = ApiSettings(),
    val lastSyncTime: String = "",
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val syncSuccess: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MobilePrayerRepository(application)

    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        scheduleDailySync()
    }

    private fun loadSettings() {
        val settings = repository.getSettings()
        val lastSync = repository.getLastSyncTime()
        _uiState.value = _uiState.value.copy(
            settings = settings,
            lastSyncTime = if (lastSync > 0) formatTime(lastSync) else "Never"
        )
    }

    fun onCityChanged(city: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(city = city)
        )
    }

    fun onCountryChanged(country: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(country = country)
        )
    }

    fun onMethodChanged(method: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(method = method)
        )
    }

    fun onMethodSettingsChanged(methodSettings: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(methodSettings = methodSettings)
        )
    }

    fun onSchoolChanged(school: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(school = school)
        )
    }

    /** Triggered by pull-to-refresh: re-fetch and sync using current saved settings. */
    fun refreshPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null, syncSuccess = false)
            val settings = repository.getSettings()
            val result = repository.getPrayerTimes(settings, forceRefresh = true)
            result.fold(
                onSuccess = {
                    enqueueImmediateSync()
                    repository.setLastSyncTime()
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = true,
                        lastSyncTime = formatTime(System.currentTimeMillis())
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = e.localizedMessage ?: "Refresh failed"
                    )
                }
            )
        }
    }

    /** Save settings and immediately trigger a sync to the watch. */
    fun saveAndSync() {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            repository.saveSettings(settings)
            _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null, syncSuccess = false)

            val result = repository.getPrayerTimes(settings, forceRefresh = true)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Fetch OK, enqueuing immediate sync to watch")
                    enqueueImmediateSync()
                    repository.setLastSyncTime()
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = true,
                        lastSyncTime = formatTime(System.currentTimeMillis())
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = e.localizedMessage ?: "Sync failed"
                    )
                }
            )
        }
    }

    fun dismissSyncResult() {
        _uiState.value = _uiState.value.copy(syncSuccess = false, syncError = null)
    }

    private fun enqueueImmediateSync() {
        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(
                PrayerSyncWorker.WORK_NAME + "_now",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PrayerSyncWorker>().build()
            )
    }

    private fun scheduleDailySync() {
        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                PrayerSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PrayerSyncWorker>(24, TimeUnit.HOURS).build()
            )
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
