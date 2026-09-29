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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zeno.robot.data.ZenoBrain
import java.util.Locale

class VoiceCommandActivity : ComponentActivity() {
    private lateinit var brain: ZenoBrain
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var handled = false
    private var statusText by mutableStateOf("Préparation de Zeno…")
    private var heardText by mutableStateOf("")

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDirectListening() else {
            statusText = "Microphone non autorisé"
            Toast.makeText(this, "Autorise le microphone pour parler à Zeno.", Toast.LENGTH_LONG).show()
            window.decorView.postDelayed({ finish() }, 1200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF32E6FF),
                    background = Color(0xFF020617),
                    surface = Color(0xFF07132D)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x5532E6FF),
                                    Color(0xFF071A3D),
                                    Color(0xFF020617)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(270.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0x6632E6FF), Color.Transparent)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.zeno_robot),
                                contentDescription = "Zeno",
                                modifier = Modifier.size(245.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "ZENO",
                            color = Color(0xFF32E6FF),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (heardText.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "« $heardText »",
                                color = Color(0xFFB6C9EA),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        brain = ZenoBrain(applicationContext)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startDirectListening()
        } else {
            statusText = "J’ai besoin du microphone"
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
            statusText = "Reconnaissance vocale indisponible"
            Toast.makeText(this, "La reconnaissance vocale Android n’est pas disponible.", Toast.LENGTH_LONG).show()
            window.decorView.postDelayed({ finish() }, 1400)
            return
        }

        handled = false
        statusText = "Je t’écoute…"
        heardText = ""
        speechRecognizer?.destroy()
        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText = "Je t’écoute…"
                }

                override fun onBeginningOfSpeech() {
                    statusText = "Je t’écoute…"
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    statusText = "Je réfléchis…"
                }

                override fun onError(error: Int) {
                    if (handled) return
                    handled = true
                    statusText = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Je n’ai pas compris"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je n’ai rien entendu"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Connexion vocale indisponible"
                        else -> "Problème de reconnaissance vocale"
                    }
                    Toast.makeText(this@VoiceCommandActivity, statusText, Toast.LENGTH_SHORT).show()
                    window.decorView.postDelayed({ finish() }, 1200)
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
                        statusText = "Je n’ai pas compris"
                        window.decorView.postDelayed({ finish() }, 1000)
                        return
                    }

                    heardText = candidates.first()
                    statusText = "Commande reçue"
                    val replyText = when (val reply = brain.replyCandidates(candidates)) {
                        is ZenoBrain.Result.Text -> reply.text
                        is ZenoBrain.Result.Action -> reply.text
                    }
                    statusText = replyText
                    tts?.speak(replyText, TextToSpeech.QUEUE_FLUSH, null, "zeno_voice_command")
                    window.decorView.postDelayed({ finish() }, 1800)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotBlank()) heardText = partial
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure {
                statusText = "Impossible de démarrer le micro"
                Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
                window.decorView.postDelayed({ finish() }, 1200)
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
