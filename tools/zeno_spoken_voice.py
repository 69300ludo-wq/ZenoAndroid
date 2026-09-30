from pathlib import Path

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')

service = SERVICE.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')

# ---------------- Service vocal en arrière-plan ----------------
if 'import android.speech.tts.UtteranceProgressListener' not in service:
    service = service.replace(
        'import android.speech.tts.TextToSpeech\n',
        'import android.speech.tts.TextToSpeech\nimport android.speech.tts.UtteranceProgressListener\n',
        1
    )

field_old = '''    private var tts: TextToSpeech? = null
    private var listening = false
'''
field_new = '''    private var tts: TextToSpeech? = null
    private var pendingSpeechDone: (() -> Unit)? = null
    private var listening = false
'''
if field_old not in service:
    raise SystemExit('Champ TTS du service introuvable')
service = service.replace(field_old, field_new, 1)

init_old = '''        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
'''
init_new = '''        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(1.0f)
                val frenchVoice = tts?.voices?.firstOrNull {
                    it.locale.language == Locale.FRENCH.language && !it.isNetworkConnectionRequired
                }
                if (frenchVoice != null) tts?.voice = frenchVoice
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                handler.post {
                    val callback = pendingSpeechDone
                    pendingSpeechDone = null
                    callback?.invoke()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handler.post {
                    val callback = pendingSpeechDone
                    pendingSpeechDone = null
                    callback?.invoke()
                }
            }
        })
'''
if init_old not in service:
    raise SystemExit('Initialisation TTS du service introuvable')
service = service.replace(init_old, init_new, 1)

speak_old = '''    private fun speak(text: String, done: (() -> Unit)? = null) {
        Toast.makeText(this, "Zeno : $text", Toast.LENGTH_LONG).show()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zeno_reply")
        if (done != null) handler.postDelayed(done, 1300)
    }
'''
speak_new = '''    private fun speak(text: String, done: (() -> Unit)? = null) {
        Toast.makeText(this, "Zeno : $text", Toast.LENGTH_LONG).show()
        pendingSpeechDone = done
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zeno_reply_${System.currentTimeMillis()}")
        if (result == null || result == TextToSpeech.ERROR) {
            pendingSpeechDone = null
            if (done != null) handler.postDelayed(done, 700)
        }
    }
'''
if speak_old not in service:
    raise SystemExit('Fonction speak du service introuvable')
service = service.replace(speak_old, speak_new, 1)

# ---------------- Ecran de commande vocale ----------------
if 'import android.speech.tts.UtteranceProgressListener' not in voice:
    voice = voice.replace(
        'import android.speech.tts.TextToSpeech\n',
        'import android.speech.tts.TextToSpeech\nimport android.speech.tts.UtteranceProgressListener\n',
        1
    )

voice_init_old = '''        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
'''
voice_init_new = '''        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
                tts?.setSpeechRate(0.96f)
                tts?.setPitch(1.0f)
                val frenchVoice = tts?.voices?.firstOrNull {
                    it.locale.language == Locale.FRENCH.language && !it.isNetworkConnectionRequired
                }
                if (frenchVoice != null) tts?.voice = frenchVoice
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                runOnUiThread { finish() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                runOnUiThread { finish() }
            }
        })
'''
if voice_init_old not in voice:
    raise SystemExit('Initialisation TTS de VoiceCommandActivity introuvable')
voice = voice.replace(voice_init_old, voice_init_new, 1)

reply_old = '''                    statusText = replyText
                    tts?.speak(replyText, TextToSpeech.QUEUE_FLUSH, null, "zeno_voice_command")
                    window.decorView.postDelayed({ finish() }, 1800)
'''
reply_new = '''                    statusText = replyText
                    val spoken = tts?.speak(
                        replyText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "zeno_voice_command_${System.currentTimeMillis()}"
                    )
                    // Ne ferme plus l'écran au bout de 1,8 s : on attend que Zeno ait fini de parler.
                    if (spoken == null || spoken == TextToSpeech.ERROR) {
                        window.decorView.postDelayed({ finish() }, 2200)
                    }
'''
if reply_old not in voice:
    raise SystemExit('Réponse vocale de VoiceCommandActivity introuvable')
voice = voice.replace(reply_old, reply_new, 1)

SERVICE.write_text(service, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
print('Voix parlée Zeno activée et fiabilisée')
