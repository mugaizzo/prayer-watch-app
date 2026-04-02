package com.prayerwatch.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.prayerwatch.presentation.components.*
import com.prayerwatch.presentation.theme.PrayerWatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.loadPrayerTimes()
        } else {
            viewModel.setLocationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request location permission on startup
        checkAndRequestLocationPermission()

        setContent {
            PrayerWatchTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (val state = uiState) {
                    is PrayerUiState.Loading -> LoadingScreen()
                    is PrayerUiState.LocationPermissionRequired -> LocationPermissionScreen()
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
                    nextPrayerIndex = state.nextPrayerIndex
                )
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        val fineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fineLocation == PackageManager.PERMISSION_GRANTED ||
            coarseLocation == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadPrayerTimes()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
