package com.zeno.robot.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zeno.robot.MainActivity
import com.zeno.robot.R
import com.zeno.robot.SetupActivity
import com.zeno.robot.data.ZenoBrain
import java.util.Locale
import kotlin.math.abs

class FloatingZenoService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: ImageView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var listening = false
    private lateinit var brain: ZenoBrain

    override fun onCreate() {
        super.onCreate()
        brain = ZenoBrain(applicationContext)
        createChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }

        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zeno)
            .setContentTitle("Zeno est à l’écran")
            .setContentText("Touchez Zeno pour parler. Appui long pour ouvrir l’application.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showBubble() {
        if (bubble != null || !Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = (104 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.zeno_robot)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            elevation = 18f
            contentDescription = "Zeno flottant"
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 260
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var downAt = 0L

        view.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    downAt = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - touchX) + abs(event.rawY - touchY)
                    val duration = System.currentTimeMillis() - downAt
                    if (moved < 18f * resources.displayMetrics.density) {
                        if (duration >= 650L) openZeno() else beginListening()
                    }
                    true
                }

                else -> false
            }
        }

        windowManager.addView(view, params)
        bubble = view
    }

    private fun prepareSpeechRecognizer() {
        if (speechRecognizer != null) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    setBubbleListening(true)
                    Toast.makeText(this@FloatingZenoService, "Zeno t’écoute…", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    listening = false
                    setBubbleListening(false)
                    val text = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Je n’ai pas compris. Retouche-moi et reparle."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je n’ai rien entendu."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "La reconnaissance vocale n’a pas de réseau."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Je suis déjà en train d’écouter."
                        else -> "La reconnaissance vocale a rencontré un problème."
                    }
                    Toast.makeText(this@FloatingZenoService, text, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    setBubbleListening(false)
                    val sentence = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()

                    if (sentence.isBlank()) return
                    Toast.makeText(this@FloatingZenoService, "Vous : $sentence", Toast.LENGTH_SHORT).show()
                    val result = brain.reply(sentence)
                    val reply = when (result) {
                        is ZenoBrain.Result.Text -> result.text
                        is ZenoBrain.Result.Action -> result.text
                    }
                    speak(reply)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun beginListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Autorise le microphone pour parler à Zeno.", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(this, SetupActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "La reconnaissance vocale Android n’est pas disponible sur ce téléphone.", Toast.LENGTH_LONG).show()
            return
        }

        prepareSpeechRecognizer()
        if (listening) {
            speechRecognizer?.cancel()
            listening = false
            setBubbleListening(false)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Zeno")
        }
        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure {
                Toast.makeText(this, "Impossible de démarrer le micro.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setBubbleListening(active: Boolean) {
        bubble?.animate()
            ?.scaleX(if (active) 1.18f else 1f)
            ?.scaleY(if (active) 1.18f else 1f)
            ?.alpha(if (active) 0.90f else 1f)
            ?.setDuration(180)
            ?.start()
    }

    private fun speak(text: String) {
        Toast.makeText(this, "Zeno : $text", Toast.LENGTH_LONG).show()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zeno_floating_reply")
    }

    private fun openZeno() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_description) }
        )
    }

    companion object {
        private const val CHANNEL_ID = "zeno_floating"
        private const val NOTIFICATION_ID = 4201
    }
}
