package com.holki9.applock.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.holki9.applock.data.PreferencesManager

class LauncherOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var currentOverlayView: LinearLayout? = null
    private var currentIconText: TextView? = null
    private var currentLabelText: TextView? = null
    private var currentPackage: String? = null

    private val autoDismissRunnable = Runnable {
        hideOverlay()
    }

    /**
     * Shows an action button attached directly underneath the launcher's popup menu.
     */
    fun showAttachedButton(packageName: String, appName: String, anchorRect: Rect) {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isLauncherQuickLockEnabled()) return
        if (!android.provider.Settings.canDrawOverlays(context)) return

        handler.post {
            currentPackage = packageName
            val isLocked = prefs.isAppLocked(packageName)

            val metrics = context.resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val popupHeight = dpToPx(50)

            val safeX = maxOf(dpToPx(12), anchorRect.left)
            val popupWidth = if (anchorRect.width() > 100) {
                minOf(anchorRect.width(), screenWidth - safeX - dpToPx(12))
            } else {
                screenWidth - dpToPx(32)
            }

            val targetX = safeX
            // Position directly underneath the popup, or above if near screen bottom
            val targetY = if (anchorRect.bottom + popupHeight + dpToPx(24) < screenHeight) {
                anchorRect.bottom + dpToPx(8)
            } else {
                maxOf(dpToPx(40), anchorRect.top - popupHeight - dpToPx(8))
            }

            // If already showing and attached, update position and content smoothly
            if (currentOverlayView != null && currentOverlayView?.isAttachedToWindow == true) {
                updateViewContent(isLocked, appName)
                val lp = currentOverlayView?.layoutParams as? WindowManager.LayoutParams
                if (lp != null) {
                    lp.x = targetX
                    lp.y = targetY
                    lp.width = popupWidth
                    windowManager.updateViewLayout(currentOverlayView, lp)
                }
                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, 6000)
                return@post
            }

            hideOverlayImmediate()

            val cardLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(24).toFloat()
                    if (isLocked) {
                        setColor(0xFFF8FAFC.toInt())
                        setStroke(dpToPx(1.5f), 0xFFF59E0B.toInt()) // Amber stroke
                    } else {
                        setColor(0xFFFFFFFF.toInt()) // Pure white matching One UI card
                        setStroke(dpToPx(1.5f), 0xFFE2E8F0.toInt()) // Subtle clean border
                    }
                }
                background = bg
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                elevation = dpToPx(10).toFloat()
            }

            val iconText = TextView(context).apply {
                text = if (isLocked) "🔓" else "🔒"
                textSize = 18f
                setPadding(0, 0, dpToPx(8), 0)
            }

            val labelText = TextView(context).apply {
                val action = if (isLocked) "Снять защиту" else "Заблокировать"
                val truncatedName = if (appName.length > 16) appName.take(14) + "…" else appName
                text = "$action «$truncatedName»"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (isLocked) 0xFFD97706.toInt() else 0xFF0F172A.toInt())
            }

            cardLayout.addView(iconText)
            cardLayout.addView(labelText)

            currentIconText = iconText
            currentLabelText = labelText

            // Click listener
            cardLayout.setOnClickListener {
                vibrateClick()
                val pkg = currentPackage ?: return@setOnClickListener
                val newState = prefs.toggleLock(pkg)
                if (newState) {
                    iconText.text = "✓"
                    labelText.text = "«$appName» защищен!"
                    labelText.setTextColor(0xFF047857.toInt())
                    (cardLayout.background as? GradientDrawable)?.apply {
                        setColor(0xFFD1FAE5.toInt()) // Emerald light
                        setStroke(dpToPx(1.5f), 0xFF10B981.toInt())
                    }
                } else {
                    iconText.text = "✓"
                    labelText.text = "«$appName» разблокирован"
                    labelText.setTextColor(0xFF475569.toInt())
                    (cardLayout.background as? GradientDrawable)?.apply {
                        setColor(0xFFF1F5F9.toInt())
                        setStroke(dpToPx(1.5f), 0xFF94A3B8.toInt())
                    }
                }

                handler.postDelayed({
                    hideOverlay()
                }, 700)
            }

            val layoutParams = WindowManager.LayoutParams(
                popupWidth,
                popupHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = targetX
                y = targetY
            }

            // Smooth appearance
            cardLayout.alpha = 0f
            cardLayout.scaleY = 0.85f

            try {
                windowManager.addView(cardLayout, layoutParams)
                currentOverlayView = cardLayout

                cardLayout.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start()

                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, 6000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateViewContent(isLocked: Boolean, appName: String) {
        val action = if (isLocked) "Снять защиту" else "Заблокировать"
        val truncatedName = if (appName.length > 16) appName.take(14) + "…" else appName
        currentIconText?.text = if (isLocked) "🔓" else "🔒"
        currentLabelText?.text = "$action «$truncatedName»"
        currentLabelText?.setTextColor(if (isLocked) 0xFFD97706.toInt() else 0xFF0F172A.toInt())
        (currentOverlayView?.background as? GradientDrawable)?.apply {
            if (isLocked) {
                setColor(0xFFF8FAFC.toInt())
                setStroke(dpToPx(1.5f), 0xFFF59E0B.toInt())
            } else {
                setColor(0xFFFFFFFF.toInt())
                setStroke(dpToPx(1.5f), 0xFFE2E8F0.toInt())
            }
        }
    }

    fun hideOverlay() {
        handler.removeCallbacks(autoDismissRunnable)
        currentOverlayView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    view.animate()
                        .alpha(0f)
                        .scaleY(0.85f)
                        .setDuration(140)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                hideOverlayImmediate()
                            }
                        })
                        .start()
                } catch (_: Exception) {
                    hideOverlayImmediate()
                }
            } else {
                hideOverlayImmediate()
            }
        }
    }

    fun hideOverlayImmediate() {
        handler.removeCallbacks(autoDismissRunnable)
        currentOverlayView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                }
            } catch (_: Exception) {}
        }
        currentOverlayView = null
        currentIconText = null
        currentLabelText = null
        currentPackage = null
    }

    private fun vibrateClick() {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isVibrationEnabled()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
