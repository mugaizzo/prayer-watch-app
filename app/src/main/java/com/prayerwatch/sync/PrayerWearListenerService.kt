package com.prayerwatch.sync

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.prayerwatch.data.model.ApiSettings
import com.prayerwatch.data.repository.PrayerRepository
import com.prayerwatch.domain.model.DailyPrayerTimes

/**
 * Listens for DataItems sent by the phone companion app via the Wearable Data Layer.
 *
 * Paths:
 *   /prayer_settings  – JSON of ApiSettings  (phone → watch)
 *   /prayer_times     – JSON of DailyPrayerTimes (phone → watch)
 */
class PrayerWearListenerService : WearableListenerService() {

    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            try {
                val dataItem = event.dataItem
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

                when (dataItem.uri.path) {
                    PATH_SETTINGS -> {
                        val json = dataMap.getString(KEY_JSON) ?: return@forEach
                        val settings = gson.fromJson(json, ApiSettings::class.java)
                        PrayerRepository(applicationContext).saveSettings(settings)
                        Log.d(TAG, "Received settings: city=${settings.city}")
                    }
                    PATH_PRAYER_TIMES -> {
                        val json = dataMap.getString(KEY_JSON) ?: return@forEach
                        val times = gson.fromJson(json, DailyPrayerTimes::class.java)
                        // Write directly into the same SharedPreferences the repository uses
                        val prefs = applicationContext
                            .getSharedPreferences(PrayerRepository.PREFS_NAME, MODE_PRIVATE)
                        prefs.edit()
                            .putString("cached_date", times.date)
                            .putString("cached_times", json)
                            .apply()
                        Log.d(TAG, "Received prayer times for ${times.date}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing data item", e)
            }
        }
    }

    companion object {
        private const val TAG = "PrayerWearListener"
        const val PATH_SETTINGS = "/prayer_settings"
        const val PATH_PRAYER_TIMES = "/prayer_times"
        const val KEY_JSON = "json"
    }
}
