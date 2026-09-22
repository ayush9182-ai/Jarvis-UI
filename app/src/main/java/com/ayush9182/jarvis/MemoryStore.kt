package com.ayush9182.jarvis

import android.content.Context
import android.content.SharedPreferences

class MemoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
    private val maxTurns = 10

    data class Turn(val role: String, val text: String)

    fun turns(): List<Turn> {
        val raw = prefs.getString("turns", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Turn(obj.optString("role"), obj.optString("text"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun remember(role: String, text: String) {
        val list = (turns() + Turn(role, text)).takeLast(maxTurns)
        val arr = org.json.JSONArray()
        list.forEach { turn ->
            arr.put(org.json.JSONObject().put("role", turn.role).put("text", turn.text.take(4000)))
        }
        prefs.edit().putString("turns", arr.toString()).apply()
    }

    fun clear() = prefs.edit().remove("turns").apply()
    fun contextText(): String = turns().joinToString("\n") { "${it.role}: ${it.text}" }
}
