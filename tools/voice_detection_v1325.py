from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')

service = SERVICE.read_text(encoding='utf-8')

# Davantage de variantes fréquentes de transcription du mot "Zeno".
service, count = re.subn(
    r'private val wakePhrases = setOf\(.*?\n        \)',
    'private val wakePhrases = setOf(\n'
    '            "zeno", "seno", "xeno", "zino", "zena", "jeno", "geno", "sino",\n'
    '            "ceno", "zeino", "zenau", "zenot", "saino", "zenno"\n'
    '        )',
    service,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit('wakePhrases introuvable')

# La détection partielle doit accepter toutes les variantes et déclencher plus vite.
service = service.replace(
    'val exact = candidates.any { normalize(it) in wakePhrases }',
    'val exact = containsWakePhrase(candidates)',
)
service = service.replace('handler.postDelayed(transition, 850L)', 'handler.postDelayed(transition, 260L)')

# Enlever la préférence offline forcée : si aucun moteur local performant n\'est
# disponible, Android peut utiliser son moteur réseau plus précis. Le moteur local
# reste utilisé automatiquement quand createOnDeviceSpeechRecognizer est disponible.
service = service.replace('            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)\n', '')

# Plus de propositions et une fenêtre de commande plus tolérante aux pauses.
service = service.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 30)')
service = service.replace(
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6500L)',
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000L)'
)
service = service.replace(
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)',
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5600L)'
)
service = service.replace(
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 650L)',
    'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 450L)'
)
service = service.replace('handler.postDelayed(timeout, 16_000L)', 'handler.postDelayed(timeout, 22_000L)')

# Après un timeout de réveil, reprendre plus vite sans revenir aux redémarrages
# agressifs qui faisaient biper le téléphone en boucle.
service = service.replace(
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 5_000L',
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 1_500L'
)
service = service.replace(
    'else -> handler.postDelayed({ startWakeListening() }, 2_500)',
    'else -> handler.postDelayed({ startWakeListening() }, 1_500)'
)

# Un résultat partiel qui contient déjà "Zeno" doit déclencher la phase commande
# immédiatement, même si Android ajoute "hey", "ok" ou d'autres mots autour.
partial_pattern = re.compile(
    r'''                override fun onPartialResults\(partialResults: Bundle\?\) \{\n                    if \(token != sessionSerial \|\| mode != Mode\.WAKE\) return\n                    val candidates = partialResults\?\.getStringArrayList\(SpeechRecognizer\.RESULTS_RECOGNITION\)\.orEmpty\(\)\n                    val exact = .*?\n                    val withPayload = extractWakePayload\(candidates\)\n                    if \(withPayload != null && withPayload\.isNotBlank\(\)\) \{\n                        wakeTransition\?\.let\(handler::removeCallbacks\)\n                        wakeTransition = null\n                        return\n                    \}\n                    if \(exact && wakeTransition == null\) \{\n                        val transition = Runnable \{\n                            if \(token == sessionSerial && mode == Mode\.WAKE\) beginListening\(Mode\.FOLLOW_UP, resetRetry = true\)\n                        \}\n                        wakeTransition = transition\n                        handler\.postDelayed\(transition, \d+L\)\n                    \}\n                \}''',
    re.S,
)
partial_replacement = '''                override fun onPartialResults(partialResults: Bundle?) {
                    if (token != sessionSerial || mode != Mode.WAKE) return
                    val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val withPayload = extractWakePayload(candidates)
                    if (withPayload != null && withPayload.isNotBlank()) {
                        // Laisser le moteur finir la phrase pour éviter d'exécuter une commande tronquée.
                        wakeTransition?.let(handler::removeCallbacks)
                        wakeTransition = null
                        return
                    }
                    if (containsWakePhrase(candidates) && wakeTransition == null) {
                        val transition = Runnable {
                            if (token == sessionSerial && mode == Mode.WAKE) {
                                beginListening(Mode.FOLLOW_UP, resetRetry = true)
                            }
                        }
                        wakeTransition = transition
                        handler.postDelayed(transition, 260L)
                    }
                }'''
service, partial_count = partial_pattern.subn(partial_replacement, service, count=1)
if partial_count == 0 and 'handler.postDelayed(transition, 260L)' not in service:
    raise SystemExit('onPartialResults WAKE introuvable')

SERVICE.write_text(service, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')
main = main.replace('Zeno Android v1.3.24', 'Zeno Android v1.3.25')
main = main.replace('Version 1.3.24', 'Version 1.3.25')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.25 : detection Zeno plus rapide, plus tolerante et commandes plus longues')
