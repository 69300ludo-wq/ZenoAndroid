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
import androidx.compose.ui.graphics.vector.ImageVector
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
    HOME("Zeno"), CHAT("Chat IA"), APPS("Mes applications"), TRANSLATE("Traduction"),
    WEB("Recherche Web"), CUSTOMIZE("Personnalisation"), COMMUNITY("Communauté"),
    SETTINGS("Paramètres"), ABOUT("À propos")
}

private data class HomeAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val screen: Screen,
    val tint: Color
)

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
                Brush.verticalGradient(
                    listOf(Color(0xFF07142F), Color(0xFF02091B), Color(0xFF07102A), Color(0xFF020617))
                )
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
                            Screen.ABOUT -> AboutScreen(accent)
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp).clickable(onClick = onBack),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF111A45),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .35f))
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(screen.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            if (screen != Screen.ABOUT) Text("Zeno", color = Color(0xFF879CC8), fontSize = 10.sp)
        }
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF111A45), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SmartToy, null, tint = accent) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 10.sp) }
    )
}

@Composable
private fun HomeScreen(accent: Color, navigate: (Screen) -> Unit) {
    val actions = listOf(
        HomeAction(Icons.Default.Chat, "Chat IA", "Discute avec moi", Screen.CHAT, Color(0xFF00A8FF)),
        HomeAction(Icons.Default.Translate, "Traduction", "Traduis facilement", Screen.TRANSLATE, Color(0xFF9A4DFF)),
        HomeAction(Icons.Default.Public, "Recherche Web", "Trouve des réponses", Screen.WEB, Color(0xFFFF4FC4)),
        HomeAction(Icons.Default.Apps, "Mes applications", "Gère tes applis", Screen.APPS, Color(0xFF00D6A3)),
        HomeAction(Icons.Default.Palette, "Personnalisation", "Thème et apparence", Screen.CUSTOMIZE, Color(0xFFFFB52E)),
        HomeAction(Icons.Default.Groups, "Communauté", "Partage et découvre", Screen.COMMUNITY, Color(0xFF4589FF))
    )

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = .18f), Color(0x221D4ED8), Color.Transparent),
                radius = 850f
            )
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 12.dp, bottom = 26.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp).clickable { navigate(Screen.SETTINGS) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x99101D3C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24548A))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, "Paramètres", tint = Color.White) }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x99251A32),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF76502C))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFC64B)) }
                    }
                }

                Text(
                    "Zeno",
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Votre assistant intelligent",
                    color = Color(0xFFC3CDED),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(185.dp)) {
                    Box(
                        Modifier.size(148.dp).background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1EC8FF), accent, Color(0xFF8A2DFF), Color(0xFF071B4D))
                            ),
                            CircleShape
                        )
                    )
                    Box(
                        Modifier.size(132.dp).background(Color(0xFF071F62), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = "Zeno",
                            modifier = Modifier.size(118.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xD9081A39),
                    shape = RoundedCornerShape(17.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF174D84))
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0A61BE), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SmartToy, null, tint = Color.White) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bonjour ! 👋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Je suis Zeno, votre assistant IA.\nQue puis-je faire pour vous aujourd’hui ?",
                                color = Color(0xFFB8C5E6),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
            item { ActionGrid(actions, navigate) }
            item {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(58.dp).clickable { navigate(Screen.CHAT) },
                    color = Color(0xE6091737),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7447F4))
                ) {
                    Row(
                        Modifier.padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartToy, null, tint = Color(0xFF59D8FF))
                        Spacer(Modifier.width(11.dp))
                        Text("Demande à Zeno...", color = Color(0xFF91A4CE), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Mic, null, tint = Color.White)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Paramètres",
                        color = Color(0xFF839AC8),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { navigate(Screen.SETTINGS) }.padding(8.dp)
                    )
                    Text("•", color = Color(0xFF536A96), modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        "À propos",
                        color = Color(0xFF839AC8),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { navigate(Screen.ABOUT) }.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionGrid(items: List<HomeAction>, navigate: (Screen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { action ->
                    Surface(
                        modifier = Modifier.weight(1f).height(82.dp).clickable { navigate(action.screen) },
                        color = action.tint.copy(alpha = .10f),
                        shape = RoundedCornerShape(17.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, action.tint.copy(alpha = .55f))
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = action.tint.copy(alpha = .20f)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(action.icon, null, tint = action.tint) }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(action.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(action.subtitle, color = Color(0xFF91A5CF), fontSize = 10.sp, maxLines = 2)
                            }
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
    val messages = remember {
        mutableStateListOf(
            ChatMessage(true, "Bonjour ! 👋\nJe suis Zeno, votre assistant IA. Posez-moi toutes vos questions, je suis là pour vous aider.")
        )
    }
    var input by remember { mutableStateOf("") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            input = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg -> MessageBubble(msg, accent) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Écris un message...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendChat(input, messages, brain) { input = "" } })
            )
            IconButton(onClick = { launchSpeech(context, voiceLauncher) }) { Icon(Icons.Default.Mic, null, tint = accent) }
            FilledIconButton(onClick = { sendChat(input, messages, brain) { input = "" } }) { Icon(Icons.Default.Send, null) }
        }
    }
}

private fun sendChat(input: String, messages: SnapshotStateList<ChatMessage>, brain: ZenoBrain, clear: () -> Unit) {
    if (input.isBlank()) return
    messages += ChatMessage(false, input.trim())
    val reply = brain.reply(input.trim())
    messages += ChatMessage(true, when (reply) {
        is ZenoBrain.Result.Text -> reply.text
        is ZenoBrain.Result.Action -> reply.text
    })
    clear()
}

