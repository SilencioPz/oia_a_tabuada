package com.example.oiaatabuada

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.oiaatabuada.managers.*
import com.example.oiaatabuada.managers.AppProtectionManager.Companion.DEVICE_ADMIN_REQUEST
import com.example.oiaatabuada.screens.quiz.MathGuardApp
import com.example.oiaatabuada.screens.quiz.parent.ParentHistoryScreen
import com.example.oiaatabuada.screens.quiz.parent.ParentLoginScreen
import com.example.oiaatabuada.services.MathGuardService

class MainActivity : ComponentActivity() {
    private val mathQuizManager = MathQuizManager()
    private var isContentViewCreated = false

    private lateinit var appProtectionManager: AppProtectionManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var taskMonitorManager: TaskMonitorManager
    private lateinit var kioskModeManager: KioskModeManager

    private var showParentLogin by mutableStateOf(false)
    private var showParentHistory by mutableStateOf(false)

    companion object {
        private const val TAG = "MainActivity"
        const val REQUEST_UNINSTALL = 2001
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (appProtectionManager.isDeviceAdminActive()) {
            Log.d(TAG, "Device Admin ativado com sucesso")
            appProtectionManager.setupAppProtection()
        } else {
            Log.w(TAG, "Device Admin não foi ativado")
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mathQuizManager.initialize(this)

        appProtectionManager = AppProtectionManager(this)
        overlayManager = OverlayManager(this)
        taskMonitorManager = TaskMonitorManager(this)
        kioskModeManager = KioskModeManager(this)

        setupSecurityProtections()

        enableEdgeToEdge()

        checkUninstallProtection()

        val showExit = intent?.getBooleanExtra("EXIT_APP", false) ?: false
        var showQuiz = intent?.getBooleanExtra("SHOW_QUIZ", false) ?: false
        val fromService = intent?.getBooleanExtra("FROM_SERVICE", false) ?: false
        val fromMonitor = intent?.getBooleanExtra("FROM_MONITOR", false) ?: false

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)

        if (isFirstRun) {
            prefs.edit().putBoolean("first_run", false).apply()
            showQuiz = true
            setupFirstRun()
            Log.d(TAG, "Primeira execução - configurando proteções")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupModernBackHandler()
        }

        if (fromService || fromMonitor) {
            showQuiz = true
            mathQuizManager.resetQuiz()
        }

        if (appProtectionManager.isDeviceAdminActive()) {
            Thread {
                Thread.sleep(2000)
                appProtectionManager.preventUninstall()
                appProtectionManager.lockDevice()
            }.start()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)

        Log.d(
            TAG,
            "showExit: $showExit, showQuiz: $showQuiz, fromService: $fromService, fromMonitor: $fromMonitor"
        )

        setContent {
            isContentViewCreated = true
            AppTheme {
                if (showParentHistory) {
                    ParentHistoryScreen(
                        historyManager = HistoryManager(this@MainActivity),
                        onBack = { showParentHistory = false }
                    )
                } else if (showParentLogin) {
                    ParentLoginScreen(
                        onSuccess = {
                            showParentLogin = false
                            showParentHistory = true
                        },
                        onCancel = { showParentLogin = false },
                        onBack = { showParentHistory = false }
                    )
                } else {
                    MathGuardApp(
                        showExit = showExit,
                        showQuiz = showQuiz,
                        mathQuizManager = mathQuizManager,
                        onQuizStarted = {
                            enableQuizProtections()
                        },
                        onQuizCompleted = {
                            disableQuizProtections()
                            MathGuardService.startService(this@MainActivity)

                            val protectionLock = ProtectionLockManager(this@MainActivity)
                            protectionLock.enableProtectionLock()
                        },
                        onParentAccessRequested = {
                            showParentLogin = true
                        }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            when (keyCode) {
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_RECENT_APPS,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_MENU -> {
                    Log.d(TAG, "Tecla do sistema bloqueada: $keyCode")
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event?.repeatCount == 0) {
            if (!showParentLogin && !showParentHistory) {
                showParentLogin = true
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    @RequiresApi(33)
    private fun setupModernBackHandler() {
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
                Log.d(TAG, "Botão voltar bloqueado durante quiz (Android 13+)")
                return@registerOnBackInvokedCallback
            }
            finish()
        }
    }

    private fun checkUninstallProtection() {
        if (appProtectionManager.isDeviceAdminActive()) {
            val isBlocked = appProtectionManager.isAppUninstallBlocked()
            Log.d(TAG, "Proteção contra desinstalação: ${if (isBlocked) "ATIVA" else "INATIVA"}")

            appProtectionManager.setupUninstallProtection()
        }
    }

    private fun requestUninstallPermission() {
        if (appProtectionManager.isDeviceAdminActive()) {
            val success = appProtectionManager.allowUninstall()
            if (success) {
                Toast.makeText(this, "Desinstalação liberada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSecurityProtections() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        kioskModeManager.enableKioskMode()
        kioskModeManager.setupImmersiveMode()
        kioskModeManager.preventStatusBarExpansion()
    }

    private fun enableQuizProtections() {
        Log.d(TAG, "Habilitando proteções do quiz")

        kioskModeManager.enableKioskMode()

        taskMonitorManager.startMonitoring()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun disableQuizProtections() {
        Log.d(TAG, "Desabilitando proteções do quiz")

        taskMonitorManager.stopMonitoring()

        // Manter modo quiosque básico (não desabilitar completamente)
        // kioskModeManager.disableKioskMode() // Comentado para manter proteção básica
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun setupFirstRun() {
        requestDeviceAdminPermission()
        requestOverlayPermission()

        MathGuardService.startService(this)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun requestDeviceAdminPermission() {
        if (!appProtectionManager.isDeviceAdminActive()) {
            val intent = appProtectionManager.requestDeviceAdminPermission()
            deviceAdminLauncher.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun setupAsParentalControl() {
        if (!appProtectionManager.isDeviceAdminActive()) {
            val intent = appProtectionManager.requestDeviceAdminPermission()
            startActivityForResult(intent, DEVICE_ADMIN_REQUEST)
        } else {
            appProtectionManager.preventUninstall()
            appProtectionManager.lockDevice()
        }
    }

    private fun requestOverlayPermission() {
        if (!overlayManager.checkOverlayPermission()) {
            overlayManager.requestOverlayPermission()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            Log.d(TAG, "Tentativa de sair durante o quiz - redirecionando de volta")

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("SHOW_QUIZ", true)
            }
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()

        if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            Log.d(TAG, "Tentativa de pausar durante quiz")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("SHOW_QUIZ", true)
            }
            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()

        if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            Log.d(TAG, "Tentativa de parar durante quiz - reabrindo")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("SHOW_QUIZ", true)
            }
            startActivity(intent)
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            Log.d(TAG, "Botão voltar bloqueado durante quiz")
            return
        }

        super.onBackPressed()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus && mathQuizManager.showQuestions && !mathQuizManager.showEndMessage) {
            Log.d(TAG, "Foco perdido durante quiz - tentando recuperar")

            kioskModeManager.enableKioskMode()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("SHOW_QUIZ", true)
            }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        taskMonitorManager.stopMonitoring()

        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("SHOW_QUIZ", mathQuizManager.showQuestions)
            }
            startActivity(intent)
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF2196F3),
        onPrimary = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        background = Color(0xFF121212),
        onBackground = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}