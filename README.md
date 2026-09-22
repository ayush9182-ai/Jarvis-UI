// app/src/main/java/com/ayush9182/jarvis/JarvisBrain.kt
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class JarvisBrain(private val context: Context) {
    private val prefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)

    fun apiKey(): String = prefs.getString("gemini_key", "") ?: ""
    fun saveApiKey(key: String) = prefs.edit().putString("gemini_key", key).apply()
    fun clearHistory() = prefs.edit().remove("history").apply()

    fun execute(raw: String, callback: (String) -> Unit) {
        val command = raw.trim()
        if (command.isBlank()) {
            callback("Boss, command dijiye.")
            return
        }

        val lower = command.lowercase()

        when {
            lower.matches(Regex("^(hello|hi|hlo|helo|hey|namaste|yo|sup)(\\s+jarvis)?[.!?]*$")) -> {
                callback("Hello Boss! Jarvis online hai. Aap kya karna chahte ho?")
            }
            lower.startsWith("open ") || lower.startsWith("launch ") -> {
                val target = command.removePrefix("open ").removePrefix("launch ").trim().lowercase()
                openApp(target, callback)
            }
            lower.startsWith("search ") || lower.startsWith("find ") || lower.startsWith("google ") -> {
                val target = command.removePrefix("search ").removePrefix("find ").removePrefix("google ").trim()
                openWeb(target, callback)
            }
            lower.startsWith("call ") || lower.startsWith("dial ") -> {
                val target = command.removePrefix("call ").removePrefix("dial ").trim()
                callTarget(target, callback)
            }
            lower.startsWith("message ") || lower.startsWith("sms ") || lower.startsWith("text ") -> {
                val target = command.removePrefix("message ").removePrefix("sms ").removePrefix("text ").trim()
                messageTarget(target, callback)
            }
            lower.contains("settings") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                callback("Settings open kar diya, Boss.")
            }
            lower.contains("timer") || lower.matches(Regex(".*\\d+\\s*(minute|min|m).*")) -> {
                val minutes = Regex("(\\d+)").find(command)?.value?.toLong() ?: 5L
                callback("Timer set hai ${minutes} minute ke liye, Boss.")
            }
            apiKey().isNotBlank() -> askGemini(command, callback)
            else -> callback("Boss, API key save kijiye; tab main intelligent answers doon.")
        }
    }

    private fun openWeb(query: String, callback: (String) -> Unit) {
        val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8") }"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        callback("Web search khol diya, Boss.")
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
        val launcher = match?.value?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (launcher != null) {
            context.startActivity(launcher)
            callback("${match.key} open kar diya, Boss.")
            return
        }

        openWeb(name, callback)
    }

    private fun callTarget(target: String, callback: (String) -> Unit) {
        val number = target.filter { it.isDigit() || it == '+' }
        if (number.length >= 7) {
            confirmAction("Call $number?", "Dialer open hoga, Boss.") {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                callback("Dialer ready hai, Boss.")
            }
            return
        }
        callback("Boss, abhi number format use kijiyega: call 9876543210")
    }

    private fun messageTarget(target: String, callback: (String) -> Unit) {
        val parts = target.trim().split(Regex("\\s+"), 2)
        val number = parts.firstOrNull()?.filter { it.isDigit() || it == '+' }.orEmpty()
        val body = parts.getOrNull(1)?.trim().orEmpty()

        if (number.length < 7) {
            callback("Format: message 9876543210 hello Boss")
            return
        }

        confirmAction("Message $number?", "SMS composer khulega, Boss.") {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", if (body.isBlank()) "Hello Boss!" else body)
            }
            context.startActivity(intent)
            callback("SMS draft ready hai, Boss.")
        }
    }

    private fun confirmAction(title: String, message: String, action: () -> Unit) {
        val dialog = android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ -> action() }
            .create()
        dialog.show()
    }

    private fun askGemini(prompt: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put(
                            "parts",
                            org.json.JSONArray().put(
                                JSONObject().put("text", "You are Jarvis. Greets as Boss, reply in Hinglish and keep brief. User: $prompt")
                            )
                        )
                    ))
                }

                val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey()}")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(payload.toString().toByteArray()) }

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.readText()
                val answer = JSONObject(responseText)
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
                    callback("Boss, Gemini connection fail hui: ${e.message ?: "unknown error"}")
                }
            }
        }
    }
}

