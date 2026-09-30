from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
main = MAIN.read_text(encoding='utf-8')

# Une seule source persistante : un fichier privé commun aux processus de l'app.
# On accepte aussi bien l'état initial que l'état déjà transformé par les étapes
# précédentes du pipeline, afin que ce correctif ne casse plus la compilation.
new_state = '''    val wakePhraseFile = remember { java.io.File(context.filesDir, "wake_phrase.txt") }
    var wakePhrase by remember {
        mutableStateOf(
            runCatching { wakePhraseFile.takeIf { it.exists() }?.readText()?.trim() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "Salut Zeno"
        )
    }
'''

state_patterns = [
    re.compile(
        r'''    val prefs = remember \{ context\.getSharedPreferences\("zeno_voice", Context\.MODE_PRIVATE\) \}\n'''
        r'''    val savedWakePhrase = remember \{\n.*?'''
        r'''    \}\n'''
        r'''    var wakePhrase by remember \{ mutableStateOf\(savedWakePhrase\) \}\n''',
        re.S,
    ),
    re.compile(
        r'''    val prefs = remember \{ context\.getSharedPreferences\("zeno_voice", Context\.MODE_PRIVATE\) \}\n'''
        r'''    var wakePhrase by remember \{ mutableStateOf\(prefs\.getString\("wake_phrase", "Salut Zeno"\) \?: "Salut Zeno"\) \}\n'''
    ),
]

state_replaced = False
for pattern in state_patterns:
    main, count = pattern.subn(new_state, main, count=1)
    if count:
        state_replaced = True
        break

if not state_replaced and 'val wakePhraseFile = remember { java.io.File(context.filesDir, "wake_phrase.txt") }' not in main:
    raise SystemExit('Etat de la phrase vocale introuvable')

# Le bouton Enregistrer ne redémarre jamais le service vocal et ne touche pas au
# SpeechRecognizer. L'écriture du fichier est protégée et ne peut pas faire fermer Zeno.
new_save = '''                        editWakePhrase = false
                        wakePhrase = clean
                        val saved = runCatching {
                            wakePhraseFile.writeText(clean)
                            true
                        }.getOrDefault(false)
                        Toast.makeText(
                            context,
                            if (saved) "Phrase enregistrée" else "Impossible d'enregistrer la phrase",
                            if (saved) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                        ).show()
'''

save_patterns = [
    re.compile(
        r'''                        val saved = runCatching \{\n.*?'''
        r'''                        \}\.getOrDefault\(false\)\n'''
        r'''                        if \(saved\) \{\n.*?'''
        r'''                        \} else \{\n.*?'''
        r'''                        \}\n''',
        re.S,
    ),
    re.compile(
        r'''                        wakePhrase = clean\n'''
        r'''                        prefs\.edit \{ putString\("wake_phrase", clean\) \}\n'''
        r'''                        runCatching \{\n'''
        r'''                            java\.io\.File\(context\.filesDir, "wake_phrase\.txt"\)\.writeText\(clean\)\n'''
        r'''                        \}\n'''
        r'''                        editWakePhrase = false\n'''
        r'''                        Toast\.makeText\(context, "Phrase enregistrée", Toast\.LENGTH_SHORT\)\.show\(\)\n'''
    ),
]

save_replaced = False
for pattern in save_patterns:
    main, count = pattern.subn(new_save, main, count=1)
    if count:
        save_replaced = True
        break

if not save_replaced and 'wakePhraseFile.writeText(clean)' not in main:
    raise SystemExit('Bloc final Enregistrer introuvable')

MAIN.write_text(main, encoding='utf-8')
print('Enregistrement de la phrase durci : fichier privé uniquement, aucune relance micro')
