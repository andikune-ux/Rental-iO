package dev.andikune.rentalio.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

object SystemOverlayHelper {

    private var countdownOverlayView: View? = null
    private var floatingHomeView: View? = null
    private var windowManager: WindowManager? = null

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun showPassThroughToastAlert(context: Context, minutesRemaining: Int) {
        if (!canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                val toastView = FrameLayout(context).apply {
                    setPadding(36, 18, 36, 18)
                    setBackgroundColor(Color.parseColor("#E60B1322"))
                }

                val textView = TextView(context).apply {
                    text = "⏱️ PERINGATAN: Sisa waktu sewa $minutesRemaining menit lagi!"
                    setTextColor(Color.parseColor("#00E5FF"))
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                toastView.addView(textView)

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    x = 40
                    y = 40
                }

                wm.addView(toastView, params)

                handler.postDelayed({
                    try {
                        wm.removeView(toastView)
                    } catch (_: Exception) {}
                }, 3000L)
            } catch (_: Exception) {}
        }
    }

    fun showPassThrough30sCountdown(context: Context, remainingSeconds: Int) {
        if (!canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                if (countdownOverlayView == null) {
                    val container = FrameLayout(context).apply {
                        setBackgroundColor(Color.parseColor("#55000000"))
                    }

                    val textView = TextView(context).apply {
                        id = View.generateViewId()
                        tag = "countdown_text"
                        text = "⚠️ SEWA BERAKHIR DALAM $remainingSeconds DETIK"
                        setTextColor(Color.parseColor("#FF1744"))
                        textSize = 22f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                    }

                    val lp = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    ).apply {
                        topMargin = 40
                    }
                    container.addView(textView, lp)

                    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or // Tembus Sentuhan
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // Tembus Fokus
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    )

                    wm.addView(container, params)
                    countdownOverlayView = container
                    windowManager = wm
                } else {
                    val textView = countdownOverlayView?.findViewWithTag<TextView>("countdown_text")
                    textView?.text = "⚠️ SEWA BERAKHIR DALAM $remainingSeconds DETIK"
                }
            } catch (_: Exception) {}
        }
    }

    fun hidePassThroughCountdown() {
        val view = countdownOverlayView ?: return
        val wm = windowManager ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                wm.removeView(view)
            } catch (_: Exception) {}
            countdownOverlayView = null
        }
    }

    /**
     * Tombol Home Melayang (Floating Home Assist Button)
     * Ditampilkan saat game/aplikasi sedang berjalan, agar penyewa bisa kembali
     * ke launcher Rental.iO untuk memilih game lain tanpa perlu tombol Recents.
     */
    fun showFloatingHomeButton(context: Context) {
        if (!canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            if (floatingHomeView != null) return@post

            val density = context.resources.displayMetrics.density
            val btnSize = (52 * density).toInt()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                btnSize,
                btnSize,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (16 * density).toInt()
                y = (100 * density).toInt()
            }

            val button = FrameLayout(context).apply {
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#E60E1726"))
                    setStroke((2 * density).toInt(), Color.parseColor("#00E5FF"))
                }
                background = bg
                elevation = 10 * density

                val icon = TextView(context).apply {
                    text = "🏠"
                    textSize = 22f
                    gravity = Gravity.CENTER
                }
                addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))

                var initialX = 0
                var initialY = 0
                var initialTouchX = 0f
                var initialTouchY = 0f
                var isDrag = false

                setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDrag = false
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                isDrag = true
                                params.x = initialX + dx
                                params.y = initialY + dy
                                try {
                                    wm.updateViewLayout(v, params)
                                } catch (_: Exception) {}
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            if (!isDrag) {
                                v.performClick()
                            }
                            true
                        }
                        else -> false
                    }
                }

                setOnClickListener {
                    try {
                        val homeIntent = Intent(context, dev.andikune.rentalio.ui.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        context.startActivity(homeIntent)
                    } catch (_: Exception) {}
                }
            }

            try {
                wm.addView(button, params)
                floatingHomeView = button
                windowManager = wm
            } catch (_: Exception) {}
        }
    }

    fun hideFloatingHomeButton() {
        val view = floatingHomeView ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                val wm = windowManager ?: view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeView(view)
            } catch (_: Exception) {}
            floatingHomeView = null
        }
    }
}
