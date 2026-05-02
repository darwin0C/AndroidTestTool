package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryEntry(
    val id: Long,
    val category: String,
    val title: String,
    val input: String,
    val content: String,
    val timestamp: Long,
)

object HistoryStore {
    private const val PREF_NAME = "tool_history"
    private const val KEY_ENTRIES = "entries"

    fun load(context: Context): List<HistoryEntry> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                HistoryEntry(
                    id = item.optLong("id"),
                    category = item.optString("category"),
                    title = item.optString("title"),
                    input = item.optString("input"),
                    content = item.optString("content"),
                    timestamp = item.optLong("timestamp"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, category: String, title: String, input: String, content: String) {
        val entry = HistoryEntry(
            id = System.currentTimeMillis(),
            category = category,
            title = title,
            input = input,
            content = content,
            timestamp = System.currentTimeMillis(),
        )
        val updated = listOf(entry) + load(context)
        save(context, updated.take(SettingsStore.load(context).historyLimit))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ENTRIES)
            .apply()
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(timestamp))
    }

    private fun save(context: Context, entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("category", entry.category)
                    .put("title", entry.title)
                    .put("input", entry.input)
                    .put("content", entry.content)
                    .put("timestamp", entry.timestamp),
            )
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, array.toString())
            .apply()
    }
}
