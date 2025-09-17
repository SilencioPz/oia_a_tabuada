package com.example.oiaatabuada.managers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

class MathGuardAdminReceiver : DeviceAdminReceiver() {
    companion object {
        private const val TAG = "MathGuardAdminReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin habilitado")

        val protectionManager = AppProtectionManager(context)
        protectionManager.setupAppProtection()

        Toast.makeText(context, "Proteções ativadas!", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin desabilitado")
        Toast.makeText(context, "Proteções desativadas", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Solicitação para desabilitar admin")
        return "Desativar as proteções do app de matemática?"
    }
}