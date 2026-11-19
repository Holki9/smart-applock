package com.holki9.applock.data

import java.util.concurrent.ConcurrentHashMap

object AppLockState {

    // Map packageName -> timestamp when it was unlocked
    private val unlockedPackages = ConcurrentHashMap<String, Long>()

    // Currently foreground app
    @Volatile
    var currentForegroundPackage: String? = null

    // Previous foreground app
    @Volatile
    var previousForegroundPackage: String? = null

    // Debounce to prevent multiple LockActivity triggers in quick succession
    @Volatile
    var lastLockTriggerTime: Long = 0L

    fun markUnlocked(packageName: String) {
        unlockedPackages[packageName] = System.currentTimeMillis()
    }

    fun lockAgain(packageName: String) {
        unlockedPackages.remove(packageName)
    }

    fun isTemporarilyUnlocked(packageName: String, policy: RelockPolicy): Boolean {
        val unlockTime = unlockedPackages[packageName] ?: return false
        val now = System.currentTimeMillis()

        return when (policy) {
            RelockPolicy.IMMEDIATELY -> {
                // If the app is currently in foreground, it remains unlocked until user leaves it
                currentForegroundPackage == packageName
            }
            RelockPolicy.SCREEN_OFF -> {
                // Unlocked until screen turns off
                true
            }
            RelockPolicy.TIMEOUT_1_MIN -> {
                val elapsed = now - unlockTime
                elapsed <= 60_000L
            }
            RelockPolicy.TIMEOUT_5_MIN -> {
                val elapsed = now - unlockTime
                elapsed <= 300_000L
            }
        }
    }

    fun onPackageChanged(newPackage: String, policy: RelockPolicy) {
        val old = currentForegroundPackage
        if (old != null && old != newPackage) {
            previousForegroundPackage = old
            if (policy == RelockPolicy.IMMEDIATELY) {
                unlockedPackages.remove(old)
            }
        }
        currentForegroundPackage = newPackage
    }

    fun onScreenOff() {
        unlockedPackages.clear()
    }
}
