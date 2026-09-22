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
    private lateinit var brain: JarvisBrain
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        }

        val brand = TextView(this).apply {
            text = "JARVIS"
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dp(10), 0)
        }
        topBar.addView(brand)

        val spacer = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
        topBar.addView(spacer)

        val online = TextView(this).apply {
            text = "SYSTEM ONLINE"
            setTextColor(Color.parseColor("#8BE9D7"))
            textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(Color.parseColor("#12253a"), 999f)
        }
        topBar.addView(online)
        root.addView(topBar)

        val orbWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(330)
            )
        }

        val orb = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = circleGlowBackground()
            layoutParams = LinearLayout.LayoutParams(dp(250), dp(250))
        }

        val orbLetter = TextView(this).apply {
            text = "J"
            textSize = 80f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.parseColor("#DDF9FF"))
            gravity = Gravity.CENTER
        }
        orb.addView(orbLetter)
        orbWrap.addView(orb)

        statusText = TextView(this).apply {
            text = "EXECUTING COMMAND"
            textSize = 11f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(20), 0, 0)
            gravity = Gravity.CENTER
        }
        orbWrap.addView(statusText)
        root.addView(orbWrap)

        val commandCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(Color.parseColor("#0c2236"), 24f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        val commandHeader = TextView(this).apply {
            text = "COMMAND CONSOLE"
            textSize = 12f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        commandCard.addView(commandHeader)

        val ask = TextView(this).apply {
            text = "What should I do?"
            textSize = 26f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(18))
        }
        commandCard.addView(ask)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBg(Color.parseColor("#10273d"), 18f)
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }

        val arrow = TextView(this).apply {
            text = ">"
            textSize = 24f
            setTextColor(Color.parseColor("#62e6ff"))
            setPadding(dp(8), 0, dp(10), 0)
        }
        inputRow.addView(arrow)

        commandInput = EditText(this).apply {
            hint = "Try: call mom, open YouTube, message Rahul I'm on my way"
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#7A98B3"))
            background = null
            isSingleLine = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(commandInput)

        val runBtn = Button(this).apply {
            text = "↗"
            textSize = 22f
            setTextColor(Color.parseColor("#06101D"))
            background = roundedBg(Color.parseColor("#CFF8FF"), 16f)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setOnClickListener { handleCommand(commandInput.text.toString()) }
        }
        inputRow.addView(runBtn)
        commandCard.addView(inputRow)

        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }

        val suggestions = listOf("Open YouTube", "Call mom", "Search the web", "Focus timer")
        suggestions.forEachIndexed { idx, suggestion ->
            val chip = Button(this).apply {
                text = suggestion
                textSize = 12f
                setTextColor(Color.parseColor("#D6E7F8"))
                background = roundedBg(Color.parseColor("#162a40"), 12f)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                minHeight = dp(42)
                setOnClickListener { commandInput.setText(suggestion); handleCommand(suggestion) }
            }
            chips.addView(chip)
            if (idx != suggestions.lastIndex) {
                val pad = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) }
                chips.addView(pad)
            }
        }
        commandCard.addView(chips)
        root.addView(commandCard)

        val feedHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(20) }
        }
        val liveText = TextView(this).apply {
            text = "LIVE FEED"
            textSize = 12f
            setTextColor(Color.parseColor("#62e6ff"))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        feedHeader.addView(liveText)
        val blank = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
        feedHeader.addView(blank)
        val clearBtn = Button(this).apply {
            text = "Clear"
            setTextColor(Color.parseColor("#8AA4BC"))
            background = null
            setOnClickListener { clearHistory() }
        }
        feedHeader.addView(clearBtn)
        root.addView(feedHeader)

        val activityTitle = TextView(this).apply {
            text = "Activity"
            textSize = 38f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(activityTitle)

        responseText = TextView(this).apply {
            text = "Hello Boss! Jarvis online hai."
            setTextColor(Color.parseColor("#DFF9FF"))
            textSize = 18f
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(Color.parseColor("#D9F4FF"), 18f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(responseText)

        val micButton = Button(this).apply {
            text = "🎙 Start listening"
            setTextColor(Color.BLACK)
            background = roundedBg(Color.parseColor("#BCEFFF"), 16f)
            setOnClickListener { startListening() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        }
        root.addView(micButton)

        val apiButton = Button(this).apply {
            text = "API KEY"
            setTextColor(Color.WHITE)
            background = roundedBg(Color.parseColor("#0F2740"), 12f)
            setOnClickListener { showApiDialog() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        root.addView(apiButton)

        setContentView(root)
    }

    private fun clearHistory() {
        brain.clearHistory()
        responseText.text = "Activity cleared."
    }

    private fun roundedBg(color: Int, radius: Float): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.setColor(color)
        d.cornerRadius = radius
        return d
    }

    private fun circleGlowBackground(): GradientDrawable {
        val gradient = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#071d2d"))
            setStroke(dp(4), Color.parseColor("#5FE9FF"))
        }
        gradient.setAlpha(230)
        return gradient
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun requestPermissionsIfNeeded() {
        val required = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            required += Manifest.permission.RECORD_AUDIO
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            required += Manifest.permission.CALL_PHONE
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            required += Manifest.permission.SEND_SMS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            required += Manifest.permission.POST_NOTIFICATIONS
        }
        if (required.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, required.toTypedArray(), 101)
        }
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            responseText.text = "Boss, command dijiye."
            speak("Boss, command dijiye.")
            return
        }

        statusText.text = "EXECUTING COMMAND"
        brain.execute(text) { result ->
            responseText.text = result
            speak(result)
            statusText.text = "READY FOR COMMAND"
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            responseText.text = "Boss, voice input available nahi hai is device par."
            return
        }

        if (isListening) {
            recognizer?.stopListening()
            isListening = false
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle?) {
                isListening = true
                statusText.text = "LISTENING"
                responseText.text = "Listening, Boss…"
            }

            override fun onResults(bundle: Bundle?) {
                val result = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!result.isNullOrBlank()) {
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
                responseText.text = "Boss, voice capture me issue aayi."
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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        recognizer?.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()
}
