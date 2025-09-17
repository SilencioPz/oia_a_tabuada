package com.example.oiaatabuada.managers

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.oiaatabuada.MainActivity

class OverlayManager(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false

    companion object {
        private const val TAG = "OverlayManager"
        const val OVERLAY_PERMISSION_REQUEST = 1234
    }

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    fun showOverlay() {
        if (!checkOverlayPermission() || isOverlayShowing) {
            Log.w(TAG, "Não é possível mostrar overlay - Permissão: ${checkOverlayPermission()}, Showing: $isOverlayShowing")
            return
        }

        try {
            overlayView = createOverlayView()
            val params = createOverlayParams()
            windowManager?.addView(overlayView, params)
            isOverlayShowing = true
            Log.d(TAG, "Overlay mostrado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mostrar overlay", e)
        }
    }

    fun hideOverlay() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
                overlayView = null
                isOverlayShowing = false
                Log.d(TAG, "Overlay removido")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover overlay", e)
        }
    }

    private fun createOverlayView(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(context).apply {
            text = "🧮 Hora da Tabuada!"
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            gravity = Gravity.CENTER
        }

        val message = TextView(context).apply {
            text = "É hora de praticar matemática!\nClique no botão para começar."
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        val button = Button(context).apply {
            text = "Começar Quiz"
            setOnClickListener {
                launchMathQuiz()
                hideOverlay()
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(button)

        return layout
    }

    private fun createOverlayParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun launchMathQuiz() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_QUIZ", true)
            putExtra("FROM_OVERLAY", true)
        }
        context.startActivity(intent)
    }
}