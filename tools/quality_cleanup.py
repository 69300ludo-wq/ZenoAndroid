from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

main = MAIN.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
service = SERVICE.read_text(encoding='utf-8')

# SharedPreferences : écriture asynchrone non bloquante (le fichier local reste la source fiable).
main = main.replace(
    'prefs.edit().putString("wake_phrase", clean).commit()',
    'prefs.edit().putString("wake_phrase", clean).apply()',
)

# Compose : éviter l'autoboxing d'un Float fréquemment mis à jour par le niveau micro.
if 'private var voiceLevel by mutableStateOf(0f)' in voice:
    if 'import androidx.compose.runtime.mutableFloatStateOf' not in voice:
        voice = voice.replace(
            'import androidx.compose.runtime.mutableStateOf\n',
            'import androidx.compose.runtime.mutableFloatStateOf\nimport androidx.compose.runtime.mutableStateOf\n',
            1,
        )
    voice = voice.replace(
        'private var voiceLevel by mutableStateOf(0f)',
        'private var voiceLevel by mutableFloatStateOf(0f)',
        1,
    )

# Accessibilité : les actions tactiles passent aussi par performClick/performLongClick.
anchor = '''        val params = WindowManager.LayoutParams(\n'''
click_setup = '''        view.setOnClickListener { openReliableVoiceCommand() }\n        view.setOnLongClickListener {\n            openZeno()\n            true\n        }\n\n'''
if click_setup not in service:
    if anchor not in service:
        raise SystemExit('Paramètres de la bulle Zeno introuvables')
    service = service.replace(anchor, click_setup + anchor, 1)

old_action = '''                    if (moved < 18f * resources.displayMetrics.density) {\n                        if (duration >= 650) openZeno() else openReliableVoiceCommand()\n                    }\n                    true\n'''
new_action = '''                    if (moved < 18f * resources.displayMetrics.density) {\n                        if (duration >= 650) view.performLongClick() else view.performClick()\n                    }\n                    true\n'''
if old_action in service:
    service = service.replace(old_action, new_action, 1)
elif 'view.performClick()' not in service:
    raise SystemExit('Gestion du clic flottant introuvable')

MAIN.write_text(main, encoding='utf-8')
VOICE.write_text(voice, encoding='utf-8')
SERVICE.write_text(service, encoding='utf-8')
print('Qualité finale appliquée : lint, performances Compose et accessibilité')
