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
import androidx.core.content.ContextCompat
import dev.andikune.rentalio.R

object SystemOverlayHelper {

    private var countdownOverlayView: View? = null
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

    /**
     * Bagian 3 Butir 13:
     * Toast alert di sudut kanan bawah selama 3 detik saat sisa waktu 15, 10, dan 5 menit.
     * Menggunakan FLAG_NOT_TOUCHABLE dan FLAG_NOT_FOCUSABLE agar sentuhan tembus ke game.
     */
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

                // Auto dismiss after 3 seconds
                handler.postDelayed({
                    try {
                        wm.removeView(toastView)
                    } catch (_: Exception) {}
                }, 3000L)
            } catch (_: Exception) {}
        }
    }

    /**
     * Bagian 3 Butir 13:
     * Fullscreen transparent countdown overlay pada sisa 30 detik.
     * Menggunakan FLAG_NOT_TOUCHABLE dan FLAG_NOT_FOCUSABLE sehingga pengguna tetap bisa main game sampai detik terakhir!
     */
    fun showPassThrough30sCountdown(context: Context, remainingSeconds: Int) {
        if (!canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                if (countdownOverlayView == null) {
                    val container = FrameLayout(context).apply {
                        setBackgroundColor(Color.parseColor("#55000000")) // semi-transparent
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
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
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
}
