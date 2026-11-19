package com.holki9.applock.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.holki9.applock.data.AppLockState
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.MainActivity
import com.holki9.applock.util.AppHelper

class AppLockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = PreferencesManager.getInstance(this)
        val activePackage = AppLockState.previousForegroundPackage ?: AppLockState.currentForegroundPackage

        if (activePackage != null && activePackage != packageName) {
            val appName = AppHelper.findLabelByPackage(this, activePackage)
            val isNowLocked = prefs.toggleLock(activePackage)
            val msg = if (isNowLocked) {
                "🔒 «$appName» теперь заблокирован"
            } else {
                "🔓 «$appName» разблокирован"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updateTileState()
        } else {
            // Open main activity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ pending intent if needed or direct startActivityAndCollapse
                startActivityAndCollapse(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = PreferencesManager.getInstance(this)
        val activePackage = AppLockState.previousForegroundPackage ?: AppLockState.currentForegroundPackage

        if (activePackage != null && activePackage != packageName) {
            val appName = AppHelper.findLabelByPackage(this, activePackage)
            val isLocked = prefs.isAppLocked(activePackage)
            tile.label = if (isLocked) "Заблокирован" else "Блокировать"
            tile.subtitle = appName
            tile.state = if (isLocked) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        } else {
            tile.label = "Smart AppLock"
            tile.subtitle = "Защита активна"
            tile.state = if (prefs.isServiceEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}
