package com.ayush9182.jarvis

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var commandInput: EditText
    private lateinit var statusText: TextView
    private lateinit var responseText: TextView
    private lateinit var brain: JarvisBrain
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = JarvisBrain(this)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.ENGLISH }
        buildUi()
        requestPermissionsIfNeeded()
        intent.getStringExtra(EXTRA_COMMAND)?.let { commandInput.setText(it); execute() }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(24))
            setBackgroundColor(Color.rgb(6, 16, 29))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(58))
        }
        header.addView(TextView(this).apply {
            text = "JARVIS"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(TextView(this).apply {
            text = "● SYSTEM ONLINE"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(139, 233, 215))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(Color.rgb(18, 37, 58), 999f)
        })
        root.addView(header)

        val orbContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(330))
        }
        // Live HUD: rotating rings, glow and status dots.
        orbContainer.addView(JarvisOrbView(this), LinearLayout.LayoutParams(dp(280), dp(280)))
        statusText = TextView(this).apply {
            text = "READY FOR COMMAND"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(98, 230, 255))
            setPadding(0, dp(12), 0, 0)
        }
        orbContainer.addView(statusText)
        root.addView(orbContainer)

        val console = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(Color.rgb(12, 34, 54), 24f)
        }
        console.addView(TextView(this).apply {
            text = "COMMAND CONSOLE"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(98, 230, 255))
        })
        console.addView(TextView(this).apply {
            text = "What should I do?"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(16))
        })

        commandInput = EditText(this).apply {
            hint = "Try: call 9876543210, open YouTube"
            textSize = 16f
            minLines = 2
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(122, 152, 179))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = roundedBg(Color.rgb(16, 39, 61), 18f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        console.addView(commandInput)
        console.addView(Button(this).apply {
            text = "EXECUTE"
            setTextColor(Color.rgb(6, 16, 29))
            background = roundedBg(Color.rgb(188, 239, 255), 14f)
            setOnClickListener { execute() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        })
        root.addView(console, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(TextView(this).apply {
            text = "LIVE FEED"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(98, 230, 255))
            setPadding(0, dp(20), 0, dp(8))
        })
        responseText = TextView(this).apply {
            text = "Hello Boss! Jarvis online hai."
            textSize = 17f
            setTextColor(Color.rgb(8, 24, 35))
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = roundedBg(Color.rgb(217, 244, 255), 18f)
        }
        root.addView(responseText)
        root.addView(Button(this).apply {
            text = "🎙 START LISTENING"
            setOnClickListener { startListening() }
            setTextColor(Color.BLACK)
            background = roundedBg(Color.rgb(188, 239, 255), 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
        })
        root.addView(Button(this).apply {
            text = "⚙ API KEY"
            setOnClickListener { showApiDialog() }
            setTextColor(Color.WHITE)
            background = roundedBg(Color.rgb(15, 39, 64), 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        })
        root.addView(Button(this).apply {
            text = "? HOW TO GET API KEY"
            setOnClickListener { showApiGuide() }
            setTextColor(Color.WHITE)
            background = roundedBg(Color.rgb(15, 39, 64), 14f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        })
        setContentView(root)
    }

    private fun execute() {
        val text = commandInput.text.toString().trim()
        if (text.isBlank()) return respond("Boss, command dijiye.")
        statusText.text = "EXECUTING COMMAND"
        brain.execute(text) { response ->
            respond(response)
            statusText.text = "READY FOR COMMAND"
        }
    }

    private fun respond(text: String) {
        responseText.text = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded(); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            respond("Boss, voice input available nahi hai."); return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { statusText.text = "LISTENING" }
            override fun onResults(results: Bundle?) {
                val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                commandInput.setText(raw.replace(Regex("^(hey\\s+)?jarvis[ :,.-]*", RegexOption.IGNORE_CASE), "").trim())
                execute()
            }
            override fun onError(error: Int) { statusText.text = "READY FOR COMMAND"; respond("Boss, voice capture me issue aayi.") }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        })
    }

    private fun showApiDialog() {
        val box = EditText(this).apply {
            hint = "AIza..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(brain.apiKey())
        }
        AlertDialog.Builder(this)
            .setTitle("Paste Gemini API key")
            .setMessage("Key is stored on this device. Use the setup guide if needed.")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> brain.saveApiKey(box.text.toString().trim()); respond("API key saved, Boss.") }
            .show()
    }

    private fun showApiGuide() {
        val guide = "1. Google AI Studio kholo.\n\n2. Google account se login karo.\n\n3. Create API key dabao.\n\n4. New project choose karo agar poocha jaye.\n\n5. Key ke saamne Copy dabao.\n\n6. Jarvis me API KEY kholo, paste karo aur Save dabao.\n\n7. Ab command do ya bolo: Hey Jarvis.\n\nAPI key share mat karna. Leak ho to AI Studio me revoke karke nayi key banao."
        AlertDialog.Builder(this).setTitle("Jarvis setup guide").setMessage(guide)
            .setPositiveButton("Open AI Studio") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }
            .setNegativeButton("Close", null).show()
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 12)
    }

    private fun roundedBg(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    override fun onDestroy() {
        tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    companion object { const val EXTRA_COMMAND = "jarvis_command" }
}
