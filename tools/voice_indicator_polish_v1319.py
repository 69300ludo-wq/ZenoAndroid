from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# Permission normale nécessaire pour atténuer le bip du moteur vocal Android
# pendant la transition vers l'écoute de la commande.
manifest = MANIFEST.read_text(encoding='utf-8')
perm = '    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />\n'
if 'android.permission.MODIFY_AUDIO_SETTINGS' not in manifest:
    anchor = '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'
    if anchor not in manifest:
        raise SystemExit('Permission RECORD_AUDIO introuvable')
    manifest = manifest.replace(anchor, anchor + perm, 1)
MANIFEST.write_text(manifest, encoding='utf-8')

service = SERVICE.read_text(encoding='utf-8')

# AudioManager : le SpeechRecognizer Android ne fournit pas d'option officielle
# pour désactiver son bip. On coupe très brièvement les flux concernés uniquement
# lors du passage en mode commande, puis on les restaure automatiquement.
if 'import android.media.AudioManager\n' not in service:
    anchor = 'import android.graphics.drawable.GradientDrawable\n'
    if anchor not in service:
        anchor = 'import android.content.pm.PackageManager\n'
    if anchor not in service:
        raise SystemExit('Point insertion AudioManager introuvable')
    service = service.replace(anchor, anchor + 'import android.media.AudioManager\n', 1)

field_anchor = '    private var commandTimeout: Runnable? = null\n'
if 'private var beepRestore:' not in service:
    if field_anchor not in service:
        raise SystemExit('Champ commandTimeout introuvable')
    service = service.replace(
        field_anchor,
        field_anchor +
        '    private var beepRestore: Runnable? = null\n'
        '    private val beepMutedStreams = linkedSetOf<Int>()\n'
        '    private var lastCommandPartial: List<String> = emptyList()\n',
        1,
    )

helper_anchor = '    private fun recognitionIntent(partial: Boolean, longWindow: Boolean): Intent =\n'
if 'private fun quietRecognizerBeep(' not in service:
    if helper_anchor not in service:
        raise SystemExit('recognitionIntent introuvable')
    helpers = '''    private fun quietRecognizerBeep(durationMs: Long = 760L) {
        val audio = runCatching { getSystemService(AUDIO_SERVICE) as AudioManager }.getOrNull() ?: return
        beepRestore?.let(handler::removeCallbacks)
        val streams = intArrayOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_SYSTEM)
        streams.forEach { stream ->
            runCatching {
                if (!audio.isStreamMute(stream)) {
                    audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                    beepMutedStreams += stream
                }
            }
        }
        val restore = Runnable { restoreRecognizerAudio() }
        beepRestore = restore
        handler.postDelayed(restore, durationMs)
    }

    private fun restoreRecognizerAudio() {
        beepRestore?.let(handler::removeCallbacks)
        beepRestore = null
        val audio = runCatching { getSystemService(AUDIO_SERVICE) as AudioManager }.getOrNull()
        if (audio != null) {
            beepMutedStreams.toList().forEach { stream ->
                runCatching { audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) }
            }
        }
        beepMutedStreams.clear()
    }

'''
    service = service.replace(helper_anchor, helpers + helper_anchor, 1)

# Fenêtre de commande plus longue + davantage de propositions du moteur vocal.
service = service.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 15)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 20)')
service = service.replace('putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5200L)', 'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6500L)')
service = service.replace('putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600L)', 'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)')
service = service.replace('putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 900L)', 'putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 650L)')
service = service.replace('handler.postDelayed(timeout, 12_000L)', 'handler.postDelayed(timeout, 16_000L)')

# Renforcer uniquement la phase COMMAND/FOLLOW_UP.
begin_start = service.find('    private fun beginListening(targetMode: Mode, resetRetry: Boolean) {')
begin_end = service.find('    private fun executeCandidates(candidates: List<String>) {', begin_start)
if begin_start < 0 or begin_end < 0:
    raise SystemExit('Bloc beginListening introuvable')
begin = service[begin_start:begin_end]

begin = begin.replace(
    '        if (resetRetry) retries = 0\n        mode = targetMode\n',
    '        if (resetRetry) {\n            retries = 0\n            lastCommandPartial = emptyList()\n        }\n        mode = targetMode\n',
    1,
)

# Au moment où Zeno devient violet, on masque le bip de démarrage de la commande.
begin = begin.replace(
    '            runCatching { current.startListening(recognitionIntent(partial = false, longWindow = true)) }',
    '            quietRecognizerBeep()\n            runCatching { current.startListening(recognitionIntent(partial = true, longWindow = true)) }',
    1,
)

# Masquer également le bip de fin éventuel du fournisseur vocal.
begin = begin.replace(
    '                override fun onEndOfSpeech() = Unit',
    '                override fun onEndOfSpeech() { quietRecognizerBeep(620L) }',
    1,
)
begin = begin.replace(
    '                    runCatching { current.stopListening() }',
    '                    quietRecognizerBeep(720L)\n                    runCatching { current.stopListening() }',
    1,
)

