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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.holki9.applock.data.PreferencesManager

class LauncherOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentOverlayView: View? = null
    private var currentPackage: String? = null

    private val autoDismissRunnable = Runnable {
        hideOverlay()
    }

    fun showQuickLockButton(packageName: String, appName: String, anchorY: Int = -1) {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isLauncherQuickLockEnabled()) return
        if (!android.provider.Settings.canDrawOverlays(context)) return

        handler.post {
            hideOverlay()

            currentPackage = packageName
            val isLocked = prefs.isAppLocked(packageName)

            val pillLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(28).toFloat()
                    if (isLocked) {
                        setColor(0xFF1E293B.toInt())
                        setStroke(dpToPx(2), 0xFFF59E0B.toInt()) // Amber outline for locked
                    } else {
                        setColor(0xFF4F46E5.toInt()) // Indigo for quick lock action
                    }
                }
                background = bg
                setPadding(dpToPx(18), dpToPx(12), dpToPx(20), dpToPx(12))
                elevation = dpToPx(10).toFloat()
            }

            val iconText = TextView(context).apply {
                text = if (isLocked) "🔓" else "🔒"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding(0, 0, dpToPx(8), 0)
            }

            val labelText = TextView(context).apply {
                val action = if (isLocked) "Снять защиту" else "Заблокировать"
                val truncatedName = if (appName.length > 14) appName.take(12) + "…" else appName
                text = "$action «$truncatedName»"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }

            pillLayout.addView(iconText)
            pillLayout.addView(labelText)

            // Click listener on the quick lock button
            pillLayout.setOnClickListener {
                vibrateClick()
                val newState = prefs.toggleLock(packageName)
                if (newState) {
                    iconText.text = "✓"
                    labelText.text = "Заблокировано!"
                    (pillLayout.background as? GradientDrawable)?.setColor(0xFF10B981.toInt()) // Emerald Green
                } else {
                    iconText.text = "✓"
                    labelText.text = "Разблокировано!"
                    (pillLayout.background as? GradientDrawable)?.setColor(0xFF64748B.toInt()) // Slate
                }

                handler.postDelayed({
                    hideOverlay()
                }, 800)
            }

            // Window Layout Params
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                // Position near top or around anchor
                y = if (anchorY > 0) {
                    val safeY = anchorY - dpToPx(70)
                    if (safeY < dpToPx(80)) dpToPx(90) else safeY
                } else {
                    dpToPx(120)
                }
            }

            // Outside touch dismisses
            pillLayout.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    hideOverlay()
                    true
                } else {
                    false
                }
            }

            // Entry animation
            pillLayout.alpha = 0f
            pillLayout.scaleX = 0.85f
            pillLayout.scaleY = 0.85f

            try {
                windowManager.addView(pillLayout, layoutParams)
                currentOverlayView = pillLayout

                pillLayout.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .start()

                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, 4500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hideOverlay() {
        handler.removeCallbacks(autoDismissRunnable)
        currentOverlayView?.let { view ->
            try {
                view.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(150)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            try {
                                windowManager.removeViewImmediate(view)
                            } catch (_: Exception) {}
                        }
                    })
                    .start()
            } catch (e: Exception) {
                try {
                    windowManager.removeViewImmediate(view)
                } catch (_: Exception) {}
            }
            currentOverlayView = null
            currentPackage = null
        }
    }

    private fun vibrateClick() {
        val prefs = PreferencesManager.getInstance(context)
        if (!prefs.isVibrationEnabled()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
