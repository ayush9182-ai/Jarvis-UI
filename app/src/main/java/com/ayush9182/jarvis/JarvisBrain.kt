package com.ayush9182.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class JarvisBrain(private val context: Context) {
    private val prefs=context.getSharedPreferences("jarvis",Context.MODE_PRIVATE)
    fun apiKey()=prefs.getString("gemini_key","").orEmpty()
    fun saveApiKey(key:String){prefs.edit().putString("gemini_key",key).apply()}
    fun run(raw:String, done:(String)->Unit) { val c=raw.trim(); val l=c.lowercase()
        when {
            l.matches(Regex("(hi|hlo|hello|hey|namaste)( jarvis)?[.!?]*")) -> done("Hello Boss! Jarvis online hai. Aap kya karna chahte hain?")
            l.startsWith("open ") || l.startsWith("launch ") -> openApp(c.substringAfter(' ').lowercase(),done)
            l.startsWith("search ") -> { val q=c.substringAfter(' '); context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8")))); done("Web search khol diya, Boss.") }
            l.startsWith("call ") || l.startsWith("dial ") -> { val n=c.filter{it.isDigit()||it=='+'}; if(n.length>=7){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$n")));done("Dialer ready hai, Boss.")}else done("Boss, abhi number se call support hai. Contact name ke liye Contacts permission add karenge.") }
            l.startsWith("message ") || l.startsWith("sms ") -> { val p=c.substringAfter(' '); val n=p.takeWhile{it.isDigit()||it=='+'}; val body=p.removePrefix(n).trim(); if(n.length>=7){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:$n").apply{ })); done("SMS composer ready hai, Boss.")}else done("Format: message 9876543210 hello Boss") }
            l.contains("settings") -> {context.startActivity(Intent(Settings.ACTION_SETTINGS));done("Settings khol di, Boss.")}
            apiKey().isNotBlank() -> askGemini(c,done)
            else -> done("Boss, API key save kijiye; phir main intelligent answers dunga. Main apps, calls, SMS aur search bhi handle kar sakta hoon.")
        }
    }
    private fun openApp(name:String,done:(String)->Unit){ val packages=mapOf("youtube" to "com.google.android.youtube","whatsapp" to "com.whatsapp","instagram" to "com.instagram.android","spotify" to "com.spotify.music","chrome" to "com.android.chrome","maps" to "com.google.android.apps.maps") ; val p=packages.entries.firstOrNull{name.contains(it.key)}?.value; val i=p?.let{context.packageManager.getLaunchIntentForPackage(it)}; if(i!=null){context.startActivity(i);done("$name open kar diya, Boss.")}else{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(name,"UTF-8"))));done("$name phone par nahi mila, web search khol diya, Boss.")}}
    private fun askGemini(prompt:String,done:(String)->Unit)=CoroutineScope(Dispatchers.IO).launch{ try{ val url=URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey()}"); val con=url.openConnection() as HttpURLConnection;con.requestMethod="POST";con.doOutput=true;con.setRequestProperty("Content-Type","application/json"); val body=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("parts",org.json.JSONArray().put(JSONObject().put("text","You are Jarvis. Address the user as Boss. Reply concisely in Hinglish. User: $prompt"))))).toString();con.outputStream.use{it.write(body.toByteArray())}; val text=con.inputStream.bufferedReader().readText(); val answer=JSONObject(text).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text"); withContext(Dispatchers.Main){done(answer)}}catch(e:Exception){withContext(Dispatchers.Main){done("Boss, Gemini connection me problem aa gayi: ${e.message ?: "unknown error"}" )}}}
}
