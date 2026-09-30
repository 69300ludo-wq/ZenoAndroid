from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
VOICE = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
SETUP = Path('app/src/main/java/com/zeno/robot/SetupActivity.kt')
SOS = Path('app/src/main/java/com/zeno/robot/SosActivity.kt')
BRAIN = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
ICONS = Path('app/src/main/java/com/zeno/robot/data/IconManager.kt')

main = MAIN.read_text(encoding='utf-8')
voice = VOICE.read_text(encoding='utf-8')
service = SERVICE.read_text(encoding='utf-8')
setup = SETUP.read_text(encoding='utf-8')
sos = SOS.read_text(encoding='utf-8')
brain = BRAIN.read_text(encoding='utf-8')
icons = ICONS.read_text(encoding='utf-8')

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

# KTX : utiliser les extensions idiomatiques déjà présentes dans core-ktx.
def add_import(text: str, after: str, new_import: str) -> str:
    if new_import not in text:
        if after not in text:
            raise SystemExit(f'Import de référence introuvable pour {new_import}')
        text = text.replace(after, after + new_import + '\n', 1)
    return text

main = add_import(main, 'import androidx.core.content.ContextCompat\n', 'import androidx.core.content.edit')
main = add_import(main, 'import androidx.core.content.edit\n', 'import androidx.core.net.toUri')
main = main.replace(
    'prefs.edit().putString("wake_phrase", clean).apply()',
    'prefs.edit { putString("wake_phrase", clean) }',
)
main = main.replace(
    'Uri.parse("package:${context.packageName}")',
    '"package:${context.packageName}".toUri()',
)

setup = add_import(setup, 'import androidx.core.content.ContextCompat\n', 'import androidx.core.net.toUri')
setup = setup.replace('Uri.parse("package:$packageName")', '"package:$packageName".toUri()')

sos = add_import(sos, 'import androidx.activity.compose.setContent\n', 'import androidx.core.content.edit')
sos = add_import(sos, 'import androidx.core.content.edit\n', 'import androidx.core.net.toUri')
sos = sos.replace('Uri.parse("tel:$number")', '"tel:$number".toUri()')
sos = sos.replace('Uri.parse("smsto:${Uri.encode(number)}")', '"smsto:${Uri.encode(number)}".toUri()')
sos = sos.replace(
    'prefs.edit().putString("contact", clean).apply()',
    'prefs.edit { putString("contact", clean) }',
)

brain = add_import(brain, 'import androidx.core.content.ContextCompat\n', 'import androidx.core.net.toUri')
brain = brain.replace(
    'Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")',
    '"https://www.youtube.com/results?search_query=${Uri.encode(query)}".toUri()',
)
brain = brain.replace(
    'Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")',
    '"https://www.google.com/search?q=${Uri.encode(query)}".toUri()',
)

icons = add_import(icons, 'import android.content.Context\n', 'import androidx.core.content.edit')
icons = icons.replace(
    '''context.getSharedPreferences("zeno", Context.MODE_PRIVATE)\n            .edit()\n            .putString("theme", theme.name)\n            .apply()''',
    '''context.getSharedPreferences("zeno", Context.MODE_PRIVATE).edit {\n            putString("theme", theme.name)\n        }''',
)

# Accessibilité : une vue tactile personnalisée doit réellement surcharger performClick().
service = service.replace(
    'val view = ImageView(this).apply {',
    '''val view = object : ImageView(this) {\n            override fun performClick(): Boolean = super.performClick()\n        }.apply {''',
    1,
)
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
SETUP.write_text(setup, encoding='utf-8')
SOS.write_text(sos, encoding='utf-8')
BRAIN.write_text(brain, encoding='utf-8')
ICONS.write_text(icons, encoding='utf-8')
print('Qualité finale appliquée : lint, KTX, performances Compose et accessibilité')
