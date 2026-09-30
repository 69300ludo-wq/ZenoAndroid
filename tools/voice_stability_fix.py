from pathlib import Path

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
service = SERVICE.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
main = MAIN.read_text(encoding='utf-8')

# Les scripts précédents créent déjà le service. Ici on applique des réglages tolérants
# sans faire échouer la compilation si une version précédente a légèrement changé le code.
service = service.replace('handler.postDelayed({ startWakeListening() }, 1000)', 'handler.postDelayed({ startWakeListening() }, 120)')
service = service.replace('handler.postDelayed({ startWakeListening() }, 750)', 'handler.postDelayed({ startWakeListening() }, 180)')
service = service.replace('handler.postDelayed({ startWakeListening() }, 450)', 'handler.postDelayed({ startWakeListening() }, 100)')
service = service.replace('handler.postDelayed({ beginCommandListening() }, 450)', 'handler.postDelayed({ beginCommandListening() }, 350)')
service = service.replace('handler.postDelayed({ beginCommandListening() }, 650)', 'handler.postDelayed({ beginCommandListening() }, 500)')
service = service.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)')
service = service.replace('RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L', 'RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L')
service = service.replace('RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L', 'RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L')
service = service.replace('RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L', 'RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1600L')

# Ne jamais ouvrir le menu au simple mot de réveil si le bloc existe encore.
service = service.replace('''        openZeno()\n        speak("Oui, je t'écoute") {\n            wakeTriggered = false\n            handler.postDelayed({ beginCommandListening() }, 250)\n        }''', '''        wakeTriggered = false\n        handler.postDelayed({ beginCommandListening() }, 350)''')

# Utilise le recognizer Android standard pour une meilleure compatibilité.
voice = voice.replace('''    private fun createRecognizer(): SpeechRecognizer {\n        return if (\n            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&\n            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)\n        ) {\n            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)\n        } else {\n            SpeechRecognizer.createSpeechRecognizer(this)\n        }\n    }''', '''    private fun createRecognizer(): SpeechRecognizer =\n        SpeechRecognizer.createSpeechRecognizer(this)''')
voice = voice.replace('.size((270f + (voiceLevel * 52f)).dp)', '.size(300.dp)')

SERVICE.write_text(service, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
MAIN.write_text(main, encoding='utf-8')
print('Reconnaissance vocale améliorée appliquée sans conflit')
