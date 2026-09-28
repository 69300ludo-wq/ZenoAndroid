package com.zeno.robot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.zeno.robot.data.ZenoBrain
import java.util.Locale

class VoiceCommandActivity : ComponentActivity() {
    private lateinit var brain: ZenoBrain
    private var tts: TextToSpeech? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Je n’ai rien entendu.", Toast.LENGTH_SHORT).show()
            finish()
            return@registerForActivityResult
        }

        val sentence = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (sentence.isBlank()) {
            Toast.makeText(this, "Je n’ai pas compris.", Toast.LENGTH_SHORT).show()
            finish()
            return@registerForActivityResult
        }

        Toast.makeText(this, "Vous : $sentence", Toast.LENGTH_SHORT).show()
        val resultText = when (val reply = brain.reply(sentence)) {
            is ZenoBrain.Result.Text -> reply.text
            is ZenoBrain.Result.Action -> reply.text
        }
        Toast.makeText(this, "Zeno : $resultText", Toast.LENGTH_LONG).show()
        tts?.speak(resultText, TextToSpeech.QUEUE_FLUSH, null, "zeno_voice_command")
        window.decorView.postDelayed({ finish() }, 1800)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brain = ZenoBrain(applicationContext)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
        launchSpeech()
    }

    private fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Zeno")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(this, "La reconnaissance vocale Android n’est pas disponible.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
