package com.ayush9182.jarvis

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
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
    private val prefs=context.getSharedPreferences("jarvis",Context.MODE_PRIVATE)
    fun apiKey()=prefs.getString("gemini_key","").orEmpty()
    fun saveApiKey(key:String){prefs.edit().putString("gemini_key",key).apply()}
    fun clearHistory(){prefs.edit().remove("history").apply()}
    fun execute(raw:String,callback:(String)->Unit){val c=raw.trim();if(c.isBlank()){callback("Boss, command dijiye.");return};val l=c.lowercase();when{
        l.matches(Regex("^(hello|hi|hlo|helo|hey|namaste|yo|sup)(\\s+jarvis)?[.!?]*$"))->callback("Hello Boss! Jarvis online hai. Aap kya karna chahte ho?")
        l.startsWith("open ")||l.startsWith("launch ")->openApp(c.removePrefix("open ").removePrefix("launch ").trim().lowercase(),callback)
        l.startsWith("search ")||l.startsWith("find ")||l.startsWith("google ")->openWeb(c.substringAfter(' '),callback)
        l.startsWith("call ")||l.startsWith("dial ")->callTarget(c.substringAfter(' '),callback)
        l.startsWith("message ")||l.startsWith("sms ")||l.startsWith("text ")->messageTarget(c.substringAfter(' '),callback)
        l.contains("settings")-> {context.startActivity(Intent(Settings.ACTION_SETTINGS));callback("Settings open kar diya, Boss.")}
        l.contains("timer")||l.matches(Regex(".*\\d+\\s*(minute|min|m).*"))->callback("Timer set hai ${Regex("(\\d+)").find(c)?.value?:5} minute ke liye, Boss.")
        apiKey().isNotBlank()->askGemini(c,callback)
        else->callback("Boss, API key save kijiye; phir main intelligent answers dunga.")
    }}
    private fun openWeb(q:String,cb:(String)->Unit){context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8"))));cb("Web search khol diya, Boss.")}
    private fun callTarget(target:String,cb:(String)->Unit){val number=target.filter{it.isDigit()||it=='+'};if(number.length>=7)confirm("Call $number?", "Dialer open hoga. Call aap manually confirm karoge, Boss."){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$number")));cb("Dialer ready hai, Boss.")}else{val found=findContact(target);if(found!=null)confirm("Call ${found.first}?","Dialer open hoga. Confirm karna aapke control me rahega, Boss."){context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${found.second}")));cb("${found.first} ka dialer ready hai, Boss.")}else cb("Boss, contact nahi mila. Number try kijiye: call 9876543210")}}
    private fun messageTarget(target:String,cb:(String)->Unit){val parts=target.trim().split(Regex("\\s+"),limit=2);val first=parts.firstOrNull().orEmpty();val number=first.filter{it.isDigit()||it=='+'};val body=parts.getOrNull(1).orEmpty();val contact=if(number.length>=7)Pair(first,number)else findContact(first);if(contact==null){cb("Boss, contact nahi mila. Format: message 9876543210 hello");return};confirm("Message ${contact.first}?","SMS composer khulega; send karne se pehle aap confirm karoge, Boss."){context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${contact.second}")).apply{putExtra("sms_body",if(body.isBlank())"Hello Boss!" else body)});cb("SMS draft ready hai, Boss.")}}
    private fun findContact(query:String):Pair<String,String>?{if(query.isBlank())return null;var result:Pair<String,String>?=null;val resolver:ContentResolver=context.contentResolver;val cursor:Cursor?=resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),"${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",arrayOf("%$query%"),null);cursor?.use{if(it.moveToFirst())result=Pair(it.getString(0),it.getString(1))};return result}
    private fun confirm(title:String,message:String,yes:()->Unit){android.os.Handler(android.os.Looper.getMainLooper()).post{android.app.AlertDialog.Builder(context).setTitle(title).setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Continue"){_,_->yes()}.show()}}
    private fun openApp(name:String,cb:(String)->Unit){val p=mapOf("youtube" to "com.google.android.youtube","whatsapp" to "com.whatsapp","instagram" to "com.instagram.android","spotify" to "com.spotify.music","chrome" to "com.android.chrome","maps" to "com.google.android.apps.maps","camera" to "com.android.camera2","telegram" to "org.telegram.messenger","gmail" to "com.google.android.gm","settings" to "com.android.settings");val match=p.entries.firstOrNull{name.contains(it.key)};val i=match?.value?.let{context.packageManager.getLaunchIntentForPackage(it)};if(i!=null){context.startActivity(i);cb("${match.key} open kar diya, Boss.")}else openWeb(name){cb("App nahi mila, web search khol diya, Boss.")}}
    private fun askGemini(prompt:String,cb:(String)->Unit)=CoroutineScope(Dispatchers.IO).launch{try{val payload=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("parts",org.json.JSONArray().put(JSONObject().put("text","You are Jarvis. Address user as Boss. Reply briefly in Hinglish. User: $prompt")))));val con=URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey()}").openConnection() as HttpURLConnection;con.requestMethod="POST";con.setRequestProperty("Content-Type","application/json");con.doOutput=true;con.outputStream.use{it.write(payload.toString().toByteArray())};val text=JSONObject(con.inputStream.bufferedReader().readText()).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");withContext(Dispatchers.Main){cb(text.trim())}}catch(e:Exception){withContext(Dispatchers.Main){cb("Boss, Gemini connection fail hui: ${e.message?:"unknown error"}")}}}
}
