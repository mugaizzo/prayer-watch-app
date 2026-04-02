package com.prayerwatch.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PrayerWatchTypography {
    val currentTime = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurface
    )
    val nextPrayerLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = OnSurfaceVariant
    )
    val nextPrayerName = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = NextPrayerHighlight
    )
    val nextPrayerTime = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = NextPrayerHighlight
    )
    val countdown = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = AccentGold
    )
    val prayerRow = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = OnSurface
    )
    val prayerRowActive = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = NextPrayerHighlight
    )
}
