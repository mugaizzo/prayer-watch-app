package com.prayerwatch.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {

    /**
     * Fetch prayer times for a specific date.
     *
     * @param date   Date in DD-MM-YYYY format (or "today")
     * @param latitude  User latitude
     * @param longitude User longitude
     * @param method Calculation method (2 = ISNA, 4 = Umm Al-Qura, etc.)
     */
    @GET("v1/timings/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2
    ): PrayerTimesResponse

    companion object {
        const val BASE_URL = "https://api.aladhan.com/"

        // Common calculation methods
        const val METHOD_MWL = 3          // Muslim World League
        const val METHOD_ISNA = 2         // Islamic Society of North America
        const val METHOD_EGYPT = 5        // Egyptian General Authority of Survey
        const val METHOD_MAKKAH = 4       // Umm Al-Qura University, Makkah
        const val METHOD_KARACHI = 1      // University of Islamic Sciences, Karachi
        const val METHOD_TEHRAN = 7       // Institute of Geophysics, University of Tehran
        const val METHOD_JAFARI = 0       // Shia Ithna-Ashari, Leva Institute, Qum
    }
}
