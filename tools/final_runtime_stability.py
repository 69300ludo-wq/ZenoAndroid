from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = p.read_text(encoding='utf-8')

# Ne jamais tuer tout le service uniquement parce que l'overlay n'est pas encore autorisé.
s = s.replace('''        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 1000)
        } else stopSelf()
''', '''        if (Settings.canDrawOverlays(this)) runCatching { showBubble() }
        if (hasMic() && SpeechRecognizer.isRecognitionAvailable(this)) {
            runCatching { prepareSpeechRecognizer() }
            handler.postDelayed({ startWakeListening() }, 300)
        }
''')

# Overlay : un échec WindowManager ne doit jamais faire planter Zeno.
s = s.replace('''        windowManager.addView(view, params)
        bubble = view
''', '''        runCatching {
            windowManager.addView(view, params)
            bubble = view
        }.onFailure {
            bubble = null
        }
''')

# Le réveil vocal ne doit pas ouvrir le menu ni faire parler Zeno par-dessus l'utilisateur.
start = '''        openZeno()
        speak("Oui, je t'écoute") {
            wakeTriggered = false
            handler.postDelayed({ beginCommandListening() }, 250)
        }
'''
s = s.replace(start, '''        wakeTriggered = false
        handler.postDelayed({ beginCommandListening() }, 500)
''')

# Reconnaissance standard = meilleure compatibilité entre constructeurs Android.
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
s = s.replace(old, '''    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(this)
''')

# Création protégée contre les exceptions constructeur/service vocal.
s = s.replace('''        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
''', '''        val recognizer = runCatching { createRecognizer() }.getOrNull() ?: return
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
''', 1)

# Plus de temps pour donner une commande complète.
s = s.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)', '''putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        if (!partial) {
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2400L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
        }''')

# Si le moteur plante/bloque, le recréer proprement au lieu de boucler sur le même objet.
s = s.replace('''        runCatching { speechRecognizer?.startListening(recognitionIntent(partial = true)) }
            .onFailure { handler.postDelayed({ startWakeListening() }, 1000) }
''', '''        runCatching { speechRecognizer?.startListening(recognitionIntent(partial = true)) }
            .onFailure {
                listening = false
                runCatching { speechRecognizer?.destroy() }
                speechRecognizer = null
                handler.postDelayed({
                    runCatching { prepareSpeechRecognizer() }
                    startWakeListening()
                }, 700)
            }
''')

# La commande vide redonne une chance au micro sans retourner immédiatement au réveil.
s = s.replace('''        if (sentence.isBlank()) {
            handler.postDelayed({ startWakeListening() }, 500)
            return
        }
''', '''        if (sentence.isBlank()) {
            handler.postDelayed({ beginCommandListening() }, 700)
            return
        }
''')

p.write_text(s, encoding='utf-8')
print('Correctif final runtime : overlay protégé, vocal protégé, menu supprimé au réveil')
