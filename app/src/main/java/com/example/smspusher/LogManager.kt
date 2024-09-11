package com.example.smspusher

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private const val PREFS_NAME = "LogPrefs"
    private const val LOGS_KEY = "logs_key"

    fun addLog(context: Context, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, logEntry) // Add new log at the beginning
        _logs.value = currentLogs

        // Save logs to SharedPreferences
        saveLogs(context)
    }

    fun loadLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLogs = prefs.getStringSet(LOGS_KEY, emptySet())?.toList() ?: emptyList()
        _logs.value = savedLogs.sortedDescending() // Sort logs in reverse chronological order
    }

    fun clearLogs(context: Context) {
        _logs.value = emptyList()
        saveLogs(context)
    }

    private fun saveLogs(context: Context) {
        val sharedPreferences = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val logs = getAllLogs() // Retrieve all logs
        sharedPreferences.edit().putStringSet(LOGS_KEY, logs.toSet()).apply()
    }

    private fun getAllLogs(): List<String> {
        return _logs.value // Return the current logs stored in the MutableStateFlow
    }
}