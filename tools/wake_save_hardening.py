from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
main = MAIN.read_text(encoding='utf-8')

# Une seule source persistante, un fichier privé à l'application. Cela évite les
# problèmes de cache SharedPreferences entre le processus principal et :voice.
old_state = '''    val prefs = remember { context.getSharedPreferences("zeno_voice", Context.MODE_PRIVATE) }
    var wakePhrase by remember { mutableStateOf(prefs.getString("wake_phrase", "Salut Zeno") ?: "Salut Zeno") }
'''
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
if old_state not in main:
    raise SystemExit('Etat de la phrase vocale introuvable')
main = main.replace(old_state, new_state, 1)

# Le clic Enregistrer ne touche jamais au service micro, ne relance aucune activité
# et ne dépend plus de SharedPreferences. Toutes les E/S sont protégées.
old_save = '''                        wakePhrase = clean
                        prefs.edit { putString("wake_phrase", clean) }
                        runCatching {
                            java.io.File(context.filesDir, "wake_phrase.txt").writeText(clean)
                        }
                        editWakePhrase = false
                        Toast.makeText(context, "Phrase enregistrée", Toast.LENGTH_SHORT).show()
'''
new_save = '''                        // Ferme d'abord le dialogue puis met à jour l'interface.
                        // Même si le stockage échoue, aucune exception ne doit fermer Zeno.
                        editWakePhrase = false
                        wakePhrase = clean
                        runCatching { wakePhraseFile.writeText(clean) }
'''
if old_save not in main:
    raise SystemExit('Bloc final Enregistrer introuvable')
main = main.replace(old_save, new_save, 1)

# Les imports KTX/Toast peuvent avoir été ajoutés par les étapes précédentes.
# On les laisse si utilisés ailleurs ; Kotlin accepte les imports non utilisés.
MAIN.write_text(main, encoding='utf-8')
print('Enregistrement de la phrase durci : aucun redémarrage micro, aucune opération non protégée')
