from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

main = MAIN.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
service = SERVICE.read_text(encoding='utf-8')

# 1) Sauvegarde de la phrase : aucune action sur le service micro au clic Enregistrer.
#    Le fichier privé est la source principale, SharedPreferences n'est qu'une copie de secours.
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

# 2) Le service d'écoute permanente et l'écran de commande ne doivent jamais utiliser
#    le microphone en même temps. L'écran vocal arrête le service avant d'ouvrir le micro.
voice = voice.replace(
    'import com.zeno.robot.data.ZenoBrain\n',
    'import com.zeno.robot.data.ZenoBrain\nimport com.zeno.robot.service.FloatingZenoService\nimport android.provider.Settings\n'
)

old_permission_block = '''        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startDirectListening()
        } else {
            statusText = "J’ai besoin du microphone"
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
'''
new_permission_block = '''        // Libère d'abord le SpeechRecognizer permanent : deux recognizers simultanés
        // provoquent ERROR_RECOGNIZER_BUSY sur de nombreux téléphones.
        runCatching { stopService(Intent(this, FloatingZenoService::class.java)) }
        window.decorView.postDelayed({
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startDirectListening()
            } else {
                statusText = "J’ai besoin du microphone"
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }, 650)
'''
if old_permission_block not in voice:
    raise SystemExit('Bloc de démarrage vocal introuvable')
voice = voice.replace(old_permission_block, new_permission_block, 1)

old_create = '''    private fun createRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(this)
'''
if old_create not in voice:
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
            return
        }
        speechRecognizer = recognizer.apply {
            setRecognitionListener(object : RecognitionListener {
'''
if old_recognizer not in voice:
    raise SystemExit('Création SpeechRecognizer introuvable')
voice = voice.replace(old_recognizer, new_recognizer, 1)

# Redémarre l'écoute permanente seulement quand l'écran vocal est terminé.
insert_before_destroy = '''    private fun resumeFloatingVoice() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            Settings.canDrawOverlays(this)
        ) {
            runCatching {
                ContextCompat.startForegroundService(this, Intent(this, FloatingZenoService::class.java))
            }
        }
    }

'''
if 'private fun resumeFloatingVoice()' not in voice:
    voice = voice.replace('    override fun onDestroy() {', insert_before_destroy + '    override fun onDestroy() {', 1)

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
        resumeFloatingVoice()
        super.onDestroy()
    }
''',
1
)

# 3) Le service ne doit pas empiler plusieurs startListening ni recréer un recognizer
#    pendant qu'une commande directe utilise le micro.
service = service.replace(
'''        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        handler.postDelayed({ runCatching { startWakeListening() } }, 350)
''',
'''        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        if (!listening && !wakeTriggered) {
            handler.postDelayed({ runCatching { startWakeListening() } }, 650)
        }
''',
1
)

MAIN.write_text(main, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
SERVICE.write_text(service, encoding='utf-8')
print('Correctif appliqué : sauvegarde sûre et un seul utilisateur du microphone à la fois')
