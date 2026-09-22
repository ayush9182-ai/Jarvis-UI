package com.ayush9182.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var command: EditText
    private lateinit var reply: TextView
    private lateinit var brain: JarvisBrain
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(state: Bundle?) { super.onCreate(state); brain = JarvisBrain(this); buildUi() }
    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,34,28,24); setBackgroundColor(0xFF06101D.toInt()) }
        val title = TextView(this).apply { text="JARVIS 2.0\nANDROID COMMAND CENTER"; textSize=24f; setTextColor(0xFFBDF7FF.toInt()); gravity=Gravity.CENTER; setPadding(0,0,0,20) }
        root.addView(title)
        val api = Button(this).apply { text="⚙ API KEY / SETTINGS"; setOnClickListener { showApiDialog() } }
        root.addView(api, LinearLayout.LayoutParams(-1, -2))
        command = EditText(this).apply { hint="Boss, mujhe command dijiye…"; setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFF7890A8.toInt()); setSingleLine(false); minLines=3; setPadding(18,18,18,18) }
        root.addView(command, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin=22; bottomMargin=14 })
        val controls = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        val mic = Button(this).apply { text="🎙 LISTEN"; setOnClickListener { listen() } }
        val run = Button(this).apply { text="EXECUTE"; setOnClickListener { execute() } }
        controls.addView(mic, LinearLayout.LayoutParams(0,-2,1f)); controls.addView(run, LinearLayout.LayoutParams(0,-2,1f)); root.addView(controls)
        reply = TextView(this).apply { text="Jarvis online, Boss. Main aapka intezaar kar raha hoon."; textSize=17f; setTextColor(0xFFBDF7FF.toInt()); setPadding(4,26,4,10) }
        root.addView(reply)
        setContentView(root)
    }
    private fun execute() { val text=command.text.toString().trim(); if(text.isBlank()) { speak("Boss, command dijiye."); return }; reply.text="Boss, processing…"; brain.run(text) { reply.text=it; speak(it) } }
    private fun speak(text:String) { android.speech.tts.TextToSpeech(this) { } .also { it.speak("Boss, $text", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "jarvis") } }
    private fun listen() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 11); return }
        if(!SpeechRecognizer.isRecognitionAvailable(this)) { reply.text="Boss, is phone par speech recognition available nahi hai."; return }
        recognizer?.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.setRecognitionListener(object: RecognitionListener {
            override fun onResults(b:Bundle?) { val spoken=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); val clean=spoken.replace(Regex("^(hey\\s+)?jarvis[ ,]*", RegexOption.IGNORE_CASE),""); command.setText(clean); if(clean.isNotBlank()) execute() }
            override fun onError(e:Int) { reply.text="Boss, main sun nahi paaya. Dobara try kijiye." }
            override fun onReadyForSpeech(p:Bundle?) { reply.text="Listening, Boss…" }
            override fun onBeginningOfSpeech(){}; override fun onRmsChanged(v:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){}; override fun onPartialResults(b:Bundle?){}; override fun onEvent(t:Int,b:Bundle?){}
        })
        recognizer!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN") })
    }
    private fun showApiDialog() { val box=EditText(this).apply { hint="Paste Gemini API key"; inputType=129; setText(brain.apiKey()) }; AlertDialog.Builder(this).setTitle("Jarvis brain key").setMessage("Key device par locally save hogi. Source code me nahi jayegi.").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_-> brain.saveApiKey(box.text.toString().trim()); reply.text="API key saved, Boss." }.show() }
    override fun onDestroy(){ recognizer?.destroy(); super.onDestroy() }
}
