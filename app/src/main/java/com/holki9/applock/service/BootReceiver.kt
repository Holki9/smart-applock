package com.holki9.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.holki9.applock.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-initialize preferences
            PreferencesManager.getInstance(context)
        }
    }
}
