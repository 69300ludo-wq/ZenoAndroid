from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# --- Accueil : un seul voyant, plus gros, visible uniquement sur HOME.
main = MAIN.read_text(encoding='utf-8')
main = main.replace(
    '            VoiceStatusDot(draggable = screen == Screen.HOME)\n',
    '            if (screen == Screen.HOME) VoiceStatusDot(draggable = true)\n',
    1,
)

# Le voyant reste libre au doigt, mais démarre en haut-centre et est beaucoup plus visible.
main = main.replace(
    '    val visualSize = prefs.getInt("indicator_size", 10).coerceIn(7, 18)\n',
    '    val visualSize = prefs.getInt("indicator_size", 20).coerceIn(16, 30)\n',
    1,
)
main = main.replace('    val touchSize = 58f\n', '    val touchSize = 76f\n', 1)
main = main.replace(
    'mutableFloatStateOf(if (storedY.isFinite()) storedY.coerceIn(0f, maxY) else 4f)',
    'mutableFloatStateOf(if (storedY.isFinite()) storedY.coerceIn(0f, maxY) else 8f)',
    1,
)
main = main.replace(
    '.size((visualSize + if (voiceState == "command") 3 else 0).dp)',
    '.size((visualSize + if (voiceState == "command") 6 else 0).dp)',
    1,
)

# Le voyant de superposition / tiroir n'est plus utilisé : l'accueil est la seule zone visuelle.
main = main.replace(
    '"Active le micro et le petit voyant au-dessus des autres applis."',
    '"Active le micro pour la reconnaissance vocale Zeno."',
)

# Masquer la ligne de permission de superposition dans la carte d'accueil si elle existe.
overlay_row = re.compile(
    r'''\n            Row\(Modifier\.fillMaxWidth\(\), verticalAlignment = Alignment\.CenterVertically\) \{\n                Icon\(Icons\.Default\.Layers,.*?\n            \}\n(?=\n            (?:Button|Row|TextButton)\()''',
    re.S,
)
main, _ = overlay_row.subn('', main, count=1)

main = main.replace('Zeno Android v1.3.19', 'Zeno Android v1.3.20')
main = main.replace('Version 1.3.19', 'Version 1.3.20')
MAIN.write_text(main, encoding='utf-8')

# --- Service : supprimer réellement le second voyant superposé.
service = SERVICE.read_text(encoding='utf-8')
# Tous les appels autonomes de création d'overlay sont remplacés par un nettoyage.
service = service.replace('        showOverlayDot()\n', '        removeOverlayDot()\n')
service = service.replace(
    '        if (overlayDot == null && Settings.canDrawOverlays(this)) showOverlayDot()\n',
    '        if (overlayDot != null) removeOverlayDot()\n',
)
service = service.replace(
    '        updateOverlayDot(state)\n',
    '        if (overlayDot != null) removeOverlayDot()\n',
)

# --- Bip : masquer aussi le bip de la phase d'attente, pas seulement celui de la commande.
service = service.replace(
    'private fun quietRecognizerBeep(durationMs: Long = 760L)',
    'private fun quietRecognizerBeep(durationMs: Long = 1250L)',
    1,
)

wake_start = '        runCatching { current.startListening(recognitionIntent(partial = true, longWindow = false)) }\n'
if wake_start in service and '        quietRecognizerBeep(1250L)\n' + wake_start not in service:
    service = service.replace(
        wake_start,
        '        quietRecognizerBeep(1250L)\n' + wake_start,
        1,
    )

# Le bip de fin peut arriver juste avant les résultats selon le fournisseur vocal.
service = service.replace(
    'override fun onEndOfSpeech() { quietRecognizerBeep(620L) }',
    'override fun onEndOfSpeech() { quietRecognizerBeep(1000L) }',
)
service = service.replace(
    'quietRecognizerBeep(720L)\n                    runCatching { current.stopListening() }',
    'quietRecognizerBeep(1100L)\n                    runCatching { current.stopListening() }',
)

SERVICE.write_text(service, encoding='utf-8')

# Plus aucune superposition Android nécessaire pour le voyant.
manifest = MANIFEST.read_text(encoding='utf-8')
manifest = manifest.replace('    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n', '')
MANIFEST.write_text(manifest, encoding='utf-8')

print('Zeno 1.3.20 : voyant HOME plus gros, second voyant supprimé et bip vocal masqué aussi au réveil')
