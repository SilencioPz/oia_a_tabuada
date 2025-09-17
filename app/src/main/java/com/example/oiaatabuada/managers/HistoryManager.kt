package com.example.oiaatabuada.managers

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class ExerciseSession(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val startTime: String,
    val endTime: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val timeSpentSeconds: Long,
    val questions: List<QuestionResult>,
    val sessionType: String = "QUIZ_COMPLETO"
)

data class QuestionResult(
    val question: String,
    val userAnswer: String,
    val correctAnswer: String,
    val wasCorrect: Boolean,
    val operationType: String
)

class HistoryManager(private val context: Context) {
    private val prefs by lazy {
        context!!.getSharedPreferences("math_history", Context.MODE_PRIVATE)
    }
    private val gson = Gson()
    private val tag = "HistoryManager"

    fun saveSession(session: ExerciseSession) {
        try {
            val allSessions = getAllSessions().toMutableList()
            allSessions.add(session)

            val json = gson.toJson(allSessions)
            prefs.edit().putString("exercise_sessions", json).apply()

            Log.d(tag, "Sessão salva: ${session.correctAnswers}/${session.totalQuestions}")

        } catch (e: Exception) {
            Log.e(tag, "Erro ao salvar sessão", e)
        }
    }

    fun getAllSessions(): List<ExerciseSession> {
        return try {
            val json = prefs.getString("exercise_sessions", "[]") ?: "[]"
            val type = object : TypeToken<List<ExerciseSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Erro ao carregar sessões", e)
            emptyList()
        }
    }

    fun getTodaySessions(): List<ExerciseSession> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getAllSessions().filter { it.date == today }
    }

    fun getWeeklyStats(): Map<String, Int> {
        val sessions = getAllSessions()
        val stats = mutableMapOf<String, Int>()

        sessions.groupBy { it.date }.forEach { (date, daySessions) ->
            stats[date] = daySessions.sumOf { it.correctAnswers }
        }

        return stats
    }

    fun getTotalStats(): String {
        val sessions = getAllSessions()
        val totalCorrect = sessions.sumOf { it.correctAnswers }
        val totalQuestions = sessions.sumOf { it.totalQuestions }
        val totalTime = sessions.sumOf { it.timeSpentSeconds } / 60 // em minutos

        return "$totalCorrect/$totalQuestions acertos • ${totalTime}min"
    }

    fun clearHistory() {
        prefs.edit().remove("exercise_sessions").apply()
    }

    fun exportHistoryJson(): String {
        return gson.toJson(getAllSessions())
    }

    fun setParentPassword(newPassword: String) {
        prefs.edit().putString("parent_password", newPassword).apply()
    }

    fun checkParentPassword(password: String): Boolean {
        val savedPassword = prefs.getString("parent_password", "123456") // Senha padrão
        return password == savedPassword
    }

    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        return if (checkParentPassword(oldPassword)) {
            setParentPassword(newPassword)
            true
        } else {
            false
        }
    }
}