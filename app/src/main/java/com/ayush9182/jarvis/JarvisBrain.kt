package com.ayush9182.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class JarvisBrain(private val context: Context) {
    private val prefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)

    fun apiKey(): String = prefs.getString("gemini_key", "") ?: ""

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_key", key).apply()
    }

    fun execute(raw: String, callback: (String) -> Unit) {
        val command = raw.trim()
        if (command.isBlank()) {
            callback("Boss, command dijiye.")
            return
        }

        val lower = command.lowercase()

        when {
            lower.matches(Regex("^(hello|hi|hlo|helo|hey|namaste|yo|sup)(\s+jarvis)?[.!?]*$")) -> {
                callback("Hello Boss! Jarvis online hai. Aap kya karna chahte ho?")
            }

            lower.startsWith("open ") || lower.startsWith("launch ") -> {
                val target = command.removePrefix("open ").removePrefix("launch ").trim().lowercase()
                openApp(target, callback)
            }

            lower.startsWith("search ") || lower.startsWith("find ") || lower.startsWith("google ") -> {
                val target = command.removePrefix("search ").removePrefix("find ").removePrefix("google ").trim()
                val url = "https://www.google.com/search?q=${URLEncoder.encode(target, "UTF-8") }"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                callback("Web search khol diya, Boss.")
            }

            lower.startsWith("call ") || lower.startsWith("dial ") -> {
                val target = command.removePrefix("call ").removePrefix("dial ").trim()
                val number = target.filter { it.isDigit() || it == '+' }
                if (number.length >= 7) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    callback("Dialer open kar diya, Boss.")
                } else {
                    callback("Boss, contact name ke liye device contacts access chahiye. Abhi number format use kijiye: call 9876543210")
                }
            }

            lower.startsWith("message ") || lower.startsWith("sms ") || lower.startsWith("text ") -> {
                val target = command.removePrefix("message ").removePrefix("sms ").removePrefix("text ").trim()
                val number = target.takeWhile { it.isDigit() || it == '+' }
                val body = target.removePrefix(number).trim()
                if (number.length >= 7) {
                    val uri = Uri.parse("smsto:$number")
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", if (body.isBlank()) "Hello Boss!" else body)
                    }
                    context.startActivity(intent)
                    callback("SMS composer ready hai, Boss.")
                } else {
                    callback("Format: message 9876543210 hello Boss")
                }
            }

            lower.contains("settings") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                callback("Settings open kar diya, Boss.")
            }

            lower.contains("timer") || lower.matches(Regex(".*\\d+\\s*(minute|min|m).*")) -> {
                val minutes = Regex("(\\d+)").find(command)?.value?.toLong() ?: 5L
                callback("Timer set hai ${minutes} minute ke liye, Boss.")
            }

            apiKey().isNotBlank() -> {
                askGemini(command, callback)
            }

            else -> {
                callback("Boss, API key save kijiye; tab main intelligent answers doon. Main apps, calls, SMS, settings aur web search handle kar sakta hoon.")
            }
        }
    }

    private fun openApp(name: String, callback: (String) -> Unit) {
        val packages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera2",
            "telegram" to "org.telegram.messenger",
            "gmail" to "com.google.android.gm",
            "settings" to "com.android.settings"
        )

        val match = packages.entries.firstOrNull { name.contains(it.key) }
        val packageName = match?.value

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                callback("${match.key} open kar diya, Boss.")
                return
            }
        }

        val searchUrl = "https://www.google.com/search?q=${URLEncoder.encode(name, "UTF-8") }"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
        callback("App nahi mila, web search khol diya, Boss.")
    }

    private fun askGemini(prompt: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val key = apiKey()
                val payload = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put(
                            "parts",
                            org.json.JSONArray().put(JSONObject().put("text", "You are Jarvis. Address the user as Boss. Keep the reply brief and in Hinglish. User: $prompt"))
                        )
                    ))
                }

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }

                val response = conn.inputStream.bufferedReader().readText()
                val answer = JSONObject(response)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                withContext(Dispatchers.Main) {
                    callback(answer.trim())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Boss, Gemini se connection fail hui: ${e.message ?: "unknown error"}")
                }
            }
        }
    }
}
