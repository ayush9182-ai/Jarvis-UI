package com.ayush9182.jarvis

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var input: EditText
    private lateinit var response: TextView
    private lateinit var brain: JarvisBrain
    private var speech: TextToSpeech? = null
    private var rec: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = JarvisBrain(this)
        buildUi()
        initTts()
        requestPermsIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 30)
            setBackgroundColor(0xFF06101D.toInt())
        }

        val header = TextView(this).apply {
            text = "JARVIS 2.0"
            textSize = 26f
            setTextColor(0xFFBDF7FF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "Boss mode • Android command center"
            textSize = 14f
            setTextColor(0xFF7FA2B8.toInt())
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)

        val apiBtn = Button(this).apply {
            text = "⚙ API KEY"
            setOnClickListener { showApiDialog() }
        }
        root.addView(apiBtn)

        val serviceBtn = Button(this).apply {
            text = "🟢 START BOSS MODE"
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, JarvisService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                response.text = "Boss mode active. Say: Hey Jarvis"
            }
        }
        root.addView(serviceBtn)

        input = EditText(this).apply {
            hint = "Boss, command dijiye…"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF7A92A8.toInt())
            minLines = 3
            setPadding(18, 18, 18, 18)
        }
        root.addView(input)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }

        val micBtn = Button(this).apply {
            text = "🎙 LISTEN"
            setOnClickListener { startListening() }
        }
        val executeBtn = Button(this).apply {
            text = "EXECUTE"
            setOnClickListener { handleCommand(input.text.toString()) }
        }

        actions.addView(micBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(executeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)

        response = TextView(this).apply {
            text = "Jarvis online, Boss. Main aapka assistant hoon."
            textSize = 16f
            setTextColor(0xFFBCEEFF.toInt())
            setPadding(0, 24, 0, 0)
        }
        root.addView(response)

        setContentView(root)
    }

    private fun requestPermsIfNeeded() {
        val needsAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        val needsSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        val needsNotif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

        val list = mutableListOf<String>()
        if (needsAudio) list += Manifest.permission.RECORD_AUDIO
        if (needsCall) list += Manifest.permission.CALL_PHONE
        if (needsSms) list += Manifest.permission.SEND_SMS
        if (needsNotif) list += Manifest.permission.POST_NOTIFICATIONS
        if (list.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 101)
        }
    }

    private fun initTts() {
        speech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.language = Locale.ENGLISH
            }
        }
    }

    private fun speak(text: String) {
        if (speech == null) return
        speech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            response.text = "Boss, command dijiye."
            speak("Boss, command dijiye.")
            return
        }

        response.text = "Processing…"
        brain.execute(text) { result ->
            response.text = result
            speak(result)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermsIfNeeded()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            response.text = "Boss, is device par speech input available nahi hai."
            return
        }

        if (isListening) {
            rec?.stopListening()
            isListening = false
            return
        }

        rec = SpeechRecognizer.createSpeechRecognizer(this)
        rec?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val voice = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (voice.isNotBlank()) {
                    val cleaned = voice.replace(Regex("^(hey\\s+)?jarvis[ :,.-]*", RegexOption.IGNORE_CASE), "").trim()
                    input.setText(cleaned)
                    handleCommand(cleaned)
                }
                isListening = false
            }

            override fun onError(error: Int) {
                response.text = "Boss, voice capture me issue aayi."
                isListening = false
            }

            override fun onReadyForSpeech(p0: Bundle?) { response.text = "Listening, Boss…" }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(p0: Float) { }
            override fun onBufferReceived(p0: ByteArray?) { }
            override fun onEndOfSpeech() { }
            override fun onPartialResults(p0: Bundle?) { }
            override fun onEvent(p0: Int, p1: Bundle?) { }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Hey Jarvis")
        }

        rec?.startListening(intent)
        isListening = true
    }

    private fun showApiDialog() {
        val inputBox = EditText(this).apply {
            hint = "Paste Gemini API key"
            setText(brain.apiKey())
        }

        AlertDialog.Builder(this)
            .setTitle("Jarvis brain key")
            .setMessage("Gemini API key local storage me save hogi.")
            .setView(inputBox)
            .setPositiveButton("Save") { _, _ ->
                val key = inputBox.text.toString().trim()
                brain.saveApiKey(key)
                response.text = if (key.isNotBlank()) "API key saved, Boss." else "API key removed."
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        speech?.stop()
        speech?.shutdown()
        rec?.destroy()
        super.onDestroy()
    }
}
