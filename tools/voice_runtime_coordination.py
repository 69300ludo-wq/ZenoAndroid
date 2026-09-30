from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

main = MAIN.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
service = SERVICE.read_text(encoding='utf-8')

# 1) Sauvegarde fiable de la phrase, sans toucher au cycle de vie du service micro.
old_init = '''    val prefs = remember { context.getSharedPreferences("zeno_voice", Context.MODE_PRIVATE) }
    var wakePhrase by remember { mutableStateOf(prefs.getString("wake_phrase", "Salut Zeno") ?: "Salut Zeno") }
'''
new_init = '''    val prefs = remember { context.getSharedPreferences("zeno_voice", Context.MODE_PRIVATE) }
    val savedWakePhrase = remember {
        runCatching {
            java.io.File(context.filesDir, "wake_phrase.txt")
                .takeIf { it.exists() }
                ?.readText()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: prefs.getString("wake_phrase", "Salut Zeno") ?: "Salut Zeno"
    }
    var wakePhrase by remember { mutableStateOf(savedWakePhrase) }
'''
if old_init in main:
    main = main.replace(old_init, new_init, 1)

old_save = '''                        wakePhrase = clean
                        prefs.edit().putString("wake_phrase", clean).apply()
                        runCatching {
                            java.io.File(context.filesDir, "wake_phrase.txt").writeText(clean)
                        }
                        editWakePhrase = false
                        Toast.makeText(context, "Phrase enregistrée", Toast.LENGTH_SHORT).show()
'''
new_save = '''                        val saved = runCatching {
                            java.io.File(context.filesDir, "wake_phrase.txt").writeText(clean)
                            prefs.edit().putString("wake_phrase", clean).commit()
                            true
                        }.getOrDefault(false)
                        if (saved) {
                            wakePhrase = clean
                            editWakePhrase = false
                            Toast.makeText(context, "Phrase enregistrée", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Impossible d'enregistrer la phrase", Toast.LENGTH_LONG).show()
                        }
'''
if old_save not in main:
    raise SystemExit('Bloc de sauvegarde final introuvable')
main = main.replace(old_save, new_save, 1)

# 2) Coordination robuste entre le service flottant et l'activité vocale.
if 'import com.zeno.robot.service.VoiceSessionCoordinator' not in voice:
    voice = voice.replace(
        'import com.zeno.robot.data.ZenoBrain\n',
        'import com.zeno.robot.data.ZenoBrain\nimport com.zeno.robot.service.VoiceSessionCoordinator\n',
        1
    )

if 'VoiceSessionCoordinator.directVoiceActive = true' not in voice:
    voice = voice.replace(
        '        super.onCreate()\n',
        '        super.onCreate()\n        VoiceSessionCoordinator.directVoiceActive = true\n',
        1
    )

old_create = '''    private fun createRecognizer(): SpeechRecognizer {
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
new_create = '''    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(this)
'''
voice = voice.replace(old_create, new_create, 1)

old_recognizer = '''        speechRecognizer?.destroy()
        speechRecognizer = createRecognizer().apply {
            setRecognitionListener(object : RecognitionListener {
'''
new_recognizer = '''        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        val recognizer = runCatching { createRecognizer() }.getOrNull()
        if (recognizer == null) {
            statusText = "Impossible d'initialiser le microphone"
            Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
            window.decorView.postDelayed({ finish() }, 1200)
            return
        }
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
'''
if old_recognizer not in voice:
    raise SystemExit('Création SpeechRecognizer introuvable')
voice = voice.replace(old_recognizer, new_recognizer, 1)

voice = voice.replace(
'''    override fun onDestroy() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
''',
'''    override fun onDestroy() {
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching { tts?.shutdown() }
        tts = null
        VoiceSessionCoordinator.directVoiceActive = false
        super.onDestroy()
    }
''',
1
)
if 'VoiceSessionCoordinator.directVoiceActive = false' not in voice:
    raise SystemExit('Libération de la session vocale introuvable')

# 3) Le service libère son recognizer avant d'ouvrir le vocal direct et ne le recrée
# que lorsque l'activité vocale a réellement terminé.
old_open = '''    private fun openReliableVoiceCommand() {
        pauseWakeUntil = System.currentTimeMillis() + 5000L
        speechRecognizer?.cancel()
        listening = false
        setBubbleListening(false)
        startActivity(
            Intent(this, VoiceCommandActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        handler.postDelayed({ startWakeListening() }, 5200)
    }
'''
new_open = '''    private fun openReliableVoiceCommand() {
        pauseWakeUntil = Long.MAX_VALUE
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        listening = false
        wakeTriggered = false
        wakeMode = true
        setBubbleListening(false)

        VoiceSessionCoordinator.directVoiceActive = true
        val opened = runCatching {
            startActivity(
                Intent(this, VoiceCommandActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

        if (!opened) {
            VoiceSessionCoordinator.directVoiceActive = false
            pauseWakeUntil = 0L
        }

        waitForDirectVoiceToFinish()
    }

    private fun waitForDirectVoiceToFinish() {
        handler.postDelayed({
            if (VoiceSessionCoordinator.directVoiceActive) {
                waitForDirectVoiceToFinish()
            } else {
                pauseWakeUntil = 0L
                if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
                if (!listening && !wakeTriggered) runCatching { startWakeListening() }
            }
        }, 700)
    }
'''
if old_open not in service:
    raise SystemExit('Bloc openReliableVoiceCommand introuvable')
service = service.replace(old_open, new_open, 1)

old_start = '''    private fun startWakeListening() {
        if (System.currentTimeMillis() < pauseWakeUntil) return
'''
new_start = '''    private fun startWakeListening() {
        if (VoiceSessionCoordinator.directVoiceActive) return
        if (System.currentTimeMillis() < pauseWakeUntil) return
'''
if old_start not in service:
    raise SystemExit('startWakeListening introuvable')
service = service.replace(old_start, new_start, 1)

service = service.replace(
'''        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        handler.postDelayed({ runCatching { startWakeListening() } }, 350)
''',
'''        if (speechRecognizer == null && !VoiceSessionCoordinator.directVoiceActive) {
            runCatching { prepareSpeechRecognizer() }
        }
        if (!listening && !wakeTriggered && !VoiceSessionCoordinator.directVoiceActive) {
            handler.postDelayed({ runCatching { startWakeListening() } }, 650)
        }
''',
1
)

service = service.replace(
'''    private fun prepareSpeechRecognizer() {
        if (speechRecognizer != null || !SpeechRecognizer.isRecognitionAvailable(this)) return
''',
'''    private fun prepareSpeechRecognizer() {
        if (VoiceSessionCoordinator.directVoiceActive) return
        if (speechRecognizer != null || !SpeechRecognizer.isRecognitionAvailable(this)) return
''',
1
)

MAIN.write_text(main, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
SERVICE.write_text(service, encoding='utf-8')
print('Correctif appliqué : session micro exclusive, reprise automatique sans minuterie fixe')
