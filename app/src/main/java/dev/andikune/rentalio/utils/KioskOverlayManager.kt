package dev.andikune.rentalio.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class KioskOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var toastView: View? = null
    private var countdownView: View? = null
    private var tvCountdownNumber: TextView? = null

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    /**
     * Munculkan Custom Popup / HUD Toast di sudut KANAN BAWAH layar.
     * Pass-through: FLAG_NOT_TOUCHABLE and FLAG_NOT_FOCUSABLE.
     */
    fun showWarningToast(message: String, durationMs: Long = 3000L) {
        if (!canDrawOverlays()) return

        mainHandler.post {
            removeToastView()

            val density = context.resources.displayMetrics.density
            val padH = (16 * density).toInt()
            val padV = (10 * density).toInt()

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(padH, padV, padH, padV)
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#E60B1220"))
                    setStroke((1.5f * density).toInt(), Color.parseColor("#00E5FF"))
                    cornerRadius = 14 * density
                }
                background = bg
            }

            val tv = TextView(context).apply {
                text = message
                setTextColor(Color.parseColor("#00E5FF"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            container.addView(tv)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = (24 * density).toInt()
                y = (24 * density).toInt()
            }

            try {
                windowManager.addView(container, params)
                toastView = container
                mainHandler.postDelayed({
                    removeToastView()
                }, durationMs)
            } catch (_: Exception) {}
        }
    }

    private fun removeToastView() {
        toastView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            toastView = null
        }
    }

    /**
     * Countdown 30 Detik Terakhir:
     * Fullscreen Transparent Overlay with large centered countdown number.
     * Pass-through: FLAG_NOT_TOUCHABLE and FLAG_NOT_FOCUSABLE.
     */
    fun update30SecondsCountdown(secondsRemaining: Int) {
        if (!canDrawOverlays() || secondsRemaining <= 0) {
            hide30SecondsCountdown()
            return
        }

        mainHandler.post {
            if (countdownView == null) {
                createCountdownOverlay()
            }
            tvCountdownNumber?.text = "$secondsRemaining"
        }
    }

    private fun createCountdownOverlay() {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val density = context.resources.displayMetrics.density
            val pad = (20 * density).toInt()
            setPadding(pad, pad, pad, pad)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#B3050810"))
                setStroke((2 * density).toInt(), Color.parseColor("#FF1744"))
                cornerRadius = 24 * density
            }
            background = bg
        }

        val tvTitle = TextView(context).apply {
            text = "WAKTU HABIS DALAM"
            setTextColor(Color.parseColor("#FF8A80"))
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        }
        card.addView(tvTitle)

        val tvNum = TextView(context).apply {
            text = "30"
            setTextColor(Color.parseColor("#FF1744"))
            textSize = 48f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        tvCountdownNumber = tvNum
        card.addView(tvNum)

        val tvSub = TextView(context).apply {
            text = "DETIK"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 11f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        card.addView(tvSub)

        val frameParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        root.addView(card, frameParams)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(root, params)
            countdownView = root
        } catch (_: Exception) {}
    }

    fun hide30SecondsCountdown() {
        mainHandler.post {
            countdownView?.let {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {}
                countdownView = null
                tvCountdownNumber = null
            }
        }
    }
}
