package com.example.oiaatabuada.managers

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MathQuizManager() {
    var showQuestions by mutableStateOf(true)
    var showStartMessage by mutableStateOf(true)
    var showEndMessage by mutableStateOf(false)
    var timeUntilNextChallenge by mutableStateOf(600)
    var currentQuestion by mutableStateOf(1)
    var userAnswer by mutableStateOf("")
    var questions by mutableStateOf<List<MathQuestion>>(emptyList())

    private var quizStartTime: Long = 0

    private val currentSessionResults = mutableListOf<QuestionResult>()

    private lateinit var context: Context
    private lateinit var historyManager: HistoryManager

    fun startQuiz() {
        quizStartTime = System.currentTimeMillis()
        currentSessionResults.clear()
    }

    fun initialize(context: Context) {
        this.context = context
        this.historyManager = HistoryManager(context)
    }

    init {
        resetQuiz()
    }

    fun resetQuiz() {
        currentQuestion = 1
        userAnswer = ""
        generateRandomQuestions()
        showStartMessage = true
        showEndMessage = false
        showQuestions = true
    }

    private fun generateRandomQuestions() {
        questions = List(3) { generateRandomQuestion() }
    }

    private fun generateRandomQuestion(): MathQuestion {
        return when (Random.nextInt(1, 6)) {
            1 -> generateMultiplicationQuestion()
            2 -> generateDivisionQuestion()
            3 -> generateAdditionQuestion()
            4 -> generateSubtractionQuestion()
            5 -> generateMixedOperationQuestion()
            else -> generateMultiplicationQuestion() // fallback
        }
    }

    private fun generateMultiplicationQuestion(): MathQuestion {
        val num1 = Random.nextInt(2, 13) // Tabuada até 12
        val num2 = Random.nextInt(2, 13)
        return MathQuestion("Quanto é $num1 × $num2?", num1 * num2)
    }

    private fun generateDivisionQuestion(): MathQuestion {
        val divisor = Random.nextInt(2, 13)
        val result = Random.nextInt(2, 13)
        val dividend = divisor * result
        return MathQuestion("Quanto é $dividend ÷ $divisor?", result)
    }

    private fun generateAdditionQuestion(): MathQuestion {
        val num1 = Random.nextInt(15, 101)
        val num2 = Random.nextInt(15, 101)
        return MathQuestion("Quanto é $num1 + $num2?", num1 + num2)
    }

    private fun generateSubtractionQuestion(): MathQuestion {
        val num1 = Random.nextInt(50, 151)
        val num2 = Random.nextInt(10, num1 - 10)
        return MathQuestion("Quanto é $num1 - $num2?", num1 - num2)
    }

    private fun generateMixedOperationQuestion(): MathQuestion {
        val a = Random.nextInt(2, 10)
        val b = Random.nextInt(2, 10)
        val c = Random.nextInt(2, 10)
        return MathQuestion("Quanto é ($a × $b) + $c?", (a * b) + c)
    }

    fun submitAnswer() {
        val currentQuestionIndex = currentQuestion - 1
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val answer = userAnswer.toIntOrNull()
            val isCorrect = answer == question.answer

            currentSessionResults.add(
                QuestionResult(
                    question = question.question,
                    userAnswer = userAnswer,
                    correctAnswer = question.answer.toString(),
                    wasCorrect = isCorrect,
                    operationType = detectOperationType(question.question)
                )
            )

            if (isCorrect) {
                if (currentQuestion < 3) {
                    currentQuestion++
                    userAnswer = ""
                } else {
                    saveQuizSession()
                    showEndMessage = true
                    showQuestions = false
                    startCountdown()
                }
            } else {
                saveQuizSession()
                resetQuiz()
            }
        }
    }

    private fun detectOperationType(question: String): String {
        return when {
            question.contains("×") || question.contains("*") -> "MULTIPLICAÇÃO"
            question.contains("÷") || question.contains("/") -> "DIVISÃO"
            question.contains("+") -> "ADIÇÃO"
            question.contains("-") -> "SUBTRAÇÃO"
            else -> "MISTA"
        }
    }

    private fun saveQuizSession() {
        val endTime = System.currentTimeMillis()
        val timeSpentSeconds = (endTime - quizStartTime) / 1000
        val correctCount = currentSessionResults.count { it.wasCorrect }

        val session = ExerciseSession(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(quizStartTime),
            endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(endTime),
            correctAnswers = correctCount,
            totalQuestions = currentSessionResults.size,
            timeSpentSeconds = timeSpentSeconds,
            questions = currentSessionResults.toList()
        )

        historyManager.saveSession(session)
    }

    private fun startCountdown() {
        timeUntilNextChallenge = 600
    }
}

data class MathQuestion(val question: String, val answer: Int)