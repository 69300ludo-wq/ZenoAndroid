from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = p.read_text(encoding='utf-8')

# Remplace entièrement le démarrage du service par une version protégée.
start = s.index('    override fun onCreate() {')
end = s.index('    private fun createNotification()', start)
safe_start = '''    override fun onCreate() {
        super.onCreate()

        // Sur Android récent, un service micro lancé sans permission peut tuer le processus.
        // On vérifie tout AVANT startForeground et on arrête proprement au moindre problème.
        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        brain = ZenoBrain(applicationContext)

        val foregroundOk = runCatching {
            createChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        }.isSuccess
        if (!foregroundOk) {
            stopSelf()
            return
        }

        tts = runCatching {
            TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching { tts?.language = Locale.FRENCH }
                }
            }
        }.getOrNull()

        // Le point lumineux est facultatif : s'il échoue, le vocal continue sans planter.
        if (Settings.canDrawOverlays(this)) runCatching { showBubble() }
        runCatching { prepareSpeechRecognizer() }
        handler.postDelayed({ runCatching { startWakeListening() } }, 500)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasMic()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (Settings.canDrawOverlays(this) && bubble == null) runCatching { showBubble() }
        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        handler.postDelayed({ runCatching { startWakeListening() } }, 350)
        // Evite une relance automatique du service par Android dans un état invalide.
        return START_NOT_STICKY
    }

'''
s = s[:start] + safe_start + s[end:]

# Overlay : aucun WindowManager ne doit pouvoir faire planter le processus.
s = s.replace('''        windowManager.addView(view, params)
        bubble = view
''', '''        runCatching {
            windowManager.addView(view, params)
            bubble = view
        }.onFailure {
            bubble = null
        }
''')

# Le réveil vocal ne doit ouvrir AUCUN menu et ne doit pas parler par-dessus l'utilisateur.
old_trigger = '''        openZeno()
        speak("Oui, je t'écoute") {
            wakeTriggered = false
            handler.postDelayed({ beginCommandListening() }, 250)
        }
'''
s = s.replace(old_trigger, '''        wakeTriggered = false
        handler.postDelayed({ beginCommandListening() }, 550)
''')

# Compatibilité maximale : moteur vocal Android standard, création protégée.
old_recognizer = '''    private fun createRecognizer(): SpeechRecognizer {
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
s = s.replace(old_recognizer, '''    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(this)
''')
s = s.replace('''        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
''', '''        val recognizer = runCatching { createRecognizer() }.getOrNull() ?: return
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
''', 1)

# Plus de temps pour finir une commande.
s = s.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)')
needle = '        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)\n'
if needle in s and 'EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS' not in s:
    s = s.replace(needle, needle + '''        if (!partial) {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1600L)
        }
''', 1)

# Ne jamais ouvrir une activité de secours si le moteur vocal a un raté : on retente proprement.
s = s.replace('''        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            openReliableVoiceCommand()
            return
        }
''', '''        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handler.postDelayed({ startWakeListening() }, 1000)
            return
        }
''')
s = s.replace('''            runCatching { speechRecognizer?.startListening(recognitionIntent(partial = false)) }
                .onFailure { openReliableVoiceCommand() }
''', '''            runCatching { speechRecognizer?.startListening(recognitionIntent(partial = false)) }
                .onFailure {
                    listening = false
                    wakeMode = true
                    handler.postDelayed({ startWakeListening() }, 700)
                }
''')

# Destruction toujours protégée.
s = s.replace('''        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
''', '''        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching { tts?.shutdown() }
''')

p.write_text(s, encoding='utf-8')
print('Service Zeno sécurisé : permissions avant FGS, overlay optionnel, vocal sans ouverture de menu')
