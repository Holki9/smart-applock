package com.holki9.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.holki9.applock.data.AppLockState
import com.holki9.applock.data.PreferencesManager
import com.holki9.applock.ui.LockActivity
import com.holki9.applock.util.AppHelper

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager
    private lateinit var overlayManager: LauncherOverlayManager
    private val handler = Handler(Looper.getMainLooper())

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

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        overlayManager = LauncherOverlayManager(this)

        Thread {
            AppHelper.initPackageCache(this)
        }.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(packageName, event)
            }
        }
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()

        // 1. If inside Launcher (Home / App Drawer)
        if (launcherPackages.contains(packageName)) {
            // Ignore RecentsActivity (task switcher screen)
            val className = event.className?.toString() ?: ""
            if (className.contains("RecentsActivity") || className.contains("Quickstep")) {
                overlayManager.hideOverlay()
                return
            }

            if (prefs.isLauncherQuickLockEnabled()) {
                // Check immediately and also 120ms later to account for One UI popup opening animation
                checkForLauncherPopup()
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ checkForLauncherPopup() }, 120)
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

        // Instant hash set check
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

    /**
     * Finds Samsung One UI popup menu by searching for its distinctive buttons ("Выбрать" / "Select").
     * Attaches our lock button directly underneath the popup card.
     */
    private fun checkForLauncherPopup() {
        val root = rootInActiveWindow ?: return
        try {
            // Find popup by its unique buttons
            var selectNodes = root.findAccessibilityNodeInfosByText("Выбрать")
            if (selectNodes.isNullOrEmpty()) {
                selectNodes = root.findAccessibilityNodeInfosByText("Select")
            }
            if (selectNodes.isNullOrEmpty()) {
                selectNodes = root.findAccessibilityNodeInfosByText("Добавить на главный экран")
            }

            if (selectNodes.isNullOrEmpty()) {
                overlayManager.hideOverlay()
                return
            }

            val selectNode = selectNodes.firstOrNull() ?: return

            // Traverse up to find the popup container card
            var popupCard: AccessibilityNodeInfo? = selectNode.parent
            while (popupCard != null && popupCard.parent != null) {
                val p = popupCard.parent
                val pClass = p.className?.toString() ?: ""
                if (pClass.contains("DragLayer") || pClass.contains("Applist") ||
                    pClass.contains("Workspace") || pClass.contains("DecorView") ||
                    pClass.contains("Appscreen") || pClass.contains("ConstraintLayout")
                ) {
                    break
                }
                popupCard = p
            }

            if (popupCard == null) return

            val popupRect = Rect()
            popupCard.getBoundsInScreen(popupRect)

            // Extract the app title
            var detectedTitle: String? = null
            val ignoreWords = setOf(
                "выбрать", "добавить на г...", "добавить на главный экран", "удалить",
                "отключить", "виджеты", "select", "remove", "uninstall", "widgets",
                "информация о приложении", "app info"
            )

            fun findTitle(node: AccessibilityNodeInfo, depth: Int) {
                if (depth > 3 || detectedTitle != null) return
                val text = node.text?.toString() ?: node.contentDescription?.toString()
                if (!text.isNullOrBlank()) {
                    val lower = text.trim().lowercase()
                    if (!ignoreWords.contains(lower) && !lower.startsWith("добавить")) {
                        detectedTitle = text.trim()
                        return
                    }
                }
                for (i in 0 until minOf(node.childCount, 6)) {
                    val child = node.getChild(i) ?: continue
                    findTitle(child, depth + 1)
                }
            }

            findTitle(popupCard, 0)

            if (!detectedTitle.isNullOrBlank()) {
                val matchedPackage = AppHelper.findPackageByLabel(detectedTitle)
                if (matchedPackage != null && matchedPackage != this.packageName) {
                    overlayManager.showAttachedButton(matchedPackage, detectedTitle!!, popupRect)
                    return
                }
            }

            // Fallback: if title not found inside card, check direct children
            for (i in 0 until minOf(popupCard.childCount, 4)) {
                val child = popupCard.getChild(i) ?: continue
                val text = child.text?.toString()
                if (!text.isNullOrBlank() && !ignoreWords.contains(text.lowercase().trim())) {
                    val matchedPackage = AppHelper.findPackageByLabel(text)
                    if (matchedPackage != null && matchedPackage != this.packageName) {
                        overlayManager.showAttachedButton(matchedPackage, text, popupRect)
                        return
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        overlayManager.hideOverlayImmediate()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlayImmediate()
    }
}
