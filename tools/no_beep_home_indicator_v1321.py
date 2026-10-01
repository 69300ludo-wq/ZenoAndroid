from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

# --- Voyant Accueil : visible, gros, en haut par défaut et toujours déplaçable au doigt.
main = MAIN.read_text(encoding='utf-8')

# S'assurer que le voyant est bien rendu sur l'écran Accueil uniquement.
main = main.replace(
    '            if (screen == Screen.HOME) VoiceStatusDot(draggable = true)\n',
    '            if (screen == Screen.HOME) VoiceStatusDot(draggable = true)\n',
    1,
)

# Un ancien réglage de petite taille ne doit plus rendre le voyant quasi invisible.
main = main.replace(
    '    val visualSize = prefs.getInt("indicator_size", 20).coerceIn(16, 30)\n',
    '    val visualSize = prefs.getInt("indicator_size", 28).coerceIn(24, 38)\n',
    1,
)
main = main.replace('    val touchSize = 76f\n', '    val touchSize = 88f\n', 1)
main = main.replace(
    'mutableFloatStateOf(if (storedY.isFinite()) storedY.coerceIn(0f, maxY) else 8f)',
    'mutableFloatStateOf(if (storedY.isFinite()) storedY.coerceIn(0f, maxY) else 10f)',
    1,
)
main = main.replace(
    '.size((visualSize + if (voiceState == "command") 6 else 0).dp)',
    '.size((visualSize + if (voiceState == "command") 8 else 0).dp)',
    1,
)

# Si aucun emplacement n'a encore été choisi, le voyant est centré en haut.
# Les coordonnées déjà déplacées par l'utilisateur restent conservées.

main = main.replace('Zeno Android v1.3.20', 'Zeno Android v1.3.21')
main = main.replace('Version 1.3.20', 'Version 1.3.21')
MAIN.write_text(main, encoding='utf-8')

# --- Reconnaissance vocale : suppression plus agressive du bip fournisseur Android.
service = SERVICE.read_text(encoding='utf-8')

# Remplacer le suivi simple des flux muets par une sauvegarde exacte des volumes.
service = service.replace(
    '    private val beepMutedStreams = linkedSetOf<Int>()\n',
    '    private val beepOriginalVolumes = mutableMapOf<Int, Int>()\n',
    1,
)

helper_pattern = re.compile(
    r'''    private fun quietRecognizerBeep\(durationMs: Long = \d+L\) \{.*?\n    \}\n\n    private fun restoreRecognizerAudio\(\) \{.*?\n    \}\n\n''',
    re.S,
)
helper = '''    private fun quietRecognizerBeep(durationMs: Long = 1800L) {
        val audio = runCatching { getSystemService(AUDIO_SERVICE) as AudioManager }.getOrNull() ?: return
        beepRestore?.let(handler::removeCallbacks)
        val streams = intArrayOf(
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_MUSIC
        )
        streams.forEach { stream ->
            runCatching {
                if (!beepOriginalVolumes.containsKey(stream)) {
                    beepOriginalVolumes[stream] = audio.getStreamVolume(stream)
                }
                // Certains moteurs ignorent ADJUST_MUTE. Mettre temporairement le volume
                // à zéro coupe aussi leur bip de début/fin, puis le volume exact est restauré.
                audio.setStreamVolume(stream, 0, 0)
                audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
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
            beepOriginalVolumes.toMap().forEach { (stream, volume) ->
                runCatching {
                    audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                    audio.setStreamVolume(stream, volume, 0)
                }
            }
        }
        beepOriginalVolumes.clear()
    }

'''
service, count = helper_pattern.subn(helper, service, count=1)
if count != 1:
    raise SystemExit('Fonctions de masquage du bip introuvables')

# Couper le son avant toute destruction/recréation du SpeechRecognizer :
# certains fournisseurs émettent leur bip au cancel/destroy et non au startListening.
invalidate_anchor = '''    private fun invalidateRecognizer() {
        sessionSerial += 1
'''
if invalidate_anchor not in service:
    raise SystemExit('invalidateRecognizer introuvable')
service = service.replace(
    invalidate_anchor,
    '''    private fun invalidateRecognizer() {
        quietRecognizerBeep(1700L)
        sessionSerial += 1
''',
    1,
)

# Étendre les fenêtres déjà ajoutées autour des débuts/fins de reconnaissance.
service = service.replace('quietRecognizerBeep(1250L)', 'quietRecognizerBeep(1800L)')
service = service.replace('quietRecognizerBeep(1000L)', 'quietRecognizerBeep(1500L)')
service = service.replace('quietRecognizerBeep(1100L)', 'quietRecognizerBeep(1600L)')
service = service.replace('quietRecognizerBeep()\n            runCatching { current.startListening', 'quietRecognizerBeep(1800L)\n            runCatching { current.startListening')

SERVICE.write_text(service, encoding='utf-8')

print('Zeno 1.3.21 : bip reconnaissance masqué plus fortement + gros voyant visible sur Accueil')
