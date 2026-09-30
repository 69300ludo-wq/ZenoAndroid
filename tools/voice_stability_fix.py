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
        val foregroundStarted = runCatching { startForeground(NOTIFICATION_ID, createNotification()) }.isSuccess
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
            handler.postDelayed({ startWakeListening() }, 120)
        }
'''
if old not in service: raise SystemExit('Bloc onCreate introuvable')
service = service.replace(old, new, 1)

old = '''        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
'''
new = '''        val recognizer = runCatching { createRecognizer() }.getOrNull() ?: return
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
'''
if old not in service: raise SystemExit('Recognizer service introuvable')
service = service.replace(old, new, 1)

old = '''                    if (System.currentTimeMillis() >= pauseWakeUntil) {
                        handler.postDelayed({ startWakeListening() }, 750)
                    }
'''
new = '''                    if (!wakeMode) {
                        handler.postDelayed({ beginCommandListening() }, 500)
                    } else if (System.currentTimeMillis() >= pauseWakeUntil) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> 650L
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 4000L
                            SpeechRecognizer.ERROR_AUDIO -> 450L
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 120L
                            else -> 220L
                        }
                        handler.postDelayed({ startWakeListening() }, delay)
                    }
'''
if old not in service: raise SystemExit('Gestion erreur introuvable')
service = service.replace(old, new, 1)

service = service.replace('handler.postDelayed({ startWakeListening() }, 450)', 'handler.postDelayed({ startWakeListening() }, 100)', 1)

old_trigger = '''        openZeno()
        speak("Oui, je t'écoute") {
            wakeTriggered = false
            handler.postDelayed({ beginCommandListening() }, 250)
        }
'''
new_trigger = '''        // Ne pas ouvrir le menu : passe directement à la commande.
        wakeTriggered = false
        handler.postDelayed({ beginCommandListening() }, 350)
'''
if old_trigger not in service: raise SystemExit('Déclenchement introuvable')
service = service.replace(old_trigger, new_trigger, 1)

old_intent = '''        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
'''
new_intent = '''        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        if (!partial) {
            // Laisse le temps de prononcer une commande complète, même avec une petite pause.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1600L)
        }
'''
if old_intent not in service: raise SystemExit('Intent reconnaissance introuvable')
service = service.replace(old_intent, new_intent, 1)

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
new = '''    private fun createRecognizer(): SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
'''
if old not in voice: raise SystemExit('createRecognizer activité introuvable')
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
if old not in voice: raise SystemExit('Recognizer activité introuvable')
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
    runCatching { ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java)) }
        .onFailure { Toast.makeText(context, "Impossible de démarrer l'écoute vocale. Ouvre Zeno puis réessaie.", Toast.LENGTH_LONG).show() }
}
'''
if old not in main: raise SystemExit('startFloatingZeno introuvable')
main = main.replace(old, new, 1)

SERVICE.write_text(service, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
MAIN.write_text(main, encoding='utf-8')
print('Reconnaissance vocale Zeno améliorée : écoute rapide et commande plus longue')
