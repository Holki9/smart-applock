package com.holki9.applock.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
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

    fun showQuickLockButton(packageName: String, appName: String) {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isLauncherQuickLockEnabled()) return
        if (!android.provider.Settings.canDrawOverlays(context)) return

        handler.post {
            currentPackage = packageName
            val isLocked = prefs.isAppLocked(packageName)

            // If already showing, simply update text & state instead of re-creating window!
            if (currentOverlayView != null && currentOverlayView?.isAttachedToWindow == true) {
                updateViewContent(isLocked, appName)
                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, 5000)
                return@post
            }

            hideOverlayImmediate()

            val pillLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(28).toFloat()
                    if (isLocked) {
                        setColor(0xFF1E293B.toInt()) // Slate dark
                        setStroke(dpToPx(2), 0xFFF59E0B.toInt()) // Amber outline for locked
                    } else {
                        setColor(0xFF4F46E5.toInt()) // Primary Indigo
                        setStroke(dpToPx(1), 0xFF818CF8.toInt())
                    }
                }
                background = bg
                setPadding(dpToPx(18), dpToPx(12), dpToPx(20), dpToPx(12))
                elevation = dpToPx(12).toFloat()
            }

            val iconText = TextView(context).apply {
                text = if (isLocked) "🔓" else "🔒"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding(0, 0, dpToPx(8), 0)
            }

            val labelText = TextView(context).apply {
                val action = if (isLocked) "Снять защиту" else "Заблокировать"
                val truncatedName = if (appName.length > 15) appName.take(13) + "…" else appName
                text = "$action «$truncatedName»"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }

            pillLayout.addView(iconText)
            pillLayout.addView(labelText)

            currentIconText = iconText
            currentLabelText = labelText

            // Click listener
            pillLayout.setOnClickListener {
                vibrateClick()
                val pkg = currentPackage ?: return@setOnClickListener
                val newState = prefs.toggleLock(pkg)
                if (newState) {
                    iconText.text = "✓"
                    labelText.text = "«$appName» защищен!"
                    (pillLayout.background as? GradientDrawable)?.apply {
                        setColor(0xFF10B981.toInt()) // Emerald Green
                        setStroke(dpToPx(1), 0xFF34D399.toInt())
                    }
                } else {
                    iconText.text = "✓"
                    labelText.text = "«$appName» разблокирован"
                    (pillLayout.background as? GradientDrawable)?.apply {
                        setColor(0xFF64748B.toInt()) // Slate
                        setStroke(dpToPx(1), 0xFF94A3B8.toInt())
                    }
                }

                handler.postDelayed({
                    hideOverlay()
                }, 900)
            }

            // Window Layout Params - Positioned near TOP center
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(85) // Gracefully below status bar
            }

            // Entry animation
            pillLayout.alpha = 0f
            pillLayout.translationY = -dpToPx(30).toFloat()
            pillLayout.scaleX = 0.9f
            pillLayout.scaleY = 0.9f

            try {
                windowManager.addView(pillLayout, layoutParams)
                currentOverlayView = pillLayout

                pillLayout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start()

                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, 5000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateViewContent(isLocked: Boolean, appName: String) {
        val action = if (isLocked) "Снять защиту" else "Заблокировать"
        val truncatedName = if (appName.length > 15) appName.take(13) + "…" else appName
        currentIconText?.text = if (isLocked) "🔓" else "🔒"
        currentLabelText?.text = "$action «$truncatedName»"
        (currentOverlayView?.background as? GradientDrawable)?.apply {
            if (isLocked) {
                setColor(0xFF1E293B.toInt())
                setStroke(dpToPx(2), 0xFFF59E0B.toInt())
            } else {
                setColor(0xFF4F46E5.toInt())
                setStroke(dpToPx(1), 0xFF818CF8.toInt())
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
                        .translationY(-dpToPx(25).toFloat())
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(160)
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

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
