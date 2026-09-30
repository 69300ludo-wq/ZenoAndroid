from pathlib import Path

main = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
s = main.read_text(encoding='utf-8')

s = s.replace(
    '    VOICE_PHRASE("Phrase vocale"), SETTINGS("Paramètres"), ABOUT("À propos")',
    '    VOICE_PHRASE("Phrase vocale"), SOS("Mode SOS"), SETTINGS("Paramètres"), ABOUT("À propos")',
    1
)
s = s.replace(
    '                            Screen.VOICE_PHRASE -> WakePhraseScreen(accent)\n                            Screen.SETTINGS ->',
    '                            Screen.VOICE_PHRASE -> WakePhraseScreen(accent)\n                            Screen.SOS -> SosScreen(Color(0xFFFF4D5A))\n                            Screen.SETTINGS ->',
    1
)
s = s.replace(
    '        HomeAction(Icons.Default.Mic, "Phrase vocale", "Choisis la phrase pour réveiller Zeno", Screen.VOICE_PHRASE, Color(0xFFFF4FC4)),\n        HomeAction(Icons.Default.Settings,',
    '        HomeAction(Icons.Default.Mic, "Phrase vocale", "Choisis la phrase pour réveiller Zeno", Screen.VOICE_PHRASE, Color(0xFFFF4FC4)),\n        HomeAction(Icons.Default.Warning, "Mode SOS", "Secours et contact d’urgence", Screen.SOS, Color(0xFFFF4D5A)),\n        HomeAction(Icons.Default.Settings,',
    1
)

if 'SOS("Mode SOS")' not in s or 'Screen.SOS -> SosScreen' not in s or '"Mode SOS", "Secours et contact d’urgence"' not in s:
    raise SystemExit('Impossible d’ajouter le module SOS à l’accueil')
main.write_text(s, encoding='utf-8')

brain = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
b = brain.read_text(encoding='utf-8')
if 'import com.zeno.robot.SosActivity' not in b:
    b = b.replace('import com.zeno.robot.SetupActivity\n', 'import com.zeno.robot.SetupActivity\nimport com.zeno.robot.SosActivity\n', 1)

needle = '''        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return when {'''
replacement = '''        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (text == "sos" || text.contains("mode sos") || text.contains("ouvre sos") ||
            text.contains("urgence") || text.contains("j ai besoin d aide") || text.contains("appelle les secours")) {
            context.startActivity(Intent(context, SosActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return Result.Action("J’ouvre le mode SOS. Tu peux appeler le 112 ou ton contact d’urgence.")
        }

        return when {'''
if 'J’ouvre le mode SOS' not in b:
    if needle not in b:
        raise SystemExit('Emplacement des commandes vocales SOS introuvable')
    b = b.replace(needle, replacement, 1)
brain.write_text(b, encoding='utf-8')

print('Mode SOS ajouté à l’accueil et aux commandes vocales Zeno')
