package com.prayerwatch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prayerwatch.presentation.components.*
import com.prayerwatch.presentation.theme.PrayerWatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrayerWatchTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (val state = uiState) {
                    is PrayerUiState.Loading -> LoadingScreen()
                    is PrayerUiState.NotConfigured -> NotConfiguredScreen()
                    is PrayerUiState.Error -> ErrorScreen(message = state.message)
                    is PrayerUiState.Success -> PrayerWatchPager(state = state)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @androidx.compose.runtime.Composable
    private fun PrayerWatchPager(state: PrayerUiState.Success) {
        val pagerState = rememberPagerState(pageCount = { 2 })

        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> WatchFaceScreen(
                    currentTime = state.currentTime,
                    nextPrayer = state.nextPrayer,
                    nextPrayerIndex = state.nextPrayerIndex,
                    countdown = state.countdown,
                    dailyPrayerTimes = state.dailyPrayerTimes
                )
                1 -> AllPrayerTimesScreen(
                    dailyPrayerTimes = state.dailyPrayerTimes,
                    nextPrayerIndex = state.nextPrayerIndex,
                    cityLabel = "${state.settings.city}, ${state.settings.country}"
                )
            }
        }
    }
}
