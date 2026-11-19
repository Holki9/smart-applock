package com.holki9.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
        "com.sec.android.app.launcher",          // Samsung One UI Home
        "com.google.android.apps.nexuslauncher", // Pixel Launcher
        "com.teslacoilsw.launcher",             // Nova Launcher
        "com.miui.home",                        // MIUI Launcher
        "com.huawei.android.launcher",          // Huawei Launcher
        "com.oppo.launcher",                    // ColorOS Launcher
        "com.android.launcher",                 // AOSP Launcher
        "com.android.launcher3"                 // Launcher3
    )

    @Volatile
    private var lastLongClickedPackage: String? = null
    @Volatile
    private var lastLongClickedAppName: String? = null
    @Volatile
    private var lastLongClickedTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        overlayManager = LauncherOverlayManager(this)

        // Super-fast background cache init (~15ms, 0 RAM overhead)
        Thread {
            AppHelper.initPackageCache(this)
        }.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleViewLongClicked(packageName, event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(packageName, event)
            }
        }
    }

    private fun handleViewLongClicked(packageName: String, event: AccessibilityEvent) {
        if (!prefs.isLauncherQuickLockEnabled()) return
        if (!launcherPackages.contains(packageName)) return

        val source = event.source ?: return
        try {
            var rawText = source.text?.toString() ?: source.contentDescription?.toString()

            // If root source has no text, inspect immediate children (depth 1 only)
            if (rawText.isNullOrBlank()) {
                val count = minOf(source.childCount, 6)
                for (i in 0 until count) {
                    val child = source.getChild(i) ?: continue
                    val text = child.text?.toString() ?: child.contentDescription?.toString()
                    if (!text.isNullOrBlank()) {
                        rawText = text
                        break
                    }
                }
            }

            if (!rawText.isNullOrBlank()) {
                val matched = AppHelper.findPackageByLabel(rawText)
                if (matched != null && matched != this.packageName) {
                    val appName = AppHelper.findLabelByPackage(this, matched)
                    lastLongClickedPackage = matched
                    lastLongClickedAppName = appName
                    lastLongClickedTime = System.currentTimeMillis()
                    overlayManager.showQuickLockButton(matched, appName)
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()

        // 1. If on Launcher
        if (launcherPackages.contains(packageName)) {
            if (prefs.isLauncherQuickLockEnabled()) {
                // Check if a popup window just showed up
                checkLauncherPopupOnStateChange(event, now)
            }
            return
        }

        // 2. If left launcher
        overlayManager.hideOverlay()

        // Ignore system UI, keyboard, own app
        if (packageName == "com.android.systemui" ||
            packageName == this.packageName ||
            packageName.contains("inputmethod") ||
            packageName.contains("keyboard")
        ) {
            return
        }

        // 3. App Locking Logic
        val policy = prefs.getRelockPolicy()
        AppLockState.onPackageChanged(packageName, policy)

        // Instant O(1) hash set check
        if (!prefs.isAppLocked(packageName)) return

        // Check if temporarily unlocked
        if (AppLockState.isTemporarilyUnlocked(packageName, policy)) return

        // Debounce lock screen triggers
        if (now - AppLockState.lastLockTriggerTime < 800) return
        AppLockState.lastLockTriggerTime = now

        // Launch Lock Screen
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

    private fun checkLauncherPopupOnStateChange(event: AccessibilityEvent, now: Long) {
        // If an icon was long-clicked in the last 4 seconds, keep or show the button
        val lastPkg = lastLongClickedPackage
        val lastName = lastLongClickedAppName
        if (lastPkg != null && lastName != null && (now - lastLongClickedTime < 4000)) {
            overlayManager.showQuickLockButton(lastPkg, lastName)
            return
        }

        // Also inspect event.text (many launcher popups emit their title in event.text)
        try {
            val eventTexts = event.text
            if (!eventTexts.isNullOrEmpty()) {
                for (item in eventTexts) {
                    val str = item?.toString() ?: continue
                    val matched = AppHelper.findPackageByLabel(str)
                    if (matched != null && matched != this.packageName) {
                        val appName = AppHelper.findLabelByPackage(this, matched)
                        lastLongClickedPackage = matched
                        lastLongClickedAppName = appName
                        lastLongClickedTime = now
                        overlayManager.showQuickLockButton(matched, appName)
                        return
                    }
                }
            }

            // Shallow check of source node (depth at most 2, max 10 nodes)
            val source = event.source ?: return
            inspectShallowForApp(source, depth = 0)
        } catch (_: Exception) {}
    }

    private fun inspectShallowForApp(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 2) return

        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank() && text.length > 2) {
            val matched = AppHelper.findPackageByLabel(text)
            if (matched != null && matched != this.packageName) {
                val appName = AppHelper.findLabelByPackage(this, matched)
                lastLongClickedPackage = matched
                lastLongClickedAppName = appName
                lastLongClickedTime = System.currentTimeMillis()
                overlayManager.showQuickLockButton(matched, appName)
                return
            }
        }

        val childCount = minOf(node.childCount, 6)
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            inspectShallowForApp(child, depth + 1)
        }
    }

    override fun onInterrupt() {
        overlayManager.hideOverlayImmediate()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlayImmediate()
    }
}
