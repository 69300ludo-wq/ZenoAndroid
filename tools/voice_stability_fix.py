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
            // Démarre presque immédiatement pour réduire le délai après activation du service.
            handler.postDelayed({ startWakeListening() }, 350)
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
                        // Relance plus vite l'écoute tout en laissant un petit délai au moteur
                        // lorsqu'il signale qu'il est encore occupé.
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> 900L
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 4000L
                            SpeechRecognizer.ERROR_AUDIO -> 700L
                            else -> 350L
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
                }, 650)
            }
'''
if old not in service:
    raise SystemExit('startWakeListening introuvable')
service = service.replace(old, new, 1)

# Si une écoute se termine sans la phrase de réveil, repartir rapidement.
service = service.replace(
    'handler.postDelayed({ startWakeListening() }, 450)',
    'handler.postDelayed({ startWakeListening() }, 220)',
    1
)

# Après le réveil, ouvrir l'écoute de la commande plus rapidement.
service = service.replace(
    'handler.postDelayed({ beginCommandListening() }, 250)',
    'handler.postDelayed({ beginCommandListening() }, 120)',
    1
)

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
print('Stabilité conservée et détection vocale accélérée')