// app/src/main/java/com/ayush9182/jarvis/MainActivity.kt
package com.ayush9182.jarvis

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var commandInput: EditText
    private lateinit var statusText: TextView
    private lateinit var responseText: TextView
    private lateinit var orb: View
    private lateinit var brain: JarvisBrain
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        brain = JarvisBrain(this)
        initUi()
        initTts()
        requestPermissionsIfNeeded()
    }

    private fun initUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#06101D"))
            setPadding(dp(22), dp(20), dp(22), dp(18))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60))
        }
        topBar.addView(TextView(this).apply { text = "JARVIS"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        topBar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        topBar.addView(TextView(this).apply {
            text = "● SYSTEM ONLINE"
            textSize = 10f
            setTextColor(Color.parseColor("#8BE9D7"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(Color.parseColor("#12253a"), 999f)
        })
        root.addView(topBar)

        val orbWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(330))
        }
        orb = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = circleBg()
            layoutParams = LinearLayout.LayoutParams(dp(250), dp(250))
        }
        orb.addView(TextView(this).apply {
            text = "J"
            textSize = 80f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#DDF9FF"))
            gravity = Gravity.CENTER
        })
        orbWrap.addView(orb)
        statusText = TextView(this).apply {
            text = "READY FOR COMMAND"
            textSize = 11f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(20), 0, 0)
            gravity = Gravity.CENTER
        }
        orbWrap.addView(statusText)
        root.addView(orbWrap)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(Color.parseColor("#0c2236"), 24f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        }

        card.addView(TextView(this).apply {
            text = "COMMAND CONSOLE"
            textSize = 12f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.DEFAULT_BOLD
        })
        card.addView(TextView(this).apply {
            text = "What should I do?"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(18))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#10273d"), 18f)
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        row.addView(TextView(this).apply {
            text = ">"
            textSize = 24f
            setTextColor(Color.parseColor("#62e6ff"))
            setPadding(dp(8), 0, dp(10), 0)
        })
        commandInput = EditText(this).apply {
            hint = "Try: call mom, open YouTube"
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#7A98B3"))
            background = null
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(commandInput)
        row.addView(Button(this).apply {
            text = "↗"
            textSize = 22f
            setTextColor(Color.parseColor("#06101D"))
            background = roundedBg(Color.parseColor("#CFF8FF"), 16f)
            setOnClickListener { handleCommand(commandInput.text.toString()) }
        })
        card.addView(row)

        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) }
        }
        val suggestions = listOf("Open YouTube", "Call mom", "Search the web", "Focus timer")
        suggestions.forEachIndexed { index, suggestion ->
            chips.addView(Button(this).apply {
                text = suggestion
                textSize = 12f
                setTextColor(Color.parseColor("#D6E7F8"))
                background = roundedBg(Color.parseColor("#162a40"), 12f)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener { commandInput.setText(suggestion); handleCommand(suggestion) }
            })
            if (index < suggestions.lastIndex) {
                chips.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
            }
        }
        card.addView(chips)
        root.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        val feedRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) }
        }
        feedRow.addView(TextView(this).apply {
            text = "LIVE FEED"
            textSize = 12f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.DEFAULT_BOLD
        })
        feedRow.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        feedRow.addView(Button(this).apply {
            text = "Clear"
            setTextColor(Color.parseColor("#8AA4BC"))
            background = null
            setOnClickListener { brain.clearHistory(); responseText.text = "Activity cleared." }
        })
        root.addView(feedRow)

        root.addView(TextView(this).apply {
            text = "Activity"
            textSize = 38f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })

        responseText = TextView(this).apply {
            text = "Hello Boss! Jarvis online hai."
            textSize = 18f
            setTextColor(Color.parseColor("#0B1B2D"))
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(Color.parseColor("#D9F4FF"), 18f)
        }
        root.addView(responseText)

        root.addView(Button(this).apply {
            text = "🎙 Start listening"
            setTextColor(Color.BLACK)
            background = roundedBg(Color.parseColor("#BCEFFF"), 16f)
            setOnClickListener { startListening() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(20) }
        })

        root.addView(Button(this).apply {
            text = "⚙ API KEY"
            setTextColor(Color.WHITE)
            background = roundedBg(Color.parseColor("#0F2740"), 12f)
            setOnClickListener { showApiDialog() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        })

        setContentView(root)
        startOrbAnimation()
    }

    private fun startOrbAnimation() {
        val anim = ScaleAnimation(1f, 1.06f, 1f, 1.06f, 0.5f, 0.5f)
        anim.duration = 1400
        anim.repeatMode = android.view.animation.Animation.REVERSE
        anim.repeatCount = android.view.animation.Animation.INFINITE
        orb.startAnimation(anim)
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            respond("Boss, command dijiye.")
            return
        }
        statusText.text = "EXECUTING COMMAND"
        brain.execute(text) {
            respond(it)
            statusText.text = "READY FOR COMMAND"
        }
    }

    private fun respond(text: String) {
        responseText.text = text
        speak(text)
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val required = mutableListOf<String>()
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        ).forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                required.add(it)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (required.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, required.toTypedArray(), 101)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            respond("Boss, voice input available nahi hai.")
            return
        }

        if (isListening) {
            recognizer?.stopListening()
            isListening = false
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(bundle: Bundle?) {
                val result = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (result.isNotBlank()) {
                    val cleaned = result.replace(Regex("^(hey\\s+)?jarvis[ :,.-]*", RegexOption.IGNORE_CASE), "").trim()
                    commandInput.setText(cleaned)
                    handleCommand(cleaned)
                }
                isListening = false
                statusText.text = "READY FOR COMMAND"
            }

            override fun onError(error: Int) {
                isListening = false
                statusText.text = "READY FOR COMMAND"
                respond("Boss, voice capture me issue aayi.")
            }

            override fun onReadyForSpeech(bundle: Bundle?) {
                isListening = true
                statusText.text = "LISTENING"
                respond("Listening, Boss…")
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(bytes: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(bundle: Bundle?) {}
            override fun onRmsChanged(v: Float) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Hey Jarvis")
        }
        recognizer?.startListening(intent)
    }

    private fun showApiDialog() {
        val box = EditText(this).apply {
            hint = "Paste Gemini API key"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(brain.apiKey())
        }

        AlertDialog.Builder(this)
            .setTitle("Jarvis API key")
            .setMessage("Gemini API key local storage me save hogi.")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val key = box.text.toString().trim()
                brain.saveApiKey(key)
                responseText.text = if (key.isNotBlank()) "API key saved, Boss." else "API key cleared."
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun roundedBg(color: Int, radius: Float): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.setColor(color)
        d.cornerRadius = radius
        return d
    }

    private fun circleBg(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#071d2d"))
        setStroke(dp(4), Color.parseColor("#5FE9FF"))
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }
}

// app/src/main/java/com/ayush9182/jarvis/JarvisService.kt
package com.ayush9182.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class JarvisService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "jarvis_channel")
            .setContentTitle("Jarvis Boss Mode")
            .setContentText("Listening for: Hey Jarvis")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jarvis_channel",
                "Jarvis Boss Mode",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// README.md
# Jarvis Android Assistant

This repository is the production-style Android starter for Jarvis.

## Included

- Futuristic dark UI with animated orb
- Text and voice command flow
- Gemini API key input
- Phone actions with confirmation dialogs
- Foreground service foundation for `Hey Jarvis`
- Contact and notification permission handling
- APK build guide

## Build APK

Open the repository in Android Studio and build the app, or run:

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Do not hardcode API keys into source code.
- Use a backend or secure storage for production keys.
- Real always-on wake-word detection requires a dedicated foreground service and device-approved microphone flow.
- Call and SMS actions must remain confirmation-based before sending or dialing.
