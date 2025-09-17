package com.example.oiaatabuada.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.oiaatabuada.MainActivity
import kotlinx.coroutines.*

class MathGuardService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var countdownJob: Job? = null
    private var isQuizActive = false

    companion object {
        private const val TAG = "MathGuardService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "math_guard_channel"
        private const val QUIZ_INTERVAL = 600_000L // 10 minutos em ms

        fun startService(context: Context) {
            val intent = Intent(context, MathGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MathGuardService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Serviço criado")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Serviço iniciado")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        startQuizTimer()

        return START_STICKY
    }

    private fun startQuizTimer() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (true) {
                delay(QUIZ_INTERVAL)
                if (!isQuizActive) {
                    Log.d(TAG, "Hora do quiz! Iniciando activity...")
                    launchQuizActivity()
                }
            }
        }
    }

    private fun launchQuizActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_QUIZ", true)
            putExtra("FROM_SERVICE", true)
        }
        startActivity(intent)
        isQuizActive = true
    }

    fun setQuizActive(active: Boolean) {
        isQuizActive = active
        Log.d(TAG, "Quiz ativo: $active")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Math Guard Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém o app de matemática funcionando"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tabuada Ativa")
            .setContentText("App de matemática está funcionando")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Serviço destruído")
    }
}