package com.example.oiaatabuada.screens.quiz

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import android.content.Intent
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.oiaatabuada.MainActivity
import com.example.oiaatabuada.managers.MathQuizManager

@Composable
fun MathGuardApp(
    showExit: Boolean = false,
    showQuiz: Boolean = false,
    mathQuizManager: MathQuizManager,
    onQuizStarted: () -> Unit = {},
    onQuizCompleted: () -> Unit = {},
    onParentAccessRequested: () -> Unit = {}
) {
    Log.d("MathGuardApp", "MathGuardApp iniciado - showExit: $showExit, showQuiz: $showQuiz")

    var showPasswordScreen by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasQuizStarted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(showExit) {
        if (showExit) {
            showPasswordScreen = true
        }
    }

    LaunchedEffect(mathQuizManager.showQuestions, mathQuizManager.showStartMessage) {
        if (mathQuizManager.showQuestions && !mathQuizManager.showStartMessage && !hasQuizStarted) {
            hasQuizStarted = true
            onQuizStarted()
            Log.d("MathGuardApp", "Quiz iniciado - ativando proteções")
        }
    }

    if (showPasswordScreen) {
        PasswordScreen(
            onSuccess = {
                showPasswordScreen = false
                if (showExit) {
                    (context as? Activity)?.finish()
                }
            },
            onCancel = {
                showPasswordScreen = false
                if (showExit) {
                    val newIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("SHOW_QUIZ", true)
                    }
                    context.startActivity(newIntent)
                    (context as? Activity)?.finish()
                }
            }
        )
        return
    }

    LaunchedEffect(mathQuizManager.showQuestions) {
        if (!mathQuizManager.showQuestions) {
            while (mathQuizManager.timeUntilNextChallenge > 0) {
                delay(1000)
                mathQuizManager.timeUntilNextChallenge--
            }
            mathQuizManager.timeUntilNextChallenge = 600
            mathQuizManager.showQuestions = true
            mathQuizManager.showStartMessage = true
            mathQuizManager.showEndMessage = false
            mathQuizManager.resetQuiz()
            hasQuizStarted = false
        }
    }

    LaunchedEffect(mathQuizManager.showStartMessage) {
        if (mathQuizManager.showStartMessage && mathQuizManager.showQuestions) {
            delay(3000)
            mathQuizManager.showStartMessage = false
        }
    }

    LaunchedEffect(mathQuizManager.showEndMessage) {
        if (mathQuizManager.showEndMessage) {
            delay(3000)
            mathQuizManager.showEndMessage = false
            mathQuizManager.showQuestions = false
            hasQuizStarted = false
            onQuizCompleted()
            Log.d("MathGuardApp", "Quiz completado - desativando proteções")
        }
    }

    if (showError) {
        ErrorScreen(
            message = errorMessage,
            onDismiss = {
                showError = false
                errorMessage = ""
            }
        )
        return
    }

    if (mathQuizManager.showStartMessage && mathQuizManager.showQuestions) {
        StartMessageScreen()
    } else if (mathQuizManager.showEndMessage) {
        EndMessageScreen()
    } else if (mathQuizManager.showQuestions && mathQuizManager.currentQuestion <= 3 && mathQuizManager.questions.isNotEmpty()) {
        val currentQuestionIndex = mathQuizManager.currentQuestion - 1
        if (currentQuestionIndex < mathQuizManager.questions.size) {
            MathQuestionScreen(
                question = mathQuizManager.questions[currentQuestionIndex],
                questionNumber = mathQuizManager.currentQuestion,
                userAnswer = mathQuizManager.userAnswer,
                onAnswerChange = { mathQuizManager.userAnswer = it },
                onSubmit = { mathQuizManager.submitAnswer() }
            )
        } else {
            errorMessage = "Erro: Índice de pergunta inválido"
            showError = true
        }
    } else {
        WaitingScreen(mathQuizManager.timeUntilNextChallenge)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.BottomEnd)
                .clickable { onParentAccessRequested() }
                .background(Color.Transparent)
        )
    }
}