package com.zeno.robot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.IconManager
import com.zeno.robot.data.TranslationController
import com.zeno.robot.data.ZenoBrain
import com.zeno.robot.model.ChatMessage
import com.zeno.robot.model.InstalledApp
import com.zeno.robot.model.ZenoTheme
import com.zeno.robot.service.FloatingZenoService
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoApp() }
    }
}

private enum class Screen(val title: String) {
    HOME("Zeno"), CHAT("Chat IA"), APPS("Mes applications"), TRANSLATE("Traduction vocale"),
    WEB("Recherche web"), CUSTOMIZE("Personnalisation"), COMMUNITY("Communauté"), SETTINGS("Paramètres")
}

@Composable
private fun ZenoApp() {
    val context = LocalContext.current
    val iconManager = remember { IconManager(context) }
    var theme by remember { mutableStateOf(iconManager.current()) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    val accent = Color(theme.accent)
    val scheme = darkColorScheme(
        primary = accent,
        secondary = Color(0xFFB65CFF),
        background = Color(0xFF020617),
        surface = Color(0xFF07132D),
        surfaceVariant = Color(0xFF0C1E43)
    )

    MaterialTheme(colorScheme = scheme) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF01030D), Color(0xFF071A3D), Color(0xFF09061E), Color(0xFF020617)))
            )
        ) {
            if (screen == Screen.HOME) {
                HomeScreen(accent) { screen = it }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ZenoTopBar(screen, accent, onBack = { screen = Screen.HOME })
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            Screen.CHAT -> ChatScreen(accent)
                            Screen.APPS -> AppsScreen(accent)
                            Screen.TRANSLATE -> TranslationScreen(accent)
                            Screen.WEB -> WebSearchScreen(accent)
                            Screen.CUSTOMIZE -> CustomizeScreen(theme, accent, onTheme = {
                                iconManager.apply(it)
                                theme = it
                            })
                            Screen.COMMUNITY -> CommunityScreen(accent)
                            Screen.SETTINGS -> SettingsScreen(accent) { screen = Screen.CUSTOMIZE }
                            Screen.HOME -> Unit
                        }
                    }
                    NavigationBar(containerColor = Color(0xF006122A)) {
                        NavItem(Icons.Default.Home, "Accueil", false) { screen = Screen.HOME }
                        NavItem(Icons.Default.Chat, "Chat", screen == Screen.CHAT) { screen = Screen.CHAT }
                        NavItem(Icons.Default.Apps, "Apps", screen == Screen.APPS) { screen = Screen.APPS }
                        NavItem(Icons.Default.Groups, "Membres", screen == Screen.COMMUNITY) { screen = Screen.COMMUNITY }
                        NavItem(Icons.Default.Settings, "Réglages", screen == Screen.SETTINGS) { screen = Screen.SETTINGS }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenoTopBar(screen: Screen, accent: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = accent) }
        Text(screen.title.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Surface(shape = CircleShape, color = accent.copy(alpha = .15f)) {
            Text("● ZENO ACTIF", color = accent, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Icon(icon, contentDescription = label) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun HomeScreen(accent: Color, navigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val setupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = .22f), Color(0x221D4ED8), Color.Transparent),
                radius = 900f
            )
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 18.dp, bottom = 30.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ZENO", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("TON UNIVERS IA", color = Color(0xFF99B8E8), fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                    Surface(shape = CircleShape, color = Color(0x3316FFB1)) {
                        Text("● PRÊT", color = Color(0xFF72FFC8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(330.dp)) {
                    Box(
                        Modifier.size(320.dp).background(
                            Brush.radialGradient(listOf(accent.copy(alpha = .32f), Color(0x223B82F6), Color.Transparent)),
                            CircleShape
                        )
                    )
                    Image(
                        painter = painterResource(R.drawable.zeno_robot),
                        contentDescription = "Zeno dans son univers",
                        modifier = Modifier.size(295.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text("Entre dans l’univers de Zeno", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(
                    "Parle, cherche, traduis et ouvre tes applications avec ton compagnon Android.",
                    color = Color(0xFFB6C9EA), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Surface(color = Color(0x2219C8FF), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .6f))) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = accent)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Dis : « Salut Zeno »", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Zeno répond : « Oui, je t’écoute »", color = Color(0xFF9FBAE5), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navigate(Screen.CHAT) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(10.dp))
                    Text("PARLER À ZENO", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { setupLauncher.launch(Intent(context, SetupActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = accent)
                    Spacer(Modifier.width(10.dp))
                    Text("ACTIVER ZENO SUR L’ÉCRAN", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(20.dp))
                Text("EXPLORE ZENO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            item {
                ActionGrid(
                    listOf(
                        Triple(Icons.Default.Chat, "Chat IA", Screen.CHAT),
                        Triple(Icons.Default.Translate, "Traduction", Screen.TRANSLATE),
                        Triple(Icons.Default.Public, "Recherche web", Screen.WEB),
                        Triple(Icons.Default.Apps, "Mes applis", Screen.APPS),
                        Triple(Icons.Default.Palette, "Personnaliser", Screen.CUSTOMIZE),
                        Triple(Icons.Default.Groups, "Communauté", Screen.COMMUNITY)
                    ), accent, navigate
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(items: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, Screen>>, accent: Color, navigate: (Screen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, screen) ->
                    Surface(
                        modifier = Modifier.weight(1f).height(96.dp).clickable { navigate(screen) },
                        color = Color(0xB30A1B3B), shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .48f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
                            Icon(icon, null, tint = accent)
                            Spacer(Modifier.height(9.dp))
                            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChatScreen(accent: Color) {
    val context = LocalContext.current
    val brain = remember { ZenoBrain(context.applicationContext) }
    val messages = remember { mutableStateListOf(ChatMessage(true, "Bonjour ! Parle-moi ou écris-moi. Tu peux dire « ouvre YouTube » ou « cherche restaurants à Lyon ».")) }
    var input by remember { mutableStateOf("") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) input = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
    }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(messages) { msg -> MessageBubble(msg, accent) } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Parle à Zeno…") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { sendChat(input, messages, brain) { input = "" } }))
            IconButton(onClick = { launchSpeech(context, voiceLauncher) }) { Icon(Icons.Default.Mic, null, tint = accent) }
            FilledIconButton(onClick = { sendChat(input, messages, brain) { input = "" } }) { Icon(Icons.Default.Send, null) }
        }
    }
}

private fun sendChat(input: String, messages: SnapshotStateList<ChatMessage>, brain: ZenoBrain, clear: () -> Unit) {
    if (input.isBlank()) return
    messages += ChatMessage(false, input.trim())
    val reply = brain.reply(input.trim())
    messages += ChatMessage(true, when (reply) { is ZenoBrain.Result.Text -> reply.text; is ZenoBrain.Result.Action -> reply.text })
    clear()
}

@Composable
private fun MessageBubble(message: ChatMessage, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromZeno) Arrangement.Start else Arrangement.End) {
        Surface(color = if (message.fromZeno) Color(0xFF0B2552) else accent.copy(alpha = .25f), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (message.fromZeno) accent.copy(alpha = .35f) else accent)) {
            Text(message.text, color = Color.White, modifier = Modifier.padding(14.dp).widthIn(max = 290.dp))
        }
    }
}

@Composable
private fun AppsScreen(accent: Color) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context) }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    LaunchedEffect(Unit) { apps = launcher.listLaunchableApps() }
    val filtered = remember(apps, query) { apps.filter { it.label.contains(query, ignoreCase = true) } }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Rechercher une application") })
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { app ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { launcher.openByPackage(app.packageName) }, color = Color(0xB30A1B3B), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = accent.copy(alpha = .18f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Text(app.label.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold); Text(app.packageName, color = Color(0xFF7E9BC7), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Icon(Icons.Default.OpenInNew, null, tint = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebSearchScreen(accent: Color) {
    val context = LocalContext.current
    val brain = remember { ZenoBrain(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Public, null, tint = accent, modifier = Modifier.size(68.dp))
        Text("Recherche sur le net", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Que veux-tu rechercher ?") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (query.isNotBlank()) brain.searchWeb(query) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Rechercher") }
    }
}

@Composable
private fun TranslationScreen(accent: Color) {
    val context = LocalContext.current
    val controller = remember { TranslationController() }
    var source by remember { mutableStateOf("fr") }; var target by remember { mutableStateOf("en") }; var text by remember { mutableStateOf("") }; var translated by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }; var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) { tts = TextToSpeech(context) { }; onDispose { tts?.shutdown() } }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reconnaissance vocale + traduction", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = source == "fr", onClick = { source = "fr"; target = "en" }, label = { Text("Français → Anglais") }); FilterChip(selected = source == "en", onClick = { source = "en"; target = "fr" }, label = { Text("Anglais → Français") }) }
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(150.dp), label = { Text("Texte à traduire") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launchSpeech(context, voiceLauncher) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text("Dicter") }
            Button(onClick = { status = "Traduction…"; controller.translate(text, source, target, onResult = { translated = it; status = "" }, onError = { status = it }) }, modifier = Modifier.weight(1f)) { Text("Traduire") }
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Color(0xFF0A1B3B), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Résultat", color = accent, fontWeight = FontWeight.Bold)
                Text(if (translated.isBlank()) "La traduction apparaîtra ici." else translated, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(vertical = 10.dp))
                if (translated.isNotBlank()) TextButton(onClick = { tts?.language = if (target == "fr") Locale.FRENCH else Locale.ENGLISH; tts?.speak(translated, TextToSpeech.QUEUE_FLUSH, null, "zeno_translation") }) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(6.dp)); Text("Écouter") }
            }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFFFC46B), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CustomizeScreen(current: ZenoTheme, accent: Color, onTheme: (ZenoTheme) -> Unit) {
    val context = LocalContext.current
    var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val overlayPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { overlayEnabled = Settings.canDrawOverlays(context); if (overlayEnabled) startFloatingZeno(context) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Couleurs et icône", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("Le thème change l’ambiance de Zeno et son icône Android.", color = Color(0xFF9AB4D8)) }
        items(ZenoTheme.entries) { theme ->
            Surface(modifier = Modifier.fillMaxWidth().clickable { onTheme(theme) }, color = if (theme == current) Color(theme.accent).copy(alpha = .20f) else Color(0xFF0A1B3B), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.accent))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).clip(CircleShape).background(Color(theme.accent))); Spacer(Modifier.width(12.dp)); Text(theme.label, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold); if (theme == current) Icon(Icons.Default.CheckCircle, null, tint = Color(theme.accent)) }
            }
        }
        item {
            Spacer(Modifier.height(8.dp)); Text("Zeno flottant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("Affiche le petit robot au-dessus de tes autres applications.", color = Color(0xFF9AB4D8)); Spacer(Modifier.height(10.dp))
            Button(onClick = { if (!Settings.canDrawOverlays(context)) overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) else { startFloatingZeno(context); overlayEnabled = true } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.SmartToy, null); Spacer(Modifier.width(8.dp)); Text(if (overlayEnabled) "Relancer Zeno flottant" else "Activer Zeno flottant") }
            if (overlayEnabled) OutlinedButton(onClick = { context.stopService(Intent(context, FloatingZenoService::class.java)); overlayEnabled = false }, modifier = Modifier.fillMaxWidth()) { Text("Arrêter le robot flottant") }
        }
    }
}

