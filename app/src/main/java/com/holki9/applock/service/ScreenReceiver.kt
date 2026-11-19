package com.holki9.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.holki9.applock.data.AppLockState

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            AppLockState.onScreenOff()
        }
    }
}
