package com.prayerwatch.mobile.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {

    @GET("v1/timingsByCity/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 3,
        @Query("methodSettings") methodSettings: String? = null,
        @Query("school") school: Int = 0
    ): PrayerTimesResponse

    companion object {
        const val BASE_URL = "https://api.aladhan.com/"
    }
}
