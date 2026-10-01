from pathlib import Path

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')

# Le bouton micro de l'accueil doit utiliser exactement le même SpeechRecognizer
# que le réveil "Salut Zeno". On évite ainsi un deuxième moteur vocal concurrent.
service = SERVICE.read_text(encoding='utf-8')
signature = '    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {\n'
command_gate = '''        if (intent?.action == "com.zeno.robot.action.DIRECT_COMMAND") {
            requestDirectCommand()
            return START_STICKY
        }
'''
if 'com.zeno.robot.action.DIRECT_COMMAND' not in service:
    if signature not in service:
        raise SystemExit('onStartCommand du service vocal introuvable')
    service = service.replace(signature, signature + command_gate, 1)

request_fn = '''    private fun requestDirectCommand() {
        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Microphone indisponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Coupe proprement l'écoute de "Salut Zeno" puis réutilise le même moteur
        // pour écouter la commande demandée depuis l'écran d'accueil.
        pauseWakeUntil = Long.MAX_VALUE
        wakeTriggered = true
        wakeMode = false
        runCatching { speechRecognizer?.cancel() }
        listening = false
        prepareSpeechRecognizer()
        setBubbleListening(true)

        handler.postDelayed({
            wakeTriggered = false
            runCatching { beginCommandListening() }
            // Laisse le temps au recognizer de passer réellement en mode commande
            // avant d'autoriser une future reprise du réveil vocal.
            handler.postDelayed({ pauseWakeUntil = 0L }, 1800)
        }, 420)
    }

'''
if 'private fun requestDirectCommand()' not in service:
    anchor = '    private fun setBubbleListening(active: Boolean) {'
    if anchor not in service:
        raise SystemExit('Emplacement requestDirectCommand introuvable')
    service = service.replace(anchor, request_fn + anchor, 1)
SERVICE.write_text(service, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')
home_activity_call = 'context.startActivity(Intent(context, VoiceCommandActivity::class.java))'
if home_activity_call in main:
    main = main.replace(home_activity_call, 'startVoiceCommand(context)', 1)
elif 'startVoiceCommand(context)' not in main:
    raise SystemExit('Bouton vocal de l accueil introuvable')

if 'private fun startVoiceCommand(context: Context)' not in main:
    anchor = '''private fun startVoiceZeno(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
}
'''
    helper = '''private fun startVoiceZeno(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
}

private fun startVoiceCommand(context: Context) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        context.startActivity(Intent(context, SetupActivity::class.java))
        return
    }
    val intent = Intent(context, FloatingZenoService::class.java)
        .setAction("com.zeno.robot.action.DIRECT_COMMAND")
    ContextCompat.startForegroundService(context, intent)
}
'''
    if anchor not in main:
        raise SystemExit('Helper startVoiceZeno introuvable')
    main = main.replace(anchor, helper, 1)

# La version affichée doit correspondre au paquet généré.
main = main.replace('Zeno Android v1.3.9', 'Zeno Android v1.3.10')
main = main.replace('Version 1.3.9', 'Version 1.3.10')
MAIN.write_text(main, encoding='utf-8')

print('Accueil vocal unifié : le bouton micro et Salut Zeno partagent le même moteur')
