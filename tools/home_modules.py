from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
s = p.read_text(encoding='utf-8')

# Retire le menu de navigation du bas : tous les accès sont sur l'accueil.
bottom_nav = '''                    NavigationBar(containerColor = Color(0xF006122A)) {
                        NavItem(Icons.Default.Home, "Accueil", false) { screen = Screen.HOME }
                        NavItem(Icons.Default.Chat, "Chat", screen == Screen.CHAT) { screen = Screen.CHAT }
                        NavItem(Icons.Default.Apps, "Apps", screen == Screen.APPS) { screen = Screen.APPS }
                        NavItem(Icons.Default.Groups, "Membres", screen == Screen.COMMUNITY) { screen = Screen.COMMUNITY }
                        NavItem(Icons.Default.Settings, "Réglages", screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
                    }
'''
if bottom_nav in s:
    s = s.replace(bottom_nav, '', 1)

# Ajoute un écran dédié à la phrase vocale afin que ce module n'ouvre plus les paramètres généraux.
old_enum = '''    WEB("Recherche Web"), CUSTOMIZE("Personnalisation"), COMMUNITY("Communauté"),
    SETTINGS("Paramètres"), ABOUT("À propos")
'''
new_enum = '''    WEB("Recherche Web"), CUSTOMIZE("Personnalisation"), COMMUNITY("Communauté"),
    VOICE_PHRASE("Phrase vocale"), SETTINGS("Paramètres"), ABOUT("À propos")
'''
if old_enum in s:
    s = s.replace(old_enum, new_enum, 1)

old_when = '''                            Screen.COMMUNITY -> CommunityScreen(accent)
                            Screen.SETTINGS -> SettingsScreen(accent) { screen = Screen.CUSTOMIZE }
'''
new_when = '''                            Screen.COMMUNITY -> CommunityScreen(accent)
                            Screen.VOICE_PHRASE -> WakePhraseScreen(accent)
                            Screen.SETTINGS -> SettingsScreen(accent) { screen = Screen.CUSTOMIZE }
'''
if old_when in s:
    s = s.replace(old_when, new_when, 1)

old_actions = '''        HomeAction(Icons.Default.Palette, "Personnalisation", "Thème et apparence", Screen.CUSTOMIZE, Color(0xFFFFB52E)),
        HomeAction(Icons.Default.Groups, "Communauté", "Partage et découvre", Screen.COMMUNITY, Color(0xFF4589FF))
'''
new_actions = '''        HomeAction(Icons.Default.Palette, "Personnalisation", "Change le thème et l’apparence", Screen.CUSTOMIZE, Color(0xFFFFB52E)),
        HomeAction(Icons.Default.Groups, "Communauté", "Ouvre l’espace communauté", Screen.COMMUNITY, Color(0xFF4589FF)),
        HomeAction(Icons.Default.Mic, "Phrase vocale", "Choisis la phrase pour réveiller Zeno", Screen.VOICE_PHRASE, Color(0xFFFF4FC4)),
        HomeAction(Icons.Default.Settings, "Paramètres", "Règle les options de Zeno", Screen.SETTINGS, Color(0xFF59D8FF)),
        HomeAction(Icons.Default.Info, "À propos", "Affiche la version et les informations", Screen.ABOUT, Color(0xFF7D7CFF))
'''
if old_actions in s:
    s = s.replace(old_actions, new_actions, 1)

# Rend aussi les descriptions des quatre premiers modules explicites et fidèles à leur action.
s = s.replace('HomeAction(Icons.Default.Chat, "Chat IA", "Discute avec moi", Screen.CHAT,',
              'HomeAction(Icons.Default.Chat, "Chat IA", "Ouvre le chat avec Zeno", Screen.CHAT,', 1)
s = s.replace('HomeAction(Icons.Default.Translate, "Traduction", "Traduis facilement", Screen.TRANSLATE,',
              'HomeAction(Icons.Default.Translate, "Traduction", "Traduis ton texte", Screen.TRANSLATE,', 1)
s = s.replace('HomeAction(Icons.Default.Public, "Recherche Web", "Trouve des réponses", Screen.WEB,',
              'HomeAction(Icons.Default.Public, "Recherche Web", "Lance une recherche sur le Web", Screen.WEB,', 1)
s = s.replace('HomeAction(Icons.Default.Apps, "Mes applications", "Gère tes applis", Screen.APPS,',
              'HomeAction(Icons.Default.Apps, "Mes applications", "Affiche et ouvre tes applications", Screen.APPS,', 1)

wake_screen = r'''
@Composable
private fun WakePhraseScreen(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_voice", Context.MODE_PRIVATE) }
    var savedPhrase by remember { mutableStateOf(prefs.getString("wake_phrase", "Salut Zeno") ?: "Salut Zeno") }
    var phrase by remember { mutableStateOf(savedPhrase) }
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
                "Choisis exactement la phrase qui doit réveiller Zeno.",
                color = Color(0xFF9EB1D7),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        item {
            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Phrase de réveil") },
                placeholder = { Text("Ex. Salut Zeno") },
                leadingIcon = { Icon(Icons.Default.RecordVoiceOver, null) }
            )
        }
        item {
            Button(
                onClick = {
                    val clean = phrase.trim().replace(Regex("\\s+"), " ")
                    if (clean.length >= 2) {
                        prefs.edit().putString("wake_phrase", clean).apply()
                        savedPhrase = clean
                        phrase = clean
                        status = "Phrase enregistrée : « $clean »"
                        context.stopService(Intent(context, FloatingZenoService::class.java))
                        if (Settings.canDrawOverlays(context)) startFloatingZeno(context)
                    } else {
                        status = "Entre une phrase d’au moins 2 caractères."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Enregistrer la phrase")
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accent.copy(alpha = .10f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .45f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Phrase active", color = Color(0xFF91A5CF), fontSize = 12.sp)
                    Text("« $savedPhrase »", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, color = accent, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    if (Settings.canDrawOverlays(context)) {
                        startFloatingZeno(context)
                        status = "Écoute vocale activée. Dis : « $savedPhrase »"
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                        status = "Autorise Zeno à s’afficher par-dessus les autres applications, puis reviens activer l’écoute."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Hearing, null)
                Spacer(Modifier.width(8.dp))
                Text("Activer l’écoute vocale")
            }
        }
    }
}

'''
marker = '''@Composable
private fun ActionGrid(items: List<HomeAction>, navigate: (Screen) -> Unit) {'''
if 'private fun WakePhraseScreen(' not in s and marker in s:
    s = s.replace(marker, wake_screen + marker, 1)

p.write_text(s, encoding='utf-8')
print('Chaque module de l accueil ouvre maintenant la fonction indiquée, avec un écran Phrase vocale dédié')
