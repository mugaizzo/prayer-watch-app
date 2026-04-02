package com.prayerwatch.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {

    /**
     * Fetch prayer times for a specific date by city name.
     *
     * @param date            Date in DD-MM-YYYY format
     * @param city            City name (spaces OK; Retrofit URL-encodes them)
     * @param country         ISO country code, e.g. "US"
     * @param method          Calculation method id (see companion constants)
     * @param methodSettings  Custom angle overrides "fajrAngle,maghribAngle,ishaAngle"
     *                        (use "null" for fields you don't want to override)
     * @param school          0 = Shafi (standard Asr), 1 = Hanafi
     */
    @GET("v1/timingsByCity/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = METHOD_MWL,
        @Query("methodSettings") methodSettings: String? = null,
        @Query("school") school: Int = 0
    ): PrayerTimesResponse

    companion object {
        const val BASE_URL = "https://api.aladhan.com/"

        // Common calculation methods
        const val METHOD_JAFARI = 0       // Shia Ithna-Ashari, Leva Institute, Qum
        const val METHOD_KARACHI = 1      // University of Islamic Sciences, Karachi
        const val METHOD_ISNA = 2         // Islamic Society of North America
        const val METHOD_MWL = 3          // Muslim World League
        const val METHOD_MAKKAH = 4       // Umm Al-Qura University, Makkah
        const val METHOD_EGYPT = 5        // Egyptian General Authority of Survey
        const val METHOD_TEHRAN = 7       // Institute of Geophysics, University of Tehran
        const val METHOD_GULF = 8         // Gulf Region
        const val METHOD_KUWAIT = 9       // Kuwait
        const val METHOD_QATAR = 10       // Qatar
        const val METHOD_SINGAPORE = 11   // Majlis Ugama Islam Singapura
        const val METHOD_FRANCE = 12      // Union Organization Islamic de France
        const val METHOD_TURKEY = 13      // Diyanet İşleri Başkanlığı, Turkey
        const val METHOD_RUSSIA = 14      // Spiritual Administration of Muslims of Russia
        const val METHOD_MOONSIGHTING = 15 // Moonsighting Committee Worldwide
        const val METHOD_DUBAI = 16       // Dubai (experimental)
        const val METHOD_JAKIM = 17       // Jabatan Kemajuan Islam Malaysia
        const val METHOD_TUNISIA = 18     // Tunisian Ministry of Religious Affairs
        const val METHOD_ALGERIA = 19     // Algerian Ministry of Religious Affairs
        const val METHOD_KEMENAG = 20     // Kementerian Agama, Indonesia
        const val METHOD_MOROCCO = 21     // Moroccan Ministry of Habous
        const val METHOD_COMUNIDADE = 22  // Comunidade Islâmica de Lisboa
        const val METHOD_JORDAN = 23      // Ministry of Awqaf, Jordan
    }
}
