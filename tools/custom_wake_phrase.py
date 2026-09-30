from pathlib import Path

MAIN = Path("app/src/main/java/com/zeno/robot/MainActivity.kt")
SERVICE = Path("app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt")

main = MAIN.read_text(encoding="utf-8")
service = SERVICE.read_text(encoding="utf-8")

old_settings = '''@Composable
private fun SettingsScreen(accent: Color, customize: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingCard(Icons.Default.Palette, "Apparence", "Couleurs, thème et Zeno flottant", accent, customize) }
        item { SettingCard(Icons.Default.Mic, "Voix", "Réveil vocal : Salut Zeno", accent) {} }
        item { SettingCard(Icons.Default.Translate, "Langues", "Traduction locale via ML Kit", accent) {} }
        item { SettingCard(Icons.Default.Apps, "Applications", "Zeno peut ouvrir les applications installées", accent) {} }
        item { SettingCard(Icons.Default.Security, "Confidentialité", "Micro et superposition contrôlés par Android", accent) {} }
        item { Text("Zeno Android v1.3.1", color = Color(0xFF7894C0), modifier = Modifier.padding(top = 14.dp)) }
    }
}
'''

new_settings = '''@Composable
private fun SettingsScreen(accent: Color, customize: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_voice", Context.MODE_PRIVATE) }
    var wakePhrase by remember { mutableStateOf(prefs.getString("wake_phrase", "Salut Zeno") ?: "Salut Zeno") }
    var phraseDraft by remember { mutableStateOf(wakePhrase) }
    var editWakePhrase by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingCard(Icons.Default.Palette, "Apparence", "Couleurs, thème et Zeno flottant", accent, customize) }
        item {
            SettingCard(
                Icons.Default.Mic,
                "Phrase de réveil vocal",
                "Dis : « $wakePhrase »",
                accent
            ) {
                phraseDraft = wakePhrase
                editWakePhrase = true
            }
        }
        item { SettingCard(Icons.Default.Translate, "Langues", "Traduction locale via ML Kit", accent) {} }
        item { SettingCard(Icons.Default.Apps, "Applications", "Zeno peut ouvrir les applications installées", accent) {} }
        item { SettingCard(Icons.Default.Security, "Confidentialité", "Micro et superposition contrôlés par Android", accent) {} }
        item { Text("Zeno Android v1.3.1", color = Color(0xFF7894C0), modifier = Modifier.padding(top = 14.dp)) }
    }

    if (editWakePhrase) {
        AlertDialog(
            onDismissRequest = { editWakePhrase = false },
            icon = { Icon(Icons.Default.Mic, null, tint = accent) },
            title = { Text("Phrase de réveil") },
            text = {
                Column {
                    Text("Choisis la phrase qui réveillera Zeno quand le mode vocal flottant est actif.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phraseDraft,
                        onValueChange = { phraseDraft = it.take(40) },
                        singleLine = true,
                        label = { Text("Ex. Salut Zeno") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = phraseDraft.trim().replace(Regex("\\\\s+"), " ")
                    if (clean.length >= 2) {
                        wakePhrase = clean
                        prefs.edit().putString("wake_phrase", clean).apply()
                        editWakePhrase = false
                        context.stopService(Intent(context, FloatingZenoService::class.java))
                        if (Settings.canDrawOverlays(context)) startFloatingZeno(context)
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { editWakePhrase = false }) { Text("Annuler") }
            }
        )
    }
}
'''

if old_settings not in main:
    raise SystemExit("Bloc SettingsScreen introuvable")
main = main.replace(old_settings, new_settings, 1)

old_notification = '''            .setContentText("Dis « Salut Zeno » ou touche le robot pour parler.")'''
new_notification = '''            .setContentText("Dis « ${currentWakePhrase()} » ou touche le robot pour parler.")'''
if old_notification not in service:
    raise SystemExit("Texte de notification introuvable")
service = service.replace(old_notification, new_notification, 1)

old_contains = '''    private fun containsWakePhrase(candidates: List<String>): Boolean = candidates.any { raw ->
        val text = raw.lowercase(Locale.FRENCH)
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
        text.contains("salut zeno") || text.contains("bonjour zeno") || text.contains("hey zeno")
    }
'''
new_contains = '''    private fun currentWakePhrase(): String =
        getSharedPreferences("zeno_voice", Context.MODE_PRIVATE)
            .getString("wake_phrase", "Salut Zeno")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Salut Zeno"

    private fun normalizeWake(value: String): String {
        val normalized = java.text.Normalizer.normalize(value.lowercase(Locale.FRENCH), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\\\p{Mn}+"), "")
        return normalized
            .replace('’', ' ')
            .replace('\\'', ' ')
            .replace('-', ' ')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\\\s+"), " ")
            .trim()
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean {
        val wake = normalizeWake(currentWakePhrase())
        if (wake.isBlank()) return false
        return candidates.any { raw -> normalizeWake(raw).contains(wake) }
    }
'''
if old_contains not in service:
    raise SystemExit("Bloc containsWakePhrase introuvable")
service = service.replace(old_contains, new_contains, 1)

MAIN.write_text(main, encoding="utf-8")
SERVICE.write_text(service, encoding="utf-8")
print("Phrase de réveil personnalisable activée")
