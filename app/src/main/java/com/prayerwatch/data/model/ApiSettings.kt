package com.prayerwatch.data.model

/**
 * All configurable parameters for the Aladhan timingsByCity API.
 * This object is persisted on both phone and watch (SharedPreferences, JSON-encoded)
 * and synced phone → watch via the Wearable Data Layer.
 */
data class ApiSettings(
    val city: String = "Salt Lake City",
    val country: String = "US",
    val method: Int = 3,                    // 3 = Muslim World League
    val methodSettings: String = "",        // e.g. "17.5,null,15"; empty = use method defaults
    val school: Int = 0                     // 0 = Shafi, 1 = Hanafi
) {
    /** Returns methodSettings as a nullable string for the API query param. */
    fun methodSettingsOrNull(): String? = methodSettings.takeIf { it.isNotBlank() }
}
