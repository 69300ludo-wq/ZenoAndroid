from pathlib import Path

MAIN = Path("app/src/main/java/com/zeno/robot/MainActivity.kt")
s = MAIN.read_text(encoding="utf-8")

start_marker = '''@Composable
private fun TranslationScreen(accent: Color) {'''
end_marker = '''
@Composable
private fun CustomizeScreen'''

start = s.find(start_marker)
if start < 0:
    raise SystemExit("TranslationScreen introuvable")
end = s.find(end_marker, start)
if end < 0:
    raise SystemExit("Fin de TranslationScreen introuvable")

replacement = r'''private data class TranslationLanguageOption(val tag: String, val name: String)

private val translationLanguages = listOf(
    TranslationLanguageOption("af", "Afrikaans"),
    TranslationLanguageOption("sq", "Albanais"),
    TranslationLanguageOption("de", "Allemand"),
    TranslationLanguageOption("en", "Anglais"),
    TranslationLanguageOption("ar", "Arabe"),
    TranslationLanguageOption("be", "Biélorusse"),
    TranslationLanguageOption("bn", "Bengali"),
    TranslationLanguageOption("bg", "Bulgare"),
    TranslationLanguageOption("ca", "Catalan"),
    TranslationLanguageOption("zh", "Chinois"),
    TranslationLanguageOption("ko", "Coréen"),
    TranslationLanguageOption("hr", "Croate"),
    TranslationLanguageOption("da", "Danois"),
    TranslationLanguageOption("es", "Espagnol"),
    TranslationLanguageOption("eo", "Espéranto"),
    TranslationLanguageOption("et", "Estonien"),
    TranslationLanguageOption("fi", "Finnois"),
    TranslationLanguageOption("fr", "Français"),
    TranslationLanguageOption("gl", "Galicien"),
    TranslationLanguageOption("cy", "Gallois"),
    TranslationLanguageOption("ka", "Géorgien"),
    TranslationLanguageOption("el", "Grec"),
    TranslationLanguageOption("gu", "Gujarati"),
    TranslationLanguageOption("ht", "Créole haïtien"),
    TranslationLanguageOption("he", "Hébreu"),
    TranslationLanguageOption("hi", "Hindi"),
    TranslationLanguageOption("hu", "Hongrois"),
    TranslationLanguageOption("id", "Indonésien"),
    TranslationLanguageOption("ga", "Irlandais"),
    TranslationLanguageOption("is", "Islandais"),
    TranslationLanguageOption("it", "Italien"),
    TranslationLanguageOption("ja", "Japonais"),
    TranslationLanguageOption("kn", "Kannada"),
    TranslationLanguageOption("lv", "Letton"),
    TranslationLanguageOption("lt", "Lituanien"),
    TranslationLanguageOption("mk", "Macédonien"),
    TranslationLanguageOption("ms", "Malais"),
    TranslationLanguageOption("mt", "Maltais"),
    TranslationLanguageOption("mr", "Marathi"),
    TranslationLanguageOption("nl", "Néerlandais"),
    TranslationLanguageOption("no", "Norvégien"),
    TranslationLanguageOption("ur", "Ourdou"),
    TranslationLanguageOption("fa", "Persan"),
    TranslationLanguageOption("pl", "Polonais"),
    TranslationLanguageOption("pt", "Portugais"),
    TranslationLanguageOption("ro", "Roumain"),
    TranslationLanguageOption("ru", "Russe"),
    TranslationLanguageOption("sr", "Serbe"),
    TranslationLanguageOption("sk", "Slovaque"),
    TranslationLanguageOption("sl", "Slovène"),
    TranslationLanguageOption("sv", "Suédois"),
    TranslationLanguageOption("sw", "Swahili"),
    TranslationLanguageOption("tl", "Tagalog"),
    TranslationLanguageOption("ta", "Tamoul"),
    TranslationLanguageOption("cs", "Tchèque"),
    TranslationLanguageOption("te", "Télougou"),
    TranslationLanguageOption("th", "Thaï"),
    TranslationLanguageOption("tr", "Turc"),
    TranslationLanguageOption("uk", "Ukrainien"),
    TranslationLanguageOption("vi", "Vietnamien")
).sortedBy { it.name }

@Composable
private fun TranslationLanguagePicker(
    selectedTag: String,
    label: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = translationLanguages.firstOrNull { it.tag == selectedTag } ?: translationLanguages.first()
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 10.sp, color = Color(0xFF91A5CF))
                Text(selected.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 430.dp)
        ) {
            translationLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.name) },
                    onClick = {
                        onSelected(language.tag)
                        expanded = false
                    },
                    trailingIcon = {
                        if (language.tag == selectedTag) Icon(Icons.Default.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun TranslationScreen(accent: Color) {
    val context = LocalContext.current
    val controller = remember { TranslationController() }
    var source by remember { mutableStateOf("fr") }
    var target by remember { mutableStateOf("en") }
    var text by remember { mutableStateOf("") }
    var translated by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { }
        onDispose { tts?.shutdown() }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Traducteur mondial", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Choisis la langue de départ et la langue d’arrivée.",
                color = Color(0xFF9AB4D8),
                fontSize = 12.sp
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TranslationLanguagePicker(source, "De", Modifier.weight(1f)) { source = it }
                IconButton(onClick = {
                    val oldSource = source
                    source = target
                    target = oldSource
                    val oldText = text
                    if (translated.isNotBlank()) {
                        text = translated
                        translated = oldText
                    }
                }) {
                    Icon(Icons.Default.SwapHoriz, "Inverser les langues", tint = accent)
                }
                TranslationLanguagePicker(target, "Vers", Modifier.weight(1f)) { target = it }
            }
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                label = { Text("Texte à traduire") },
                placeholder = { Text("Entrez votre texte...") }
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, source)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, source)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle dans la langue choisie")
                        }
                        runCatching { voiceLauncher.launch(intent) }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Dicter")
                }
                Button(
                    onClick = {
                        if (text.isBlank()) return@Button
                        if (source == target) {
                            translated = text
                            status = ""
                        } else {
                            status = "Traduction… le modèle peut se télécharger au premier usage."
                            controller.translate(
                                text,
                                source,
                                target,
                                onResult = { translated = it; status = "" },
                                onError = { status = it }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Translate, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Traduire")
                }
            }
        }
        item {
            Surface(
                color = Color(0xFF0A1B3B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Traduction :", color = accent, fontWeight = FontWeight.Bold)
                    Text(
                        if (translated.isBlank()) "Le résultat apparaîtra ici..." else translated,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    if (translated.isNotBlank()) {
                        TextButton(onClick = {
                            tts?.language = Locale.forLanguageTag(target)
                            tts?.speak(translated, TextToSpeech.QUEUE_FLUSH, null, "zeno_translation")
                        }) {
                            Icon(Icons.Default.VolumeUp, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Écouter")
                        }
                    }
                }
            }
        }
        if (status.isNotBlank()) {
            item { Text(status, color = Color(0xFFFFC46B), fontSize = 12.sp) }
        }
        item {
            Text(
                "Les modèles de traduction sont téléchargés automatiquement quand une langue est utilisée pour la première fois.",
                color = Color(0xFF7894C0),
                fontSize = 11.sp
            )
        }
    }
}
'''

s = s[:start] + replacement + s[end:]
MAIN.write_text(s, encoding="utf-8")
print("Traducteur mondial Zeno activé")
