package com.prayerwatch.presentation.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.prayerwatch.data.repository.PrayerRepository
import com.prayerwatch.utils.TimeUtils

/**
 * Complication data source that provides the upcoming prayer name + countdown.
 *
 * Supported types:
 *   SHORT_TEXT  – "Dhuhr" + "2h 30m"
 *   LONG_TEXT   – "Dhuhr at 1:15 PM" + "in 2h 30m"
 *
 * The complication updates every 60 seconds (declared in manifest).
 */
class PrayerComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> buildShortText("Dhuhr", "2h 30m")
            ComplicationType.LONG_TEXT -> buildLongText("Dhuhr at 1:15 PM", "in 2h 30m")
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val repository = PrayerRepository(applicationContext)
        val prayerTimes = repository.getCachedPrayerTimes() ?: return buildNoData(request.complicationType)

        val prayers = prayerTimes.asList()
        val nextInfo = TimeUtils.getNextPrayerInfo(prayers.map { it.timeInMillis })
            ?: return buildNoData(request.complicationType)

        val nextPrayer = prayers[nextInfo.first]
        val countdown = TimeUtils.formatCountdown(nextInfo.second)
        val timeFormatted = TimeUtils.to12HourFormat(nextPrayer.time)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> buildShortText(nextPrayer.name, countdown)
            ComplicationType.LONG_TEXT -> buildLongText(
                "${nextPrayer.name} at $timeFormatted",
                "in $countdown"
            )
            else -> null
        }
    }

    private fun buildShortText(title: String, text: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Next prayer: $title in $text").build()
        )
            .setTitle(PlainComplicationText.Builder(title).build())
            .build()

    private fun buildLongText(title: String, text: String) =
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("$title $text").build()
        )
            .setTitle(PlainComplicationText.Builder(title).build())
            .build()

    private fun buildNoData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> buildShortText("—", "—")
        ComplicationType.LONG_TEXT -> buildLongText("No data", "—")
        else -> null
    }
}
