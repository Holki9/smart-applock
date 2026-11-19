package com.holki9.applock

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.service.ScreenReceiver

class AppLockApplication : Application() {

    private val screenReceiver = ScreenReceiver()

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.getInstance(this)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }
}
