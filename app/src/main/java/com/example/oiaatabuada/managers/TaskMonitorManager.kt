package com.example.oiaatabuada.managers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.oiaatabuada.MainActivity

class TaskMonitorManager(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private var isMonitoring = false
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    companion object {
        private const val TAG = "TaskMonitorManager"
        private const val MONITOR_INTERVAL = 2000L // 2 segundos

        private val BLOCKED_APPS = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.miui.home",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher"
        )
    }

    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        Log.d(TAG, "Iniciando monitoramento de tarefas")

        monitoringRunnable = object : Runnable {
            override fun run() {
                checkRunningApps()
                handler.postDelayed(this, MONITOR_INTERVAL)
            }
        }

        handler.post(monitoringRunnable!!)
    }

    fun stopMonitoring() {
        isMonitoring = false
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        Log.d(TAG, "Monitoramento de tarefas parado")
    }

    @SuppressLint("ServiceCast")
    private fun checkRunningApps() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 60, // 1 minuto atrás
                    currentTime
                )

                if (stats != null) {
                    var mostRecentStat: UsageStats? = null
                    for (stat in stats) {
                        if (mostRecentStat == null || stat.lastTimeUsed > mostRecentStat.lastTimeUsed) {
                            mostRecentStat = stat
                        }
                    }

                    mostRecentStat?.packageName?.let { currentPackage ->
                        if (currentPackage != context.packageName && shouldBlockApp(currentPackage)) {
                            Log.d(TAG, "App bloqueado detectado: $currentPackage - redirecionando")
                            bringMathAppToFront()
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val runningTasks = activityManager.getRunningTasks(1)
                if (runningTasks.isNotEmpty()) {
                    val topActivity = runningTasks[0].topActivity
                    val currentPackage = topActivity?.packageName

                    if (currentPackage != null &&
                        currentPackage != context.packageName &&
                        shouldBlockApp(currentPackage)) {
                        Log.d(TAG, "App bloqueado detectado: $currentPackage - redirecionando")
                        bringMathAppToFront()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar apps rodando", e)
        }
    }

    private fun shouldBlockApp(packageName: String): Boolean {
        if (BLOCKED_APPS.contains(packageName)) {
            return true
        }

        if (packageName.contains("launcher", ignoreCase = true)) {
            return true
        }

        if (packageName.contains("settings", ignoreCase = true)) {
            return true
        }

        if (packageName.contains("installer", ignoreCase = true) ||
            packageName.contains("packagemanager", ignoreCase = true)) {
            return true
        }

        return false
    }

    private fun bringMathAppToFront() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_QUIZ", true)
            putExtra("FROM_MONITOR", true)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao trazer app para frente", e)
        }
    }

    fun isOurAppInForeground(): Boolean {
        try {
            val runningTasks = activityManager.getRunningTasks(1)
            if (runningTasks.isNotEmpty()) {
                val topActivity = runningTasks[0].topActivity
                return topActivity?.packageName == context.packageName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar se app está em primeiro plano", e)
        }
        return false
    }
}