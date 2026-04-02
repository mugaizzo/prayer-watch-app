package com.prayerwatch.presentation.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.prayerwatch.data.repository.PrayerRepository
import com.prayerwatch.domain.model.DailyPrayerTimes
import com.prayerwatch.utils.TimeUtils
import java.time.ZonedDateTime

/**
 * System watch face service.  Declared in the manifest with BIND_WALLPAPER permission
 * and the WallpaperService intent filter so it appears in the watch-face picker.
 *
 * Layout (on a ~390 px round display):
 *   • Current time        – top centre
 *   • 5 prayer rows       – centre, next prayer highlighted in green with countdown
 */
class PrayerWatchFaceService : WatchFaceService() {

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = PrayerRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository
        )
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}

// ---------------------------------------------------------------------------

private class PrayerRenderer(
    private val context: android.content.Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer2<Renderer.SharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 30_000L,  // redraw every 30 s
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    // ---- Paints -------------------------------------------------------

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    private val prayerNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.LEFT
    }
    private val prayerTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.RIGHT
    }
    private val nextPrayerNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80CBC4") // teal highlight
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    private val nextPrayerTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80CBC4")
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    private val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFC107") // amber
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }
    private val dimTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }

    // ---- Shared assets -----------------------------------------------

    override suspend fun createSharedAssets(): SharedAssets = object : SharedAssets {
        override fun onDestroy() {}
    }

    // ---- Render -------------------------------------------------------

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        canvas.drawColor(Color.BLACK)

        val cx = bounds.exactCenterX()
        val repository = PrayerRepository(context)
        val prayerTimes = repository.getCachedPrayerTimes()

        // ---- Current time ----
        val hour = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val timeStr = String.format("%d:%02d", hour, minute)
        if (isAmbient) {
            timePaint.color = Color.WHITE
        }
        canvas.drawText(timeStr, cx, bounds.top + 70f, timePaint)

        if (prayerTimes == null) {
            canvas.drawText("Open phone app", cx, bounds.exactCenterY(), labelPaint)
            canvas.drawText("to configure", cx, bounds.exactCenterY() + 28f, labelPaint)
            return
        }

        drawPrayers(canvas, bounds, cx, prayerTimes, isAmbient)
    }

    private fun drawPrayers(
        canvas: Canvas,
        bounds: Rect,
        cx: Float,
        prayerTimes: DailyPrayerTimes,
        isAmbient: Boolean
    ) {
        val prayers = prayerTimes.asList()
        val nextInfo = TimeUtils.getNextPrayerInfo(prayers.map { it.timeInMillis })
        val nextIndex = nextInfo?.first ?: -1
        val countdown = if (nextInfo != null) TimeUtils.formatCountdown(nextInfo.second) else ""

        val leftX = cx - 80f
        val rightX = cx + 80f
        val startY = bounds.top + 105f
        val rowHeight = 42f

        prayers.forEachIndexed { i, prayer ->
            val y = startY + i * rowHeight
            val isNext = i == nextIndex

            if (isAmbient) {
                // Ambient mode: muted colours
                val ap = Paint(dimPaint).also { it.color = if (isNext) Color.LTGRAY else Color.DKGRAY }
                val atp = Paint(dimTimePaint).also { it.color = ap.color }
                canvas.drawText(prayer.name, leftX, y, ap)
                canvas.drawText(TimeUtils.to12HourFormat(prayer.time), rightX, y, atp)
            } else {
                if (isNext) {
                    canvas.drawText(prayer.name, leftX, y, nextPrayerNamePaint)
                    canvas.drawText(TimeUtils.to12HourFormat(prayer.time), rightX, y, nextPrayerTimePaint)
                } else {
                    canvas.drawText(prayer.name, leftX, y, prayerNamePaint)
                    canvas.drawText(TimeUtils.to12HourFormat(prayer.time), rightX, y, prayerTimePaint)
                }
            }
        }

        // Countdown below last prayer row
        if (countdown.isNotBlank() && !isAmbient) {
            val countdownY = startY + prayers.size * rowHeight + 8f
            canvas.drawText("⏱ $countdown", cx, countdownY, countdownPaint)
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
        complicationSlotsManager.complicationSlots.values.forEach { slot ->
            if (slot.enabled) slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
        }
    }
}
