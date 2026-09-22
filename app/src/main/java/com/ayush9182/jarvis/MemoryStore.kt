package com.ayush9182.jarvis

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Short-term and durable user memory. Keep secrets out of this store. */
class MemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
    private val maxTurns = 10

    data class Turn(val role: String, val text: String)

    fun turns(): List<Turn> {
        val raw = prefs.getString("turns", "[]") ?: "[]"
        val json = JSONArray(raw)
        return (0 until json.length()).map { i ->
            val item = json.getJSONObject(i)
            Turn(item.optString("role"), item.optString("text"))
        }
    }

    fun remember(role: String, text: String) {
        val list = (turns() + Turn(role, text)).takeLast(maxTurns)
        val json = JSONArray()
        list.forEach { json.put(JSONObject().put("role", it.role).put("text", it.text.take(4000))) }
        prefs.edit().putString("turns", json.toString()).apply()
    }

    fun clear() = prefs.edit().remove("turns").apply()

    fun contextText(): String = turns().joinToString("\n") { "${it.role}: ${it.text}" }
}
