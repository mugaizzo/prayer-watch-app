# Prayer Watch App

A Pixel Watch (Wear OS) application that displays Islamic prayer times using the Aladhan API.

## Features

- Display all five daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha)
- Countdown timer showing remaining time until next prayer (updates every second)
- Location-based prayer times using the [Aladhan API](https://aladhan.com/)
- Clean, circular OLED-optimised design for Pixel Watch
- Swipe to see all prayer times list
- Background daily refresh via WorkManager
- Cached prayer times for offline / low-battery scenarios

## Screenshots

| Watch Face | All Prayer Times |
|---|---|
| Current time, next prayer name & countdown | Full list with next prayer highlighted |

## Requirements

- Android Studio Hedgehog or newer
- Wear OS SDK 30+
- A Pixel Watch device or Wear OS emulator
- Google Play Services (for location)

## Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/mugaizzo/prayer-watch-app.git
   cd prayer-watch-app
   ```

2. **Open in Android Studio**

   File → Open → select the `prayer-watch-app` directory.

3. **Sync Gradle**

   Android Studio will automatically sync the Gradle files and download dependencies.

4. **Run on device / emulator**

   - Connect your Pixel Watch via Bluetooth ADB or use the Wear OS emulator.
   - Select the `app` run configuration and click ▶ Run.

## Usage

1. On first launch the app requests **location permission** – grant it for accurate prayer times.
2. The **main watch face** shows:
   - Current time (top)
   - Next prayer name & time (centre)
   - Countdown to next prayer (bottom)
3. **Swipe left** to see the full list of all five prayer times for today.
4. Prayer times refresh automatically once a day in the background.

## Architecture

```
app/src/main/java/com/prayerwatch/
├── data/
│   ├── api/              # Retrofit API interface + response models
│   └── repository/       # PrayerRepository (network + cache)
├── domain/
│   └── model/            # PrayerTime & DailyPrayerTimes data classes
├── presentation/
│   ├── components/       # Composable UI screens
│   ├── theme/            # Wear OS Compose theme (colours, typography)
│   ├── MainActivity.kt   # Entry point, permission handling
│   └── MainViewModel.kt  # State management, location, countdown ticker
├── utils/
│   └── TimeUtils.kt      # Time parsing, formatting helpers
└── workers/
    └── PrayerTimeUpdateWorker.kt  # WorkManager daily refresh
```

## API

This app uses the free [Aladhan Prayer Times API](https://aladhan.com/prayer-times-api).

**Endpoint used:**
```
GET https://api.aladhan.com/v1/timings/{date}?latitude={lat}&longitude={lng}&method={method}
```

**Calculation methods** (default: 2 – ISNA):

| ID | Method |
|----|--------|
| 1  | University of Islamic Sciences, Karachi |
| 2  | Islamic Society of North America (ISNA) |
| 3  | Muslim World League |
| 4  | Umm Al-Qura University, Makkah |
| 5  | Egyptian General Authority of Survey |

## Permissions

| Permission | Reason |
|---|---|
| `ACCESS_FINE_LOCATION` | Accurate prayer times based on coordinates |
| `ACCESS_COARSE_LOCATION` | Fallback location for prayer times |
| `INTERNET` | Fetch prayer times from Aladhan API |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule WorkManager after reboot |

## License

MIT
