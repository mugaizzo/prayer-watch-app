# Prayer Watch App

A Pixel Watch (Wear OS) application that displays Islamic prayer times as a **watch face**, with a companion Android phone app for configuration and data syncing.

## Features

### Watch (`:app`)
- **Watch face** – displays all five daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) on the round display; next prayer highlighted with a countdown timer
- **Complications widget** – provides a SHORT_TEXT / LONG_TEXT complication data source ("Dhuhr • 2h 30m") that can be added to any Wear OS watch face
- **Offline cache** – prayer times cached in SharedPreferences; stale cache served when offline
- **Dark OLED theme** – power-efficient black background

### Phone (`:mobile`)
- **Settings UI** – configure city, country, calculation method, custom angle overrides (methodSettings), and juristic school (Shafi / Hanafi)
- **Auto daily sync** – WorkManager fetches fresh prayer times once per day and pushes to the watch
- **Manual refresh** – pull down on the settings screen to force an immediate API call and sync
- **Wearable Data Layer** – settings and prayer times sent to watch via `Wearable.DataClient`

## API

Uses the free [Aladhan Prayer Times API](https://aladhan.com/prayer-times-api).

```
GET https://api.aladhan.com/v1/timingsByCity/{date}
    ?city={city}&country={country}&method={method}
    &methodSettings={fajrAngle,maghribAngle,ishaAngle}
    &school={0|1}
```

| Parameter | Example | Description |
|---|---|---|
| `date` | `29-03-2026` | Date in DD-MM-YYYY |
| `city` | `Salt Lake City` | City name |
| `country` | `US` | ISO country code |
| `method` | `3` | Calculation method (3 = Muslim World League) |
| `methodSettings` | `17.5,null,15` | Custom Fajr/Maghrib/Isha angles (optional) |
| `school` | `0` | 0 = Shafi, 1 = Hanafi |

## Project Structure

```
prayer-watch-app/
├── app/                    Wear OS module (watch face + complications)
│   └── src/main/java/com/prayerwatch/
│       ├── data/
│       │   ├── api/        AladhanApi (timingsByCity endpoint)
│       │   ├── model/      ApiSettings
│       │   └── repository/ PrayerRepository (network + cache)
│       ├── domain/model/   PrayerTime, DailyPrayerTimes
│       ├── presentation/
│       │   ├── watchface/  PrayerWatchFaceService (Canvas renderer)
│       │   ├── complication/ PrayerComplicationService
│       │   ├── components/ Compose UI screens
│       │   ├── theme/      OLED Wear OS theme
│       │   ├── MainActivity (app-drawer entry point)
│       │   └── MainViewModel
│       ├── sync/           PrayerWearListenerService
│       ├── utils/          TimeUtils
│       └── workers/        PrayerTimeUpdateWorker (fallback daily refresh)
│
└── mobile/                 Phone companion module
    └── src/main/java/com/prayerwatch/mobile/
        ├── data/           ApiSettings, models, AladhanApi, MobilePrayerRepository
        ├── presentation/   MainActivity (settings UI), SettingsViewModel
        ├── sync/           MobileWearListenerService
        └── workers/        PrayerSyncWorker (daily fetch + push to watch)
```

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- Wear OS SDK 30+ (for the watch module)
- Android SDK 26+ (for the phone module)
- A Pixel Watch / Wear OS emulator paired with a phone

### Build & Install

1. **Clone the repository**
   ```bash
   git clone https://github.com/mugaizzo/prayer-watch-app.git
   cd prayer-watch-app
   ```

2. **Open in Android Studio**  
   File → Open → select the `prayer-watch-app` directory.

3. **Sync Gradle** – Android Studio downloads all dependencies automatically.

4. **Install both apps**
   - Select the `mobile` configuration → install on your phone
   - Select the `app` configuration → install on your Pixel Watch

### First-time Configuration

1. Open **Prayer Watch** on your phone.
2. Enter your **city**, **country code**, calculation **method**, and optionally custom angles.
3. Tap **Save & Sync to Watch** (or pull down to refresh).
4. On your watch, go to the watch-face picker and select **Prayer Times**.

## Usage

### Phone app
| Action | Result |
|---|---|
| Fill fields + tap **Save & Sync** | Saves settings, fetches prayer times, sends to watch |
| **Pull down** on the screen | Forces immediate API call + sync without changing settings |

### Watch face
| Action | Result |
|---|---|
| Select in watch-face picker | Displays all 5 prayer times; next prayer is highlighted in teal with an amber countdown |
| Tap app icon in app drawer | Opens the Compose activity with swipeable prayer-times view |
| Add complication to any watch face | Shows next prayer name + countdown (SHORT or LONG text) |

## Architecture

- **MVVM** – ViewModel + StateFlow in both modules
- **Repository pattern** – SharedPreferences cache + Retrofit network layer
- **WorkManager** – once-per-day background refresh (phone) and fallback watch refresh
- **Wearable Data Layer** – `DataClient.putDataItem()` on phone; `WearableListenerService` on watch
- **Jetpack Compose** – Compose for Wear OS on watch; Material3 Compose on phone

## Permissions

| Permission | Module | Reason |
|---|---|---|
| `INTERNET` | both | Aladhan API calls |
| `RECEIVE_BOOT_COMPLETED` | both | Re-schedule WorkManager after reboot |
| `WAKE_LOCK` | wear | Watch face keeps screen on during rendering |

## License

MIT