@Composable
private fun MessageBubble(message: ChatMessage, accent: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromZeno) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (message.fromZeno) Color(0xFF0B2552) else accent.copy(alpha = .25f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (message.fromZeno) accent.copy(alpha = .35f) else accent)
        ) {
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
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Rechercher une application...") }
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { app ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { launcher.openByPackage(app.packageName) },
                    color = Color(0xB30A1B3B),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = accent.copy(alpha = .18f), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(app.label.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                app.packageName,
                                color = Color(0xFF7E9BC7),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
        Text("Recherche sur le web", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Rechercher sur le web...") })
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (query.isNotBlank()) brain.searchWeb(query) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Rechercher")
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
    DisposableEffect(Unit) { tts = TextToSpeech(context) { }; onDispose { tts?.shutdown() } }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = source == "fr",
                onClick = { source = "fr"; target = "en" },
                label = { Text("Français → Anglais") }
            )
            FilterChip(
                selected = source == "en",
                onClick = { source = "en"; target = "fr" },
                label = { Text("Anglais → Français") }
            )
        }
        OutlinedTextField(
            text,
            { text = it },
            Modifier.fillMaxWidth().height(150.dp),
            label = { Text("Entrez votre texte...") }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launchSpeech(context, voiceLauncher) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(6.dp))
                Text("Dicter")
            }
            Button(
                onClick = {
                    status = "Traduction…"
                    controller.translate(
                        text,
                        source,
                        target,
                        onResult = { translated = it; status = "" },
                        onError = { status = it }
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("Traduire") }
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = Color(0xFF0A1B3B), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
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
                        tts?.language = if (target == "fr") Locale.FRENCH else Locale.ENGLISH
                        tts?.speak(translated, TextToSpeech.QUEUE_FLUSH, null, "zeno_translation")
                    }) {
                        Icon(Icons.Default.VolumeUp, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Écouter")
                    }
                }
            }
        }
        if (status.isNotBlank()) Text(status, color = Color(0xFFFFC46B), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CustomizeScreen(current: ZenoTheme, accent: Color, onTheme: (ZenoTheme) -> Unit) {
    val context = LocalContext.current
    var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val overlayPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayEnabled = Settings.canDrawOverlays(context)
        if (overlayEnabled) startFloatingZeno(context)
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Thème de l'application", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Choisis les couleurs et l’ambiance de Zeno.", color = Color(0xFF9AB4D8))
        }
        items(ZenoTheme.entries) { theme ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onTheme(theme) },
                color = if (theme == current) Color(theme.accent).copy(alpha = .20f) else Color(0xFF0A1B3B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.accent))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(theme.accent)))
                    Spacer(Modifier.width(12.dp))
                    Text(theme.label, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (theme == current) Icon(Icons.Default.CheckCircle, null, tint = Color(theme.accent))
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Zeno flottant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Affiche le petit robot au-dessus de tes autres applications.", color = Color(0xFF9AB4D8))
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        overlayPermission.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    } else {
                        startFloatingZeno(context)
                        overlayEnabled = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SmartToy, null)
                Spacer(Modifier.width(8.dp))
                Text(if (overlayEnabled) "Relancer Zeno flottant" else "Activer Zeno flottant")
            }
            if (overlayEnabled) {
                OutlinedButton(
                    onClick = {
                        context.stopService(Intent(context, FloatingZenoService::class.java))
                        overlayEnabled = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Arrêter le robot flottant") }
            }
        }
    }
}

@Composable
private fun CommunityScreen(accent: Color) {
    val messages = remember {
        mutableStateListOf(
            "Léa : Bonjour la communauté Zeno 👋",
            "Alex : J’adore le thème Galaxie !",
            "Zeno : Le chat entre membres est prêt côté interface."
        )
    }
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Surface(
            color = Color(0x3329B6F6),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Prototype local : pour discuter réellement entre plusieurs téléphones, il reste à connecter le serveur communauté Zeno.",
                color = Color(0xFFBEEBFF),
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { message ->
                Surface(color = Color(0xFF0A1B3B), shape = RoundedCornerShape(14.dp)) {
                    Text(message, color = Color.White, modifier = Modifier.padding(12.dp))
                }
            }
        }
        Row {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Message aux membres…") })
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    messages += "Moi : ${input.trim()}"
                    input = ""
                }
            }) { Icon(Icons.Default.Send, null, tint = accent) }
        }
    }
}

@Composable
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

@Composable
private fun SettingCard(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(0xFF0A1B3B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF173B68))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF8FA9D0), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8FA9D0))
        }
    }
}

@Composable
private fun AboutScreen(accent: Color) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        Box(
            Modifier.size(92.dp).background(
                Brush.radialGradient(listOf(Color(0xFF1EC8FF), accent, Color(0xFF9A35FF))),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Zeno", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Text("Votre assistant intelligent", color = Color(0xFFB7C5E5))
        Text("Version 1.3.1", color = Color(0xFF7E94BE), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            "Zeno est une application Android tout-en-un qui vous accompagne avec le chat IA, la traduction, la recherche web, vos applications, la personnalisation et la communauté.",
            color = Color(0xFFA7B8D9),
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )
    }
}

private fun startFloatingZeno(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
}

private fun launchSpeech(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Zeno")
    }
    runCatching { launcher.launch(intent) }
}
