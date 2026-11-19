package com.holki9.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.holki9.applock.data.AppLockState
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.LockActivity
import com.holki9.applock.util.AppHelper

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager
    private lateinit var overlayManager: LauncherOverlayManager

    private val launcherPackages = setOf(
        "com.sec.android.app.launcher",       // Samsung One UI Home
        "com.google.android.apps.nexuslauncher", // Pixel Launcher
        "com.teslacoilsw.launcher",          // Nova Launcher
        "com.miui.home",                     // MIUI Launcher
        "com.huawei.android.launcher",       // Huawei Launcher
        "com.oppo.launcher",                 // ColorOS Launcher
        "com.android.launcher",              // AOSP Launcher
        "com.android.launcher3"              // Launcher3
    )

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        overlayManager = LauncherOverlayManager(this)
        // Preload app cache in background
        Thread {
            AppHelper.getInstalledApps(this)
        }.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(packageName, event)
            }
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleViewLongClicked(packageName, event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // If on launcher, check if launcher popup showed up
                if (launcherPackages.contains(packageName) && prefs.isLauncherQuickLockEnabled()) {
                    checkLauncherPopup(event)
                }
            }
        }
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        // If user returned to home / launcher, dismiss floating quick-lock button if shown
        if (launcherPackages.contains(packageName)) {
            // Check if this window state is the popup menu
            if (prefs.isLauncherQuickLockEnabled()) {
                checkLauncherPopup(event)
            }
            return
        } else {
            overlayManager.hideOverlay()
        }

        // Ignore system ui, keyboards, own app, and lock activity
        if (packageName == "com.android.systemui" ||
            packageName == this.packageName ||
            packageName.contains("inputmethod") ||
            packageName.contains("keyboard")
        ) {
            return
        }

        val policy = prefs.getRelockPolicy()
        AppLockState.onPackageChanged(packageName, policy)

        // Check if app is locked
        if (!prefs.isAppLocked(packageName)) return

        // Check if temporarily unlocked
        if (AppLockState.isTemporarilyUnlocked(packageName, policy)) return

        // Check debounce (prevent launching LockActivity multiple times in a row)
        val now = System.currentTimeMillis()
        if (now - AppLockState.lastLockTriggerTime < 800) return
        AppLockState.lastLockTriggerTime = now

        // Trigger Lock Screen
        val appName = AppHelper.findLabelByPackage(this, packageName)
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(LockActivity.EXTRA_APP_NAME, appName)
        }
        startActivity(lockIntent)
    }

    private fun handleViewLongClicked(packageName: String, event: AccessibilityEvent) {
        if (!prefs.isLauncherQuickLockEnabled()) return
        if (!launcherPackages.contains(packageName)) return

        val source = event.source ?: return
        try {
            val label = source.text?.toString() ?: source.contentDescription?.toString()
            if (!label.isNullOrBlank()) {
                val matchedPackage = AppHelper.findPackageByLabel(label)
                if (matchedPackage != null && matchedPackage != this.packageName) {
                    val rect = Rect()
                    source.getBoundsInScreen(rect)
                    overlayManager.showQuickLockButton(matchedPackage, label, rect.top)
                    return
                }
            }

            // Also check children if text wasn't in source root
            for (i in 0 until source.childCount) {
                val child = source.getChild(i) ?: continue
                val childText = child.text?.toString() ?: child.contentDescription?.toString()
                if (!childText.isNullOrBlank()) {
                    val matchedChildPackage = AppHelper.findPackageByLabel(childText)
                    if (matchedChildPackage != null && matchedChildPackage != this.packageName) {
                        val rect = Rect()
                        child.getBoundsInScreen(rect)
                        overlayManager.showQuickLockButton(matchedChildPackage, childText, rect.top)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLauncherPopup(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        try {
            // Find any view that matches app names in launcher popup
            findAndShowQuickActionFromNode(root)
        } catch (_: Exception) {}
    }

    private fun findAndShowQuickActionFromNode(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) {
            val matchedPackage = AppHelper.findPackageByLabel(text)
            if (matchedPackage != null && matchedPackage != this.packageName) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                overlayManager.showQuickLockButton(matchedPackage, text, rect.top)
                return
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findAndShowQuickActionFromNode(child)
        }
    }

    override fun onInterrupt() {
        overlayManager.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlay()
    }
}
