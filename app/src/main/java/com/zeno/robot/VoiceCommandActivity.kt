package com.zeno.robot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zeno.robot.data.ZenoBrain
import java.util.Locale

class VoiceCommandActivity : ComponentActivity() {
    private lateinit var brain: ZenoBrain
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var handled = false

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDirectListening() else {
            Toast.makeText(this, "Autorise le microphone pour parler à Zeno.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = ZenoBrain(applicationContext)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startDirectListening()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun createRecognizer(): SpeechRecognizer {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }
    }

    private fun startDirectListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "La reconnaissance vocale Android n’est pas disponible.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        handled = false
        speechRecognizer?.destroy()
        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Toast.makeText(this@VoiceCommandActivity, "Zeno t’écoute…", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (handled) return
                    handled = true
                    val text = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Je n’ai pas compris."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je n’ai rien entendu."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "La reconnaissance vocale Android demande une connexion sur ce téléphone."
                        else -> "La reconnaissance vocale a rencontré un problème."
                    }
                    Toast.makeText(this@VoiceCommandActivity, text, Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onResults(results: Bundle?) {
                    if (handled) return
                    handled = true
                    val candidates = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (candidates.isEmpty()) {
                        Toast.makeText(this@VoiceCommandActivity, "Je n’ai pas compris.", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    Toast.makeText(this@VoiceCommandActivity, "Vous : ${candidates.first()}", Toast.LENGTH_SHORT).show()
                    val replyText = when (val reply = brain.replyCandidates(candidates)) {
                        is ZenoBrain.Result.Text -> reply.text
                        is ZenoBrain.Result.Action -> reply.text
                    }
                    Toast.makeText(this@VoiceCommandActivity, "Zeno : $replyText", Toast.LENGTH_LONG).show()
                    tts?.speak(replyText, TextToSpeech.QUEUE_FLUSH, null, "zeno_voice_command")
                    window.decorView.postDelayed({ finish() }, 1600)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure {
                Toast.makeText(this, "Impossible de démarrer le micro.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    override fun onDestroy() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
