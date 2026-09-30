from pathlib import Path

SERVICE = Path("app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt")
s = SERVICE.read_text(encoding="utf-8")

old_create = '''        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 1000)
        } else stopSelf()
'''
new_create = '''        if (hasMic() && SpeechRecognizer.isRecognitionAvailable(this)) {
            if (Settings.canDrawOverlays(this)) showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 1000)
        }
'''
if old_create not in s:
    raise SystemExit("Bloc onCreate vocal introuvable")
s = s.replace(old_create, new_create, 1)

old_start = '''        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 700)
        }
'''
new_start = '''        if (hasMic() && SpeechRecognizer.isRecognitionAvailable(this)) {
            if (Settings.canDrawOverlays(this)) showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 700)
        }
'''
if old_start not in s:
    raise SystemExit("Bloc onStartCommand vocal introuvable")
s = s.replace(old_start, new_start, 1)

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
new_recognizer = '''    private fun createRecognizer(): SpeechRecognizer {
        // Le moteur système est plus compatible pour une écoute répétée en arrière-plan.
        // Le moteur 100 % local peut échouer si le modèle français n'est pas téléchargé.
        return SpeechRecognizer.createSpeechRecognizer(this)
    }
'''
if old_recognizer not in s:
    raise SystemExit("Bloc createRecognizer introuvable")
s = s.replace(old_recognizer, new_recognizer, 1)

old_error = '''                override fun onError(error: Int) {
                    listening = false
                    setBubbleListening(false)
                    if (System.currentTimeMillis() >= pauseWakeUntil) {
                        handler.postDelayed({ startWakeListening() }, 750)
                    }
                }
'''
new_error = '''                override fun onError(error: Int) {
                    listening = false
                    setBubbleListening(false)
                    if (System.currentTimeMillis() >= pauseWakeUntil) {
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1800L
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 4000L
                            SpeechRecognizer.ERROR_AUDIO -> 1800L
                            else -> 700L
                        }
                        handler.postDelayed({ startWakeListening() }, delay)
                    }
                }
'''
if old_error not in s:
    raise SystemExit("Bloc onError introuvable")
s = s.replace(old_error, new_error, 1)

old_intent = '''        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
'''
new_intent = '''        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putStringArrayListExtra(
                RecognizerIntent.EXTRA_BIASING_STRINGS,
                arrayListOf(currentWakePhrase(), "Salut Zeno", "Bonjour Zeno", "Hey Zeno")
            )
        }
'''
if old_intent not in s:
    raise SystemExit("Bloc recognitionIntent introuvable")
s = s.replace(old_intent, new_intent, 1)

old_open = '''    private fun openZeno() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
'''
new_open = '''    private fun openZeno() {
        // La phrase de réveil peut ramener Zeno au premier plan depuis l'accueil Android.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }
'''
if old_open not in s:
    raise SystemExit("Bloc openZeno introuvable")
s = s.replace(old_open, new_open, 1)

SERVICE.write_text(s, encoding="utf-8")
print("Reconnaissance vocale Zeno renforcée sur l'écran d'accueil Android")
