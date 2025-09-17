package com.example.oiaatabuada.managers

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.graphics.Color

class ProtectionLockManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("protection_prefs", Context.MODE_PRIVATE)
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun enableProtectionLock() {
        prefs.edit().putBoolean("protection_lock", true).apply()
        showProtectionOverlay()
    }

    fun disableProtectionLock() {
        prefs.edit().putBoolean("protection_lock", false).apply()
        hideProtectionOverlay()
    }

    fun isLockActive(): Boolean {
        return prefs.getBoolean("protection_lock", false)
    }

    private fun showProtectionOverlay() {
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
        }

        overlayView = View(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e("ProtectionLock", "Erro ao mostrar overlay", e)
        }
    }

    private fun hideProtectionOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e("ProtectionLock", "Erro ao remover overlay", e)
        }
    }
}