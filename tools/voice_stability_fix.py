from pathlib import Path

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')

service = SERVICE.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
main = MAIN.read_text(encoding='utf-8')

old = '''        brain = ZenoBrain(applicationContext)
        createChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
        if (hasMic() && SpeechRecognizer.isRecognitionAvailable(this)) {
            if (Settings.canDrawOverlays(this)) showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 1000)
        }
'''
new = '''        brain = ZenoBrain(applicationContext)
        if (!hasMic()) {
            stopSelf()
            return
        }
        createChannel()
        val foregroundStarted = runCatching {
            startForeground(NOTIFICATION_ID, createNotification())
        }.isSuccess
        if (!foregroundStarted) {
            stopSelf()
            return
        }
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.FRENCH
        }
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (Settings.canDrawOverlays(this)) showBubble()
            prepareSpeechRecognizer()
            // Écoute quasi immédiate dès que le service Zeno est actif.
            handler.postDelayed({ startWakeListening() }, 150)
        }
'''
if old not in service:
    raise SystemExit('Bloc onCreate du service introuvable')
service = service.replace(old, new, 1)

old = '''        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
'''
new = '''        val recognizer = runCatching { createRecognizer() }.getOrNull() ?: return
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
'''
if old not in service:
    raise SystemExit('Création SpeechRecognizer service introuvable')
service = service.replace(old, new, 1)

old = '''                    if (System.currentTimeMillis() >= pauseWakeUntil) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1800L
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 4000L
                            SpeechRecognizer.ERROR_AUDIO -> 1800L
                            else -> 700L
                        }
                        handler.postDelayed({ startWakeListening() }, delay)
                    }
'''
new = '''                    if (System.currentTimeMillis() >= pauseWakeUntil) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> 700L
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 4000L
                            SpeechRecognizer.ERROR_AUDIO -> 500L
                            else -> 220L
                        }
                        handler.removeCallbacksAndMessages(null)
                        handler.postDelayed({
                            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                                runCatching { speechRecognizer?.destroy() }
                                speechRecognizer = null
                                prepareSpeechRecognizer()
                            }
                            startWakeListening()
                        }, delay)
                    }
'''
if old not in service:
    raise SystemExit('Gestion erreur vocale service introuvable')
service = service.replace(old, new, 1)

old = '''        runCatching { speechRecognizer?.startListening(recognitionIntent(partial = true)) }
            .onFailure { handler.postDelayed({ startWakeListening() }, 1000) }
'''
new = '''        runCatching { speechRecognizer?.startListening(recognitionIntent(partial = true)) }
            .onFailure {
                listening = false
                runCatching { speechRecognizer?.destroy() }
                speechRecognizer = null
                handler.postDelayed({
                    prepareSpeechRecognizer()
                    startWakeListening()
                }, 500)
            }
'''
if old not in service:
    raise SystemExit('startWakeListening introuvable')
service = service.replace(old, new, 1)

# Si une écoute se termine sans la phrase de réveil, repartir presque tout de suite.
service = service.replace(
    'handler.postDelayed({ startWakeListening() }, 450)',
    'handler.postDelayed({ startWakeListening() }, 100)',
    1
)

# Déclenchement direct : dès que la phrase est reconnue, Zeno s'ouvre et écoute la commande.
# On retire la réponse « Oui, je t’écoute » avant l'écoute, car elle ajoutait environ 1,3 s
# et pouvait être réentendue par le micro. Zeno garde sa voix pour répondre aux commandes.
old_trigger = '''        openZeno()
        speak("Oui, je t'écoute") {
            wakeTriggered = false
            handler.postDelayed({ beginCommandListening() }, 250)
        }
'''
new_trigger = '''        openZeno()
        wakeTriggered = false
        handler.postDelayed({ beginCommandListening() }, 50)
'''
if old_trigger not in service:
    raise SystemExit('Bloc de déclenchement vocal introuvable')
service = service.replace(old_trigger, new_trigger, 1)

old = '''    private fun createRecognizer(): SpeechRecognizer {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }
    }
'''
new = '''    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(this)
'''
if old not in voice:
    raise SystemExit('createRecognizer VoiceCommandActivity introuvable')
voice = voice.replace(old, new, 1)

old = '''        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
'''
new = '''        val recognizer = runCatching { createRecognizer() }.getOrNull()
        if (recognizer == null) {
            statusText = "Reconnaissance vocale indisponible"
            Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
            return
        }
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
'''
if old not in voice:
    raise SystemExit('Création VoiceCommandActivity introuvable')
voice = voice.replace(old, new, 1)

voice = voice.replace('.size((270f + (voiceLevel * 52f)).dp)', '.size(300.dp)', 1)

old = '''private fun startFloatingZeno(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
}
'''
new = '''private fun startFloatingZeno(context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        context.startActivity(Intent(context, SetupActivity::class.java))
        return
    }
    runCatching {
        ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
    }.onFailure {
        Toast.makeText(context, "Impossible de démarrer l'écoute vocale. Ouvre Zeno puis réessaie.", Toast.LENGTH_LONG).show()
    }
}
'''
if old not in main:
    raise SystemExit('startFloatingZeno introuvable')
main = main.replace(old, new, 1)

SERVICE.write_text(service, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
MAIN.write_text(main, encoding='utf-8')
print('Détection vocale rapide et déclenchement direct activés')
