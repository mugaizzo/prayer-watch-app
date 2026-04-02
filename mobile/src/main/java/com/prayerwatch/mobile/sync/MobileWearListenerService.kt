package com.prayerwatch.mobile.sync

import android.util.Log
import com.google.android.gms.wearable.WearableListenerService

/**
 * Placeholder WearableListenerService on the phone side.
 * Extend here if you need to receive confirmations or requests from the watch.
 */
class MobileWearListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "MobileWearListener"
    }

    // No data items expected from watch → phone at this time.
}
