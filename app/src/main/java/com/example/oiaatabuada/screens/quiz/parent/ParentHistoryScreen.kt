package com.example.oiaatabuada.screens.quiz.parent

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.oiaatabuada.managers.ExerciseSession
import com.example.oiaatabuada.managers.HistoryManager
import com.example.oiaatabuada.managers.PermissionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHistoryScreen(
    historyManager: HistoryManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessions by remember { mutableStateOf(historyManager.getAllSessions()) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico da Criança", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.padding(padding)) {
                val todayStats = historyManager.getTodaySessions()
                val todayCorrect = todayStats.sumOf { it.correctAnswers }
                val todayTotal = todayStats.sumOf { it.totalQuestions }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hoje", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("$todayCorrect/$todayTotal acertos", color = Color.White)
                        Text("${todayStats.size} sessões completadas", color = Color.White)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessions.reversed()) { session ->
                        HistorySessionItem(session)
                    }
                }

                Button(
                    onClick = {
                        val permissionManager = PermissionManager(context)
                        if (permissionManager.hasStoragePermission()) {
                            shareHistoryFixed(context, historyManager.exportHistoryJson())
                        } else {
                            permissionManager.requestStoragePermission()
                            Toast.makeText(context, "Conceda permissão de armazenamento e tente novamente", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Exportar Histórico")
                }

                Button(
                    onClick = { showExitConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("CONTINUAR QUIZ")
                }

                if (showExitConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showExitConfirmation = false },
                        title = { Text("Continuar Quiz") },
                        text = { Text("Deseja voltar para o quiz?") },
                        confirmButton = {
                            Button(onClick = {
                                showExitConfirmation = false
                                onBack()
                            }) {
                                Text("Sim")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitConfirmation = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun shareHistoryFixed(context: Context, historyJson: String) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "historico_mathguard_$timeStamp.txt"

        val formattedText = formatHistoryForSharing(historyJson, timeStamp)

        saveToDownloads(context, fileName, formattedText)

        shareViaApps(context, formattedText, timeStamp)

    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun saveToDownloads(context: Context, fileName: String, content: String) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                Toast.makeText(context, "Arquivo salvo em Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 9 e inferior
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeText(content)
            Toast.makeText(context, "Arquivo salvo em Downloads: $fileName", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao salvar em Downloads: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareViaApps(context: Context, content: String, timeStamp: String) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "📊 Relatório MathGuard - $timeStamp")
            putExtra(Intent.EXTRA_TEXT, content)
        }

        val whatsappIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, content)
        }

        val emailIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // Email vazio para o usuário preencher
            putExtra(Intent.EXTRA_SUBJECT, "📊 Relatório de Atividades MathGuard")
            putExtra(Intent.EXTRA_TEXT, content)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar relatório via:")

        val targetedIntents = mutableListOf<Intent>()

        try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            targetedIntents.add(whatsappIntent)
        } catch (e: Exception) {
            // WhatsApp não instalado
        }

        targetedIntents.add(emailIntent)

        if (targetedIntents.isNotEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
        }

        context.startActivity(chooserIntent)

    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatHistoryForSharing(historyJson: String, timeStamp: String): String {
    return try {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<ExerciseSession>>() {}.type
        val sessions: List<ExerciseSession> = gson.fromJson(historyJson, type) ?: emptyList()

        buildString {
            appendLine("📊 RELATÓRIO DE ATIVIDADES - MATHGUARD")
            appendLine("=" .repeat(50))
            appendLine("📅 Data do relatório: $timeStamp")
            appendLine("👶 Total de sessões: ${sessions.size}")
            appendLine()

            val totalCorrect = sessions.sumOf { it.correctAnswers }
            val totalQuestions = sessions.sumOf { it.totalQuestions }
            val totalTime = sessions.sumOf { it.timeSpentSeconds }
            val avgAccuracy = if (totalQuestions > 0) (totalCorrect * 100.0 / totalQuestions) else 0.0

            appendLine("📈 RESUMO GERAL:")
            appendLine("✅ Total de acertos: $totalCorrect de $totalQuestions")
            appendLine("🎯 Taxa de acerto: ${String.format("%.1f", avgAccuracy)}%")
            appendLine("⏱️ Tempo total: ${totalTime / 60} minutos")
            appendLine()

            appendLine("📝 ÚLTIMAS ATIVIDADES:")
            appendLine("-" .repeat(30))

            sessions.takeLast(10).reversed().forEach { session ->
                val accuracy = if (session.totalQuestions > 0) {
                    (session.correctAnswers * 100.0 / session.totalQuestions)
                } else 0.0

                appendLine("📅 ${session.date} às ${session.startTime}")
                appendLine("   ✅ Acertos: ${session.correctAnswers}/${session.totalQuestions} (${String.format("%.1f", accuracy)}%)")
                appendLine("   ⏱️ Tempo: ${session.timeSpentSeconds}s")
                appendLine()
            }

            appendLine("🚀 Continue incentivando os estudos!")
            appendLine("📱 Gerado pelo app OiaATabuada! :)")
        }

    } catch (e: Exception) {
        "📊 RELATÓRIO OIAATABUADA:\n\nErro ao processar dados: ${e.message}\n\nDados brutos:\n$historyJson"
    }
}

@Composable
fun HistorySessionItem(session: ExerciseSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Data: ${session.date} ${session.startTime}", color = Color.White)
            Text("Acertos: ${session.correctAnswers}/${session.totalQuestions}", color = Color.White)
            Text("Tempo: ${session.timeSpentSeconds} segundos", color = Color.White)
            Text("Tipo: ${session.sessionType}", color = Color.White)
        }
    }
}