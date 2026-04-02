package com.prayerwatch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.prayerwatch.domain.model.DailyPrayerTimes
import com.prayerwatch.domain.model.PrayerTime
import com.prayerwatch.presentation.theme.*
import com.prayerwatch.utils.TimeUtils

/**
 * Main watch face showing current time, next prayer, and countdown.
 */
@Composable
fun WatchFaceScreen(
    currentTime: String,
    nextPrayer: PrayerTime?,
    nextPrayerIndex: Int,
    countdown: String,
    dailyPrayerTimes: DailyPrayerTimes,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            // Current time
            Text(
                text = currentTime,
                style = PrayerWatchTypography.currentTime,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Next prayer label
            Text(
                text = "Next Prayer",
                style = PrayerWatchTypography.nextPrayerLabel,
                textAlign = TextAlign.Center
            )

            // Next prayer name
            if (nextPrayer != null) {
                Text(
                    text = nextPrayer.name,
                    style = PrayerWatchTypography.nextPrayerName,
                    textAlign = TextAlign.Center
                )

                // Next prayer time (12h format)
                Text(
                    text = TimeUtils.to12HourFormat(nextPrayer.time),
                    style = PrayerWatchTypography.nextPrayerTime,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Countdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⏱ $countdown",
                    style = PrayerWatchTypography.countdown,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * All prayer times list screen.
 */
@Composable
fun AllPrayerTimesScreen(
    dailyPrayerTimes: DailyPrayerTimes,
    nextPrayerIndex: Int,
    cityLabel: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Prayer Times",
            style = PrayerWatchTypography.nextPrayerLabel,
            textAlign = TextAlign.Center
        )
        if (cityLabel.isNotBlank()) {
            Text(
                text = cityLabel,
                style = PrayerWatchTypography.nextPrayerLabel,
                textAlign = TextAlign.Center
            )
        }

        dailyPrayerTimes.asList().forEachIndexed { index, prayer ->
            PrayerTimeRow(
                prayer = prayer,
                isNext = index == nextPrayerIndex
            )
        }
    }
}

@Composable
fun PrayerTimeRow(
    prayer: PrayerTime,
    isNext: Boolean
) {
    val bgColor = if (isNext) SurfaceDark else Color.Transparent
    val textStyle = if (isNext) PrayerWatchTypography.prayerRowActive else PrayerWatchTypography.prayerRow
    val indicator = if (isNext) "▶ " else "   "

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$indicator${prayer.name}",
            style = textStyle
        )
        Text(
            text = TimeUtils.to12HourFormat(prayer.time),
            style = textStyle
        )
    }
}

/**
 * Loading indicator screen.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🕌",
                style = PrayerWatchTypography.nextPrayerName
            )
            Text(
                text = "Loading...",
                style = PrayerWatchTypography.nextPrayerLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error screen.
 */
@Composable
fun ErrorScreen(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = PrayerWatchTypography.nextPrayerName
            )
            Text(
                text = message,
                style = PrayerWatchTypography.nextPrayerLabel,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
    }
}

/**
 * Not-configured screen – shown when no city/settings have been synced from the phone app yet.
 */
@Composable
fun NotConfiguredScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📱",
                style = PrayerWatchTypography.nextPrayerName
            )
            Text(
                text = "Open Prayer Watch on your phone to configure",
                style = PrayerWatchTypography.nextPrayerLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}
