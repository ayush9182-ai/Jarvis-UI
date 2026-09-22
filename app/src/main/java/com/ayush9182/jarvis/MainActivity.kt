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
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.*
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
        intent.getStringExtra(EXTRA_COMMAND)?.let { commandInput.setText(it); handleCommand(it) }
    }

    private fun initUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(6,16,29)); setPadding(dp(22),dp(20),dp(22),dp(18)) }
        val top = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; layoutParams=LinearLayout.LayoutParams(-1,dp(60)) }
        top.addView(TextView(this).apply { text="JARVIS"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        top.addView(View(this), LinearLayout.LayoutParams(0,1,1f))
        top.addView(TextView(this).apply { text="● SYSTEM ONLINE"; textSize=10f; setTextColor(Color.rgb(139,233,215)); setPadding(dp(10),dp(6),dp(10),dp(6)); background=roundedBg(Color.rgb(18,37,58),999f) })
        root.addView(top)

        val orbWrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(-1,dp(330)) }
        orb=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; background=circleBg(); layoutParams=LinearLayout.LayoutParams(dp(250),dp(250)) }
        orb.addView(TextView(this).apply { text="J"; textSize=80f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(221,249,255)); gravity=Gravity.CENTER })
        orbWrap.addView(orb)
        statusText=TextView(this).apply { text="READY FOR COMMAND"; textSize=11f; setTextColor(Color.rgb(98,230,255)); typeface=Typeface.DEFAULT_BOLD; setPadding(0,dp(20),0,0); gravity=Gravity.CENTER }
        orbWrap.addView(statusText); root.addView(orbWrap)

        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)); background=roundedBg(Color.rgb(12,34,54),24f) }
        card.addView(TextView(this).apply { text="COMMAND CONSOLE"; textSize=12f; setTextColor(Color.rgb(98,230,255)); typeface=Typeface.DEFAULT_BOLD })
        card.addView(TextView(this).apply { text="What should I do?"; textSize=26f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; setPadding(0,dp(8),0,dp(18)) })
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; background=roundedBg(Color.rgb(16,39,61),18f); setPadding(dp(12),dp(8),dp(8),dp(8)) }
        row.addView(TextView(this).apply { text=">"; textSize=24f; setTextColor(Color.rgb(98,230,255)); setPadding(dp(8),0,dp(10),0) })
        commandInput=EditText(this).apply { hint="Try: call mom, open YouTube"; textSize=16f; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(122,152,179)); background=null; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        row.addView(commandInput)
        row.addView(Button(this).apply { text="↗"; textSize=22f; setTextColor(Color.rgb(6,16,29)); background=roundedBg(Color.rgb(207,248,255),16f); setOnClickListener{handleCommand(commandInput.text.toString())} })
        card.addView(row)
        val suggestions=listOf("Open YouTube","Call mom","Search the web","Focus timer")
        val chips=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.START; layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)} }
        suggestions.forEachIndexed { i,s -> chips.addView(Button(this).apply{text=s;textSize=12f;setTextColor(Color.rgb(214,231,248));background=roundedBg(Color.rgb(22,42,64),12f);setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener{commandInput.setText(s);handleCommand(s)}}); if(i<suggestions.lastIndex)chips.addView(View(this),LinearLayout.LayoutParams(dp(6),1)) }
        card.addView(chips); root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)})

        val feed=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(18)}}
        feed.addView(TextView(this).apply{text="LIVE FEED";textSize=12f;setTextColor(Color.rgb(98,230,255));typeface=Typeface.DEFAULT_BOLD})
        feed.addView(View(this),LinearLayout.LayoutParams(0,1,1f))
        feed.addView(Button(this).apply{text="Clear";setTextColor(Color.rgb(138,164,188));background=null;setOnClickListener{brain.clearHistory();responseText.text="Activity cleared."}}); root.addView(feed)
        root.addView(TextView(this).apply{text="Activity";textSize=38f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;setPadding(0,0,0,dp(12))})
        responseText=TextView(this).apply{text="Hello Boss! Jarvis online hai.";textSize=18f;setTextColor(Color.rgb(8,24,35));setPadding(dp(18),dp(18),dp(18),dp(18));background=roundedBg(Color.rgb(217,244,255),18f)}; root.addView(responseText)
        root.addView(Button(this).apply{text="🎙 Start listening";setTextColor(Color.BLACK);background=roundedBg(Color.rgb(188,239,255),16f);setOnClickListener{startListening()};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20)}})
        root.addView(Button(this).apply{text="⚙ API KEY";setTextColor(Color.WHITE);background=roundedBg(Color.rgb(15,39,64),12f);setOnClickListener{showApiDialog()};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(12)}})
        setContentView(root)
        startOrbAnimation()
    }

    private fun startOrbAnimation(){ val scale=ScaleAnimation(1f,1.06f,1f,1.06f,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f).apply{duration=1400;repeatMode=Animation.REVERSE;repeatCount=Animation.INFINITE}; orb.startAnimation(scale) }
    private fun handleCommand(raw:String){val text=raw.trim();if(text.isEmpty()){respond("Boss, command dijiye.");return};statusText.text="EXECUTING COMMAND";brain.execute(text){respond(it);statusText.text="READY FOR COMMAND"}}
    private fun respond(text:String){responseText.text=text;speak(text)}
    private fun speak(text:String){tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis")}
    private fun initTts(){tts=TextToSpeech(this){if(it==TextToSpeech.SUCCESS)tts?.language=Locale.ENGLISH}}

    private fun requestPermissionsIfNeeded(){val p=mutableListOf<String>();listOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS).forEach{if(ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED)p+=it};if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)p+=Manifest.permission.POST_NOTIFICATIONS;if(p.isNotEmpty())ActivityCompat.requestPermissions(this,p.toTypedArray(),101)}
    private fun startListening(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissionsIfNeeded();return};if(!SpeechRecognizer.isRecognitionAvailable(this)){respond("Boss, voice input available nahi hai.");return};if(isListening){recognizer?.stopListening();isListening=false;return};recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer?.setRecognitionListener(object:RecognitionListener{override fun onReadyForSpeech(b:Bundle?){isListening=true;statusText.text="LISTENING"};override fun onResults(b:Bundle?){val r=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();val c=r.replace(Regex("^(hey\\s+)?jarvis[ :,.-]*",RegexOption.IGNORE_CASE),"").trim();commandInput.setText(c);handleCommand(c);isListening=false};override fun onError(e:Int){isListening=false;statusText.text="READY FOR COMMAND";respond("Boss, voice capture me issue aayi.")};override fun onBeginningOfSpeech(){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){};override fun onPartialResults(b:Bundle?){};override fun onRmsChanged(v:Float){};override fun onEvent(t:Int,b:Bundle?){} });recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN");putExtra(RecognizerIntent.EXTRA_PROMPT,"Say: Hey Jarvis")})}
    private fun showApiDialog(){val box=EditText(this).apply{hint="Paste Gemini API key";inputType=InputType.TYPE_CLASS_TEXT;setText(brain.apiKey())};AlertDialog.Builder(this).setTitle("Jarvis API key").setMessage("Key local storage me save hogi.").setView(box).setPositiveButton("Save"){_,_->brain.saveApiKey(box.text.toString().trim());respond("API key saved, Boss.")}.setNegativeButton("Cancel",null).show()}
    private fun roundedBg(c:Int,r:Float)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;setColor(c);cornerRadius=r}
    private fun circleBg()=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(7,29,45));setStroke(dp(4),Color.rgb(95,233,255))}
    private fun dp(v:Int)=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,v.toFloat(),resources.displayMetrics).toInt()
    override fun onDestroy(){tts?.stop();tts?.shutdown();recognizer?.destroy();super.onDestroy()}
    companion object{const val EXTRA_COMMAND="jarvis_command"}
}
