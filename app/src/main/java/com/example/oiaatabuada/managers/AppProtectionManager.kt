package com.example.oiaatabuada.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import android.provider.Settings
import androidx.annotation.RequiresApi

class AppProtectionManager(private val context: Context) {
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent: ComponentName by lazy {
        ComponentName(context, MathGuardAdminReceiver::class.java)
    }

    companion object {
        private const val TAG = "AppProtectionManager"
        const val DEVICE_ADMIN_REQUEST = 1001
    }

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun requestDeviceAdminPermission(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Este app precisa de permissões administrativas para funcionar corretamente e ajudar no aprendizado da criança."
            )
        }
        return intent
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun preventUninstall(): Boolean {
        return if (isDeviceAdminActive()) {
            try {
                try {
                    devicePolicyManager.setApplicationHidden(
                        adminComponent,
                        context.packageName,
                        true
                    )
                    Log.d(TAG, "App escondido do launcher")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao esconder app", e)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        devicePolicyManager.addUserRestriction(
                            adminComponent,
                            UserManager.DISALLOW_SAFE_BOOT
                        )
                        devicePolicyManager.addUserRestriction(
                            adminComponent,
                            UserManager.DISALLOW_FACTORY_RESET
                        )
                        devicePolicyManager.addUserRestriction(
                            adminComponent,
                            UserManager.DISALLOW_UNINSTALL_APPS
                        )
                        Log.d(TAG, "Restrições de usuário adicionadas")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao adicionar restrições", e)
                }

                try {
                    disablePlayStore()
                    disablePackageInstaller()
                    Log.d(TAG, "Apps de desinstalação desabilitados")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao desabilitar apps", e)
                }

                try {
                    enableMaximumSecurityPolicies()
                    Log.d(TAG, "Políticas de segurança máxima ativadas")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao ativar políticas de segurança", e)
                }

                Log.d(TAG, "App protegido contra desinstalação")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao proteger app", e)
                false
            }
        } else {
            Log.w(TAG, "Não é possível proteger - não é admin do dispositivo")
            false
        }
    }

    private fun disablePlayStore() {
        try {
            devicePolicyManager.setApplicationHidden(
                adminComponent,
                "com.android.vending",
                true
            )

            Runtime.getRuntime().exec("pm disable-user com.android.vending")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desabilitar Play Store", e)
        }
    }

    private fun disablePackageInstaller() {
        try {
            val installers = arrayOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.samsung.android.packageinstaller"
            )

            installers.forEach { packageName ->
                devicePolicyManager.setApplicationHidden(
                    adminComponent,
                    packageName,
                    true
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desabilitar instalador", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun enableMaximumSecurityPolicies() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                devicePolicyManager
                    .setNetworkLoggingEnabled(adminComponent, true)
            }

            devicePolicyManager
                .setCertInstallerPackage(adminComponent,
                    context.packageName)

            devicePolicyManager.setProfileName(
                adminComponent, "Modo Estudo Bloqueado")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ativar políticas máximas", e)
        }
    }

    fun allowUninstall(): Boolean {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.setApplicationHidden(
                    adminComponent,
                    context.packageName,
                    false
                )

                enablePlayStore()
                enablePackageInstaller()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    devicePolicyManager.clearUserRestriction(
                        adminComponent,
                        UserManager.DISALLOW_UNINSTALL_APPS
                    )
                }

                Log.d(TAG, "App desbloqueado para desinstalação")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao desbloquear app", e)
                false
            }
        } else {
            false
        }
    }

    private fun enablePlayStore() {
        try {
            devicePolicyManager.setApplicationHidden(
                adminComponent,
                "com.android.vending",
                false
            )
            Runtime.getRuntime().exec("pm enable com.android.vending")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao habilitar Play Store", e)
        }
    }

    private fun enablePackageInstaller() {
        try {
            val installers = arrayOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller"
            )

            installers.forEach { packageName ->
                devicePolicyManager.setApplicationHidden(
                    adminComponent,
                    packageName,
                    false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao habilitar instalador", e)
        }
    }

    fun isAppUninstallBlocked(): Boolean {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.isApplicationHidden(
                    adminComponent, context.packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao verificar status de bloqueio", e)
                false
            }
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun setupAppProtection() {
        if (isDeviceAdminActive()) {
            val success = preventUninstall()

            makeUninstallDifficult()

            if (success) {
                Toast.makeText(
                    context, "App protegido com sucesso!",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context, "Erro ao proteger app", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w(TAG, "Tentando configurar proteção sem permissões de admin")
        }
    }

    fun lockDevice(): Boolean {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.lockNow()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    devicePolicyManager.setKeyguardDisabled(adminComponent, true)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    devicePolicyManager.setGlobalSetting(
                        adminComponent,
                        Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                        "1"
                    )
                }

                Log.d(TAG, "Dispositivo travado com sucesso")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao travar dispositivo", e)
                false
            }
        } else {
            false
        }
    }

    fun setupUninstallProtection() {
        try {
            val intent = Intent(
                context, UninstallProtectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar proteção contra desinstalação", e)
        }
    }

    fun makeUninstallDifficult(): Boolean {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.setApplicationHidden(
                    adminComponent,
                    context.packageName,
                    true
                )

                disablePlayStore()
                disablePackageInstaller()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    devicePolicyManager.addUserRestriction(
                        adminComponent,
                        UserManager.DISALLOW_UNINSTALL_APPS
                    )
                }

                setupUninstallProtection()

                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao dificultar desinstalação", e)
                false
            }
        } else false
    }
}