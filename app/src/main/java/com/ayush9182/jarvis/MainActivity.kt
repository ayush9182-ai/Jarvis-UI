package com.ayush9182.jarvis

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var brain: JarvisBrain
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        brain = JarvisBrain(this)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.ENGLISH }
        buildUi()
        requestPermissions()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 24); setBackgroundColor(Color.rgb(6, 16, 29)) }
        root.addView(TextView(this).apply { text = "JARVIS 2.0\nBOSS MODE"; textSize = 26f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, 0, 0, 16) })
        status = TextView(this).apply { text = "● SYSTEM ONLINE"; textSize = 11f; setTextColor(Color.rgb(139, 233, 215)); setPadding(12, 8, 12, 8); background = bg(Color.rgb(18, 37, 58), 40f) }
        root.addView(status)
        root.addView(TextView(this).apply { text = "J"; textSize = 86f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(221, 249, 255)); background = bg(Color.rgb(7, 29, 45), 160f); layoutParams = LinearLayout.LayoutParams(-1, 230).apply { setMargins(0, 24, 0, 24) } })
        root.addView(TextView(this).apply { text = "COMMAND CONSOLE"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(98, 230, 255)) })
        input = EditText(this).apply { hint = "Boss, command dijiye…"; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(122, 152, 179)); minLines = 3; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; background = bg(Color.rgb(16, 39, 61), 18f); setPadding(16, 14, 16, 14) }
        root.addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("🎙 LISTEN") { listen() }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("EXECUTE") { execute() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        output = TextView(this).apply { text = "Hello Boss! Jarvis online hai."; textSize = 17f; setTextColor(Color.rgb(8, 24, 35)); setPadding(16, 18, 16, 18); background = bg(Color.rgb(217, 244, 255), 18f) }
        root.addView(output, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 })
        root.addView(button("⚙ API KEY") { apiDialog() })
        root.addView(button("? HOW TO GET API KEY") { guideDialog() })
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() }; setTextColor(if (label.contains("API") || label.contains("HOW")) Color.WHITE else Color.BLACK); background = bg(if (label.contains("API") || label.contains("HOW")) Color.rgb(15, 39, 64) else Color.rgb(188, 239, 255), 14f); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 } }
    private fun bg(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun execute() { val text = input.text.toString().trim(); if (text.isBlank()) return respond("Boss, command dijiye."); status.text = "● EXECUTING COMMAND"; brain.execute(text) { respond(it); status.text = "● READY FOR COMMAND" } }
    private fun respond(text: String) { output.text = text; tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }

    private fun listen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(); return }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return respond("Boss, voice input available nahi hai.")
        recognizer?.destroy(); recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { status.text = "● LISTENING" }
            override fun onResults(b: Bundle?) { val raw = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); input.setText(raw.replace(Regex("^(hey\\s+)?jarvis[ :,.-]*", RegexOption.IGNORE_CASE), "").trim()); execute() }
            override fun onError(e: Int) { status.text = "● READY FOR COMMAND"; respond("Boss, voice capture me issue aayi.") }
            override fun onBeginningOfSpeech() {} override fun onBufferReceived(b: ByteArray?) {} override fun onEndOfSpeech() {} override fun onPartialResults(b: Bundle?) {} override fun onRmsChanged(v: Float) {} override fun onEvent(t: Int, b: Bundle?) {}
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN") })
    }

    private fun apiDialog() {
        val box = EditText(this).apply { hint = "AIza..."; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(brain.apiKey()) }
        AlertDialog.Builder(this).setTitle("Paste Gemini API key").setMessage("Key is stored only on this device. Need help? Tap HOW TO GET API KEY.").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ -> brain.saveApiKey(box.text.toString().trim()); respond("API key saved, Boss.") }.show()
    }

    private fun guideDialog() {
        val message = "1. Google AI Studio open karo.\n\n2. Apne Google account se login karo.\n\n3. Create API key par tap karo.\n\n4. Create API key in new project choose karo.\n\n5. Generated key ke saamne Copy button dabao.\n\n6. Jarvis me API KEY button kholo, key paste karo aur Save dabao.\n\n7. Ab normal sawal pucho ya bolo: Hey Jarvis.\n\nSecurity: API key kisi ke saath share mat karo. Agar leak ho jaye to AI Studio se key delete karke nayi key banao."
        AlertDialog.Builder(this).setTitle("Jarvis setup guide").setMessage(message).setPositiveButton("Open AI Studio") { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/apikey"))) }.setNegativeButton("Close", null).show()
    }

    private fun requestPermissions() { val p = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS); if (android.os.Build.VERSION.SDK_INT >= 33) p += Manifest.permission.POST_NOTIFICATIONS; ActivityCompat.requestPermissions(this, p.toTypedArray(), 12) }
    override fun onDestroy() { tts?.shutdown(); recognizer?.destroy(); super.onDestroy() }
}
