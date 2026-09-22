package com.ayush9182.jarvis

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LlmClient(private val context: Context) {
    companion object {
        const val MODEL = "gemini-3.5-flash"
    }

    private val prefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    fun saveApiKey(key: String) = prefs.edit().putString("gemini_key", key).apply()
    fun apiKey() = prefs.getString("gemini_key", "").orEmpty()

    suspend fun answer(prompt: String, memory: String): String = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) {
            return@withContext "Boss, pehle API KEY save kijiye. HOW TO GET API KEY me steps diye hain."
        }

        val body = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            "You are Jarvis. Address the user as Boss. Reply in natural Hinglish unless asked otherwise. Handle conversation, reasoning and code questions accurately. Context:\n$memory\nUser:\n$prompt"
                        )
                    )
                )
            )
        )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${URLEncoder.encode(key, "UTF-8")}"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 45_000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val responseStream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val response = responseStream.bufferedReader().use { it.readText() }
        if (conn.responseCode !in 200..299) error("Gemini HTTP ${conn.responseCode}: $response")

        JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }
}
