package com.ayush9182.jarvis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LlmClient(private val context: Context) {
    private val prefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    fun saveApiKey(key: String) = prefs.edit().putString("gemini_key", key).apply()
    fun apiKey(): String = prefs.getString("gemini_key", "").orEmpty()

    suspend fun answer(prompt: String, memory: String): String = withContext(Dispatchers.IO) {
        val backend = BuildConfig.BACKEND_URL.trimEnd('/')
        try {
            post("$backend/v1/assistant", JSONObject().put("message", prompt).put("memory", memory))
                .optString("reply").ifBlank { throw IllegalStateException("Empty backend reply") }
        } catch (_: Exception) {
            // Local fallback is useful for development; release should point BACKEND_URL to your proxy.
            if (apiKey().isBlank()) return@withContext "Boss, Gemini backend configured nahi hai. API key ya backend set kijiye."
            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are Jarvis. Address the user as Boss. Reply briefly in Hinglish. Context: $memory User: $prompt")))))
            val result = post("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey()}", body)
            result.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
        }
    }

    private fun post(endpoint: String, body: JSONObject): JSONObject {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray()) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) error("LLM HTTP ${connection.responseCode}: $response")
        return JSONObject(response)
    }
}
