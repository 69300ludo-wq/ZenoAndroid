from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

main = MAIN.read_text(encoding='utf-8')
service = SERVICE.read_text(encoding='utf-8')

# La phrase de réveil est désormais fixe. On supprime complètement toute écriture,
# tout champ de personnalisation et tout redémarrage du service lié à cette phrase.
FIXED_PHRASE = 'Salut Zeno'

wake_start = main.find('@Composable\nprivate fun WakePhraseScreen(accent: Color) {')
wake_end = main.find('@Composable\nprivate fun ActionGrid(', wake_start)
if wake_start < 0 or wake_end < 0:
    raise SystemExit('Ecran Phrase vocale introuvable')

fixed_wake_screen = '''@Composable
private fun WakePhraseScreen(accent: Color) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(Icons.Default.Mic, null, tint = accent, modifier = Modifier.size(70.dp))
            Spacer(Modifier.height(8.dp))
            Text("Phrase vocale", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "La phrase de réveil de Zeno est fixe pour éviter les plantages liés à l’enregistrement.",
                color = Color(0xFF9EB1D7),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accent.copy(alpha = .10f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .45f))
            ) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Phrase fixe", color = Color(0xFF91A5CF), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("« Salut Zeno »", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Elle n’est pas modifiable.", color = Color(0xFF9EB1D7), fontSize = 12.sp)
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        startFloatingZeno(context)
                        status = "Écoute activée. Dis : « Salut Zeno »"
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        )
                        status = "Autorise Zeno à s’afficher par-dessus les autres applications, puis reviens activer l’écoute."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Hearing, null)
                Spacer(Modifier.width(8.dp))
                Text("Activer Zeno flottant + micro")
            }
        }
        if (status.isNotBlank()) {
            item { Text(status, color = accent, textAlign = TextAlign.Center) }
        }
    }
}

'''
main = main[:wake_start] + fixed_wake_screen + main[wake_end:]

settings_start = main.find('@Composable\nprivate fun SettingsScreen(accent: Color, customize: () -> Unit) {')
settings_end = main.find('@Composable\nprivate fun SettingCard(', settings_start)
if settings_start < 0 or settings_end < 0:
    raise SystemExit('Ecran Paramètres introuvable')

fixed_settings = '''@Composable
private fun SettingsScreen(accent: Color, customize: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingCard(Icons.Default.Palette, "Apparence", "Couleurs, thème et Zeno flottant", accent, customize) }
        item { SettingCard(Icons.Default.Mic, "Voix", "Phrase fixe : Salut Zeno", accent) {} }
        item { SettingCard(Icons.Default.Translate, "Langues", "Traduction locale via ML Kit", accent) {} }
        item { SettingCard(Icons.Default.Apps, "Applications", "Zeno peut ouvrir les applications installées", accent) {} }
        item { SettingCard(Icons.Default.Security, "Confidentialité", "Micro et superposition contrôlés par Android", accent) {} }
        item { Text("Zeno Android v1.3.6", color = Color(0xFF7894C0), modifier = Modifier.padding(top = 14.dp)) }
    }
}

'''
main = main[:settings_start] + fixed_settings + main[settings_end:]

# Le service vocal ne lit plus aucun fichier ni SharedPreferences pour le mot de réveil.
# Une seule valeur compilée dans l'application évite toute opération d'enregistrement.
phrase_start = service.find('    private fun currentWakePhrase(): String')
normalize_start = service.find('    private fun normalizeWake(', phrase_start)
if phrase_start < 0 or normalize_start < 0:
    raise SystemExit('Fonction currentWakePhrase introuvable')
service = service[:phrase_start] + '    private fun currentWakePhrase(): String = "Salut Zeno"\n\n' + service[normalize_start:]

MAIN.write_text(main, encoding='utf-8')
SERVICE.write_text(service, encoding='utf-8')
print('Phrase fixe activée : Salut Zeno, aucune personnalisation ni enregistrement')
