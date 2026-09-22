package com.ayush9182.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class JarvisService : Service() {
    private val handler=Handler(Looper.getMainLooper())
    private var recognizer:SpeechRecognizer?=null
    private var keepListening=false
    override fun onCreate(){super.onCreate();createChannel();startForeground(1,notification());}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{keepListening=true;startWakeListener();return START_STICKY}
    private fun startWakeListener(){if(!keepListening||!SpeechRecognizer.isRecognitionAvailable(this))return;recognizer?.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer?.setRecognitionListener(object:RecognitionListener{override fun onResults(b:Bundle?){val text=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();val match=Regex("(?:hey\\s+)?jarvis[ ,;:!-]*(.*)",RegexOption.IGNORE_CASE).find(text);if(match!=null){val command=match.groupValues.getOrNull(1).orEmpty().trim();val i=Intent(this@JarvisService,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP);if(command.isNotBlank())i.putExtra(MainActivity.EXTRA_COMMAND,command);startActivity(i);notifyUser("Jarvis activated${if(command.isNotBlank())": $command" else ""}")};restart()};override fun onError(e:Int){restart()};override fun onReadyForSpeech(b:Bundle?){ };override fun onBeginningOfSpeech(){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){};override fun onPartialResults(b:Bundle?){};override fun onRmsChanged(v:Float){};override fun onEvent(t:Int,b:Bundle?){} });recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)})}
    private fun restart(){handler.removeCallbacksAndMessages(null);if(keepListening)handler.postDelayed({startWakeListener()},700)}
    private fun notifyUser(text:String){(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2,NotificationCompat.Builder(this,"jarvis_channel").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Jarvis Boss Mode").setContentText(text).setAutoCancel(true).build())}
    private fun notification():Notification=NotificationCompat.Builder(this,"jarvis_channel").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Jarvis Boss Mode").setContentText("Listening for: Hey Jarvis").setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    private fun createChannel(){if(android.os.Build.VERSION.SDK_INT>=26)(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("jarvis_channel","Jarvis Boss Mode",NotificationManager.IMPORTANCE_LOW))}
    override fun onDestroy(){keepListening=false;handler.removeCallbacksAndMessages(null);recognizer?.destroy();super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
