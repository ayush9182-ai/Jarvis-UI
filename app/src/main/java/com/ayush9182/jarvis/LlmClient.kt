package com.ayush9182.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.*
import org.json.*
import java.net.*

class LlmClient(private val context: Context) {
    companion object { const val MODEL = "gemini-3.5-flash" }
    private val prefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    fun saveApiKey(key: String) = prefs.edit().putString("gemini_key", key).apply()
    fun apiKey() = prefs.getString("gemini_key", "").orEmpty()

    suspend fun answer(prompt: String, memory: String): String = withContext(Dispatchers.IO) {
        if (apiKey().isBlank()) return@withContext "Boss, pehle API KEY save kijiye. HOW TO GET API KEY me steps diye hain."
        val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are Jarvis. Address the user as Boss. Answer conversation, reasoning and code questions accurately. Reply in natural Hinglish unless the user asks otherwise. Context:\n$memory\nUser:\n$prompt")))))
        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=${URLEncoder.encode(apiKey(), "UTF-8")}").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"; conn.connectTimeout = 15000; conn.readTimeout = 45000; conn.setRequestProperty("Content-Type", "application/json"); conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (conn.responseCode !in 200..299) error("Gemini HTTP ${conn.responseCode}")
        JSONObject(response).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
    }
}