# Si Android a déjà fourni un résultat partiel correct avant un NO_MATCH/TIMEOUT,
# l'utiliser au lieu de jeter la phrase prononcée.
permission_block = '''                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        broadcastState("permission")
                        return
                    }
'''
partial_error = permission_block + '''                    if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && lastCommandPartial.isNotEmpty()) {
                        executeCandidates(lastCommandPartial)
                        return
                    }
'''
if permission_block not in begin:
    raise SystemExit('Bloc permission erreur commande introuvable')
begin = begin.replace(permission_block, partial_error, 1)
begin = begin.replace('if (retries < 1 && error != SpeechRecognizer.ERROR_CLIENT)', 'if (retries < 2 && error != SpeechRecognizer.ERROR_CLIENT)', 1)

results_old = '''                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        .map { it.trim() }.filter { it.isNotBlank() }
                    if (candidates.isEmpty()) {
'''
results_new = '''                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        .map { it.trim() }.filter { it.isNotBlank() }
                    val effectiveCandidates = if (candidates.isNotEmpty()) candidates else lastCommandPartial
                    if (effectiveCandidates.isEmpty()) {
'''
if results_old not in begin:
    raise SystemExit('Bloc résultats commande introuvable')
begin = begin.replace(results_old, results_new, 1)
begin = begin.replace('if (retries < 1) {', 'if (retries < 2) {', 1)
begin = begin.replace('                    executeCandidates(candidates)\n', '                    executeCandidates(effectiveCandidates)\n', 1)

partial_old = '                override fun onPartialResults(partialResults: Bundle?) = Unit\n'
partial_new = '''                override fun onPartialResults(partialResults: Bundle?) {
                    if (token != sessionSerial || mode != targetMode) return
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (partial.isNotEmpty()) lastCommandPartial = partial
                }
'''
if partial_old not in begin:
    raise SystemExit('onPartialResults commande introuvable')
begin = begin.replace(partial_old, partial_new, 1)
service = service[:begin_start] + begin + service[begin_end:]

# Détection « Zeno » plus tolérante : le mot peut être précédé de « hey/ok »
# et peut être suivi directement de la commande.
phrase_pattern = re.compile(
    r'''    private fun stripWakePrefix\(raw: String\): String \{.*?\n    \}\n\n    private fun containsWakePhrase\(candidates: List<String>\): Boolean = .*?\n\n    private fun extractWakePayload\(candidates: List<String>\): String\? \{.*?\n    \}\n\n''',
    re.S,
)
phrase_replacement = '''    private fun stripWakePrefix(raw: String): String {
        val words = normalize(raw).split(' ').filter { it.isNotBlank() }
        val index = words.indexOfFirst { it in wakePhrases }
        return if (index >= 0) words.drop(index + 1).joinToString(" ").trim() else raw
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean = candidates.any { candidate ->
        normalize(candidate).split(' ').any { it in wakePhrases }
    }

    private fun extractWakePayload(candidates: List<String>): String? {
        for (candidate in candidates) {
            val words = normalize(candidate).split(' ').filter { it.isNotBlank() }
            val index = words.indexOfFirst { it in wakePhrases }
            if (index >= 0) return words.drop(index + 1).joinToString(" ").trim()
        }
        return null
    }

'''
service, count = phrase_pattern.subn(phrase_replacement, service, count=1)
if count != 1:
    raise SystemExit('Fonctions de détection Zeno introuvables')

# Plus de variantes fréquentes sans rendre le déclenchement trop large.
service = re.sub(
    r'private val wakePhrases = setOf\(.*?\n        \)',
    'private val wakePhrases = setOf(\n            "zeno", "seno", "xeno", "zino", "zena", "jeno", "geno", "sino"\n        )',
    service,
    count=1,
    flags=re.S,
)

# Toujours restaurer le son si le service se ferme pendant une transition.
destroy_anchor = '''    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
'''
if destroy_anchor not in service:
    raise SystemExit('onDestroy service introuvable')
service = service.replace(
    destroy_anchor,
    '''    override fun onDestroy() {
        restoreRecognizerAudio()
        handler.removeCallbacksAndMessages(null)
''',
    1,
)
SERVICE.write_text(service, encoding='utf-8')

# Accueil : zone tactile légèrement plus grande pour saisir le voyant et le tirer
# directement à l'endroit voulu. Aucun menu de position n'est réintroduit.
main = MAIN.read_text(encoding='utf-8')
main = main.replace('    val touchSize = 48f\n', '    val touchSize = 58f\n', 1)
main = main.replace('// Bleu = Zeno attend / entend « Zeno ouvre ».', '// Bleu = Zeno attend le mot « Zeno ».')
main = main.replace('Zeno Android v1.3.18', 'Zeno Android v1.3.19')
main = main.replace('Version 1.3.18', 'Version 1.3.19')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.19 : bip atténué, Zeno mieux détecté, commandes partielles récupérées et voyant accueil plus facile à glisser')
