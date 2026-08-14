package com.viralcaption.ai.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viralcaption.ai.model.CaptionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CaptionRepository {
    private const val PREFS_NAME = "viral_captions_prefs"
    private const val KEY_HISTORY = "caption_history_json"
    private const val KEY_USAGE_DATE = "daily_usage_date"
    private const val KEY_USAGE_COUNT = "daily_usage_count"
    const val DAILY_FREE_LIMIT = 5
    private val gson = Gson()

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getDailyUsedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayString()
        val savedDate = prefs.getString(KEY_USAGE_DATE, null)
        if (savedDate != today) {
            prefs.edit().putString(KEY_USAGE_DATE, today).putInt(KEY_USAGE_COUNT, 0).apply()
            return 0
        }
        return prefs.getInt(KEY_USAGE_COUNT, 0)
    }

    fun getRemainingDailyGenerations(context: Context): Int {
        val used = getDailyUsedCount(context)
        return (DAILY_FREE_LIMIT - used).coerceAtLeast(0)
    }

    fun canGenerate(context: Context): Boolean {
        return getRemainingDailyGenerations(context) > 0
    }

    fun incrementDailyUsage(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayString()
        val current = getDailyUsedCount(context)
        val next = current + 1
        prefs.edit().putString(KEY_USAGE_DATE, today).putInt(KEY_USAGE_COUNT, next).apply()
        return (DAILY_FREE_LIMIT - next).coerceAtLeast(0)
    }

    fun getHistory(context: Context): List<CaptionResult> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<CaptionResult>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveItem(context: Context, item: CaptionResult): List<CaptionResult> {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        saveHistory(context, current)
        return current
    }

    fun deleteItem(context: Context, id: String): List<CaptionResult> {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == id }
        saveHistory(context, current)
        return current
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(context: Context, list: List<CaptionResult>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}