@Composable
private fun CommunityScreen(accent: Color) {
    val messages = remember { mutableStateListOf("Léa : Bonjour la communauté Zeno 👋", "Alex : J’adore le thème Galaxie !", "Zeno : Le chat entre membres est prêt côté interface.") }
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Surface(color = Color(0x3329B6F6), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Prototype local : pour discuter réellement entre plusieurs téléphones, il reste à connecter le serveur communauté Zeno.", color = Color(0xFFBEEBFF), modifier = Modifier.padding(12.dp), fontSize = 12.sp) }
        Spacer(Modifier.height(10.dp)); LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(messages) { message -> Surface(color = Color(0xFF0A1B3B), shape = RoundedCornerShape(14.dp)) { Text(message, color = Color.White, modifier = Modifier.padding(12.dp)) } } }
        Row { OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Message aux membres…") }); IconButton(onClick = { if (input.isNotBlank()) { messages += "Moi : ${input.trim()}"; input = "" } }) { Icon(Icons.Default.Send, null, tint = accent) } }
    }
}

@Composable
private fun SettingsScreen(accent: Color, customize: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingCard(Icons.Default.Palette, "Apparence", "Couleurs, icône et Zeno flottant", accent, customize) }
        item { SettingCard(Icons.Default.Mic, "Voix", "Réveil vocal : Salut Zeno", accent) {} }
        item { SettingCard(Icons.Default.Translate, "Langues", "Traduction locale via ML Kit", accent) {} }
        item { SettingCard(Icons.Default.Apps, "Applications", "Zeno peut ouvrir les applications installées", accent) {} }
        item { SettingCard(Icons.Default.Security, "Confidentialité", "Micro et superposition contrôlés par Android", accent) {} }
        item { Text("Zeno Android v0.4.0", color = Color(0xFF7894C0), modifier = Modifier.padding(top = 14.dp)) }
    }
}

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = Color(0xFF0A1B3B), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = accent); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Color(0xFF8FA9D0), fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8FA9D0)) }
    }
}

private fun startFloatingZeno(context: Context) { ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java)) }
private fun launchSpeech(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag()); putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Zeno") }
    runCatching { launcher.launch(intent) }
}
