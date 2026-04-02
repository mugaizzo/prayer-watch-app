package com.prayerwatch.mobile.data

/**
 * All configurable parameters for the Aladhan timingsByCity API.
 * Persisted in phone SharedPreferences and synced to the watch via DataClient.
 */
data class ApiSettings(
    val city: String = "Salt Lake City",
    val country: String = "US",
    val method: Int = 3,                    // 3 = Muslim World League
    val methodSettings: String = "",        // e.g. "17.5,null,15"
    val school: Int = 0                     // 0 = Shafi, 1 = Hanafi
) {
    fun methodSettingsOrNull(): String? = methodSettings.takeIf { it.isNotBlank() }
}
