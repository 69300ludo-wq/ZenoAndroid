package com.zeno.robot

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zeno.robot.data.AppFolder
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.HomeSettings
import com.zeno.robot.data.IconManager
import com.zeno.robot.data.LauncherPreferences
import com.zeno.robot.model.InstalledApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoLauncher() }
    }
}

private enum class LauncherPage { HOME, APPS, CUSTOMIZE }

@Composable
private fun ZenoLauncher() {
    val context = LocalContext.current
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val preferences = remember { LauncherPreferences(context.applicationContext) }
    var homeSettings by remember { mutableStateOf(preferences.loadHomeSettings()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(LauncherPage.HOME) }

    fun updateSettings(newSettings: HomeSettings) {
        homeSettings = newSettings
        preferences.saveHomeSettings(newSettings)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            secondary = Color(0xFF9B6CFF),
            background = Color(0xFF02040A),
            surface = Color(0xFF07162B)
        )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color(0xF2050B16),
                    drawerContentColor = Color.White
                ) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(58.dp),
                            shape = CircleShape,
                            color = accent.copy(alpha = .12f),
                            border = BorderStroke(1.dp, accent.copy(alpha = .55f))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.zeno_robot),
                                contentDescription = null,
                                modifier = Modifier.padding(3.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 2.5.sp)
                            Text("GALAXY HOME", color = accent, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                            Text("Launcher Android", color = Color.White.copy(alpha = .48f), fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    DrawerItem(Icons.Default.Home, "Accueil", page == LauncherPage.HOME) {
                        page = LauncherPage.HOME
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Apps, "Applications et dossiers", page == LauncherPage.APPS) {
                        page = LauncherPage.APPS
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Tune, "Personnaliser l'accueil", page == LauncherPage.CUSTOMIZE) {
                        page = LauncherPage.CUSTOMIZE
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Mic, "Parler à Zeno", false) {
                        context.startActivity(Intent(context, VoiceCommandActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.SmartToy, "Zeno flottant", false) {
                        context.startActivity(Intent(context, SetupActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Settings, "Réglages Zeno", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Zeno Home ${BuildConfig.VERSION_NAME}",
                        color = Color.White.copy(alpha = .38f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        ) {
            when (page) {
                LauncherPage.HOME -> ZenoHome(
                    accent = accent,
                    settings = homeSettings,
                    openMenu = { scope.launch { drawerState.open() } },
                    openApps = { page = LauncherPage.APPS },
                    customize = { page = LauncherPage.CUSTOMIZE },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) },
                    openZeno = { context.startActivity(Intent(context, MainActivity::class.java)) }
                )
                LauncherPage.APPS -> LauncherApps(
                    accent = accent,
                    preferences = preferences,
                    openMenu = { scope.launch { drawerState.open() } },
                    back = { page = LauncherPage.HOME }
                )
                LauncherPage.CUSTOMIZE -> CustomizeHome(
                    accent = accent,
                    settings = homeSettings,
                    onSettingsChange = ::updateSettings,
                    back = { page = LauncherPage.HOME }
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        icon = { Icon(icon, null) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
    )
}

@Composable
private fun ZenoHome(
    accent: Color,
    settings: HomeSettings,
    openMenu: () -> Unit,
    openApps: () -> Unit,
    customize: () -> Unit,
    speak: () -> Unit,
    openZeno: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var query by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val favoriteApps = remember(apps, settings.homePackages, settings.appCount) {
        val byPackage = apps.associateBy { it.packageName }
        val selected = settings.homePackages.mapNotNull { byPackage[it] }
        if (selected.isEmpty()) apps.take(settings.appCount) else selected.take(settings.appCount)
    }
    val shownApps = remember(apps, favoriteApps, query, settings.appCount) {
        if (query.isBlank()) favoriteApps
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(settings.appCount)
    }

    val pulse = rememberInfiniteTransition(label = "zenoPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "robotScale"
    )
    val glow by pulse.animateFloat(
        initialValue = .18f,
        targetValue = .38f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "robotGlow"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF01030A), Color(0xFF071C35), Color(0xFF0A1830), Color(0xFF081020), Color(0xFF02050B))
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(Color(0x3318D8FF), Color(0x221B70FF), Color.Transparent),
                    radius = 1050f
                )
            )
    ) {
        if (settings.showRobot) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(570.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 118.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(settings.robotSize.dp)
                        .alpha(glow)
                        .background(
                            Brush.radialGradient(listOf(accent, Color(0xFF635BFF), Color.Transparent)),
                            CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.zeno_robot),
                    contentDescription = "Zeno",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(settings.robotSize.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clickable(onClick = speak)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = openMenu, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White.copy(alpha = .88f))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (settings.showClock) {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 70.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)).replaceFirstChar { it.uppercase() },
                            color = Color(0xFFEAF6FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("● ZENO PRÊT", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                }
                IconButton(onClick = customize, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Personnaliser", tint = Color.White.copy(alpha = .88f))
                }
            }

            Spacer(Modifier.height(if (settings.showRobot) (settings.robotSize * .58f).dp else 35.dp))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .78f)) },
                    trailingIcon = { IconButton(onClick = speak) { Icon(Icons.Default.Mic, null, tint = accent) } },
                    placeholder = { Text("Rechercher une application...", color = Color.White.copy(alpha = .55f)) },
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xB3111722),
                        unfocusedContainerColor = Color(0xA30C111B),
                        focusedBorderColor = accent.copy(alpha = .55f),
                        unfocusedBorderColor = Color.White.copy(alpha = .13f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(18.dp))
            }

            if (settings.showApps) {
                shownApps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            HomeAppIcon(app = app, modifier = Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            if (settings.showDock) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    color = Color(0x99101826),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DockButton(Icons.Default.Apps, accent, openApps)
                        DockButton(Icons.Default.Chat, Color(0xFF9B6CFF), openZeno)
                        DockButton(Icons.Default.Mic, accent, speak, large = true)
                        DockButton(Icons.Default.SmartToy, Color(0xFF9B6CFF), openZeno)
                        DockButton(Icons.Default.Tune, accent, customize)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAppIcon(app: InstalledApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0x66141D2A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))
        ) {
            if (appIcon != null) {
                AndroidView(
                    factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                    update = { it.setImageDrawable(appIcon) },
                    modifier = Modifier.padding(7.dp)
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 3.dp)
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    large: Boolean = false
) {
    Surface(
        modifier = Modifier.size(if (large) 58.dp else 48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(if (large) 20.dp else 17.dp),
        color = if (large) tint.copy(alpha = .90f) else Color(0xB2182435),
        border = BorderStroke(1.dp, if (large) Color.White.copy(alpha = .25f) else tint.copy(alpha = .25f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (large) Color(0xFF00131A) else Color.White, modifier = Modifier.size(if (large) 28.dp else 24.dp))
        }
    }
}

@Composable
private fun CustomizeHome(
    accent: Color,
    settings: HomeSettings,
    onSettingsChange: (HomeSettings) -> Unit,
    back: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF02050B), Color(0xFF071729), Color(0xFF02050B)))
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                    Column(Modifier.weight(1f)) {
                        Text("PERSONNALISER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("TA PAGE D'ACCUEIL", color = accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { SettingSwitch("Horloge et date", "Afficher l'heure en grand", settings.showClock) { onSettingsChange(settings.copy(showClock = it)) } }
            item { SettingSwitch("Robot Zeno", "Afficher le robot vivant", settings.showRobot) { onSettingsChange(settings.copy(showRobot = it)) } }
            item { SettingSwitch("Barre de recherche", "Afficher la recherche sur l'accueil", settings.showSearch) { onSettingsChange(settings.copy(showSearch = it)) } }
            item { SettingSwitch("Applications", "Afficher les raccourcis d'applications", settings.showApps) { onSettingsChange(settings.copy(showApps = it)) } }
            item { SettingSwitch("Dock du bas", "Afficher la barre de raccourcis", settings.showDock) { onSettingsChange(settings.copy(showDock = it)) } }

            if (settings.showRobot) {
                item {
                    CustomSliderCard(
                        title = "Taille du robot",
                        valueLabel = "${settings.robotSize}%".replace("430%", "Normal"),
                        value = settings.robotSize.toFloat(),
                        valueRange = 300f..520f,
                        onValueChange = { onSettingsChange(settings.copy(robotSize = it.roundToInt().coerceIn(300, 520))) }
                    )
                }
            }

            if (settings.showApps) {
                item {
                    CustomSliderCard(
                        title = "Nombre d'applications",
                        valueLabel = settings.appCount.toString(),
                        value = settings.appCount.toFloat(),
                        valueRange = 4f..12f,
                        steps = 7,
                        onValueChange = { value ->
                            val count = value.roundToInt().coerceIn(4, 12)
                            onSettingsChange(settings.copy(appCount = count, homePackages = settings.homePackages.take(count)))
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("CHOISIS TES APPLIS D'ACCUEIL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("${settings.homePackages.size}/${settings.appCount} sélectionnées", color = accent, fontSize = 11.sp)
                }
                items(apps) { app ->
                    val checked = app.packageName in settings.homePackages
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xB20A1422),
                        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = .45f) else Color.White.copy(alpha = .07f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                val next = settings.homePackages.toMutableList()
                                if (checked) next.remove(app.packageName)
                                else if (next.size < settings.appCount) next.add(app.packageName)
                                onSettingsChange(settings.copy(homePackages = next))
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniAppIcon(app)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB20A1422),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun CustomSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB20A1422),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(valueLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
        }
    }
}

@Composable
private fun LauncherApps(
    accent: Color,
    preferences: LauncherPreferences,
    openMenu: () -> Unit,
    back: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var folders by remember { mutableStateOf(preferences.loadFolders()) }
    var editingFolder by remember { mutableStateOf<AppFolder?>(null) }
    var showFolderEditor by remember { mutableStateOf(false) }
    var openedFolder by remember { mutableStateOf<AppFolder?>(null) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    val filtered = remember(apps, query) { apps.filter { it.label.contains(query, ignoreCase = true) } }
    val visibleFolders = remember(folders, query) {
        if (query.isBlank()) folders else folders.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun saveFolder(folder: AppFolder) {
        val next = folders.toMutableList()
        val index = next.indexOfFirst { it.id == folder.id }
        if (index >= 0) next[index] = folder else next.add(folder)
        folders = next
        preferences.saveFolders(next)
    }

    fun deleteFolder(folder: AppFolder) {
        folders = folders.filterNot { it.id == folder.id }
        preferences.saveFolders(folders)
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF02050B), Color(0xFF071729), Color(0xFF02050B)))
        )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                Column(Modifier.weight(1f)) {
                    Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("DOSSIERS PERSONNALISÉS", color = accent, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {
                    editingFolder = null
                    showFolderEditor = true
                }) {
                    Icon(Icons.Default.CreateNewFolder, "Créer un dossier", tint = accent)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                trailingIcon = {
                    IconButton(onClick = {
                        editingFolder = null
                        showFolderEditor = true
                    }) { Icon(Icons.Default.Add, "Nouveau dossier", tint = accent) }
                },
                placeholder = { Text("Rechercher une application ou un dossier") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                if (visibleFolders.isNotEmpty()) {
                    item {
                        Text("MES DOSSIERS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }
                    items(visibleFolders.chunked(4)) { rowFolders ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowFolders.forEach { folder ->
                                FolderTile(folder, accent, Modifier.weight(1f)) { openedFolder = folder }
                            }
                            repeat(4 - rowFolders.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item {
                        HorizontalDivider(color = Color.White.copy(alpha = .08f), modifier = Modifier.padding(vertical = 2.dp))
                        Text("TOUTES LES APPLIS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }
                }

                items(filtered.chunked(4)) { rowApps ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowApps.forEach { app ->
                            HomeAppIcon(app, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                        }
                        repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    if (showFolderEditor) {
        FolderEditorDialog(
            existing = editingFolder,
            apps = apps,
            accent = accent,
            onDismiss = { showFolderEditor = false },
            onSave = { folder ->
                saveFolder(folder)
                showFolderEditor = false
            }
        )
    }

    openedFolder?.let { folder ->
        FolderContentsDialog(
            folder = folder,
            apps = apps,
            accent = accent,
            onDismiss = { openedFolder = null },
            onOpenApp = { launcher.openByPackage(it.packageName) },
            onEdit = {
                editingFolder = folder
                openedFolder = null
                showFolderEditor = true
            },
            onDelete = {
                deleteFolder(folder)
                openedFolder = null
            }
        )
    }
}

@Composable
private fun FolderTile(folder: AppFolder, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = .14f),
            border = BorderStroke(1.dp, accent.copy(alpha = .35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = accent, modifier = Modifier.size(35.dp))
                Text(
                    folder.packages.size.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FolderEditorDialog(
    existing: AppFolder?,
    apps: List<InstalledApp>,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (AppFolder) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    val selected = remember(existing?.id) {
        mutableStateListOf<String>().apply { addAll(existing?.packages.orEmpty()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nouveau dossier" else "Modifier le dossier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nom du dossier") },
                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = accent) }
                )
                Spacer(Modifier.height(10.dp))
                Text("Choisis les applications", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(apps) { app ->
                        val checked = app.packageName in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (checked) selected.remove(app.packageName) else selected.add(app.packageName)
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniAppIcon(app)
                            Spacer(Modifier.width(10.dp))
                            Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        AppFolder(
                            id = existing?.id ?: System.currentTimeMillis().toString(),
                            name = name.trim(),
                            packages = selected.toList()
                        )
                    )
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun FolderContentsDialog(
    folder: AppFolder,
    apps: List<InstalledApp>,
    accent: Color,
    onDismiss: () -> Unit,
    onOpenApp: (InstalledApp) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val folderApps = remember(folder, apps) { folder.packages.mapNotNull { appsByPackage[it] } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = accent)
                Spacer(Modifier.width(9.dp))
                Text(folder.name)
            }
        },
        text = {
            if (folderApps.isEmpty()) {
                Text("Ce dossier est vide. Appuie sur Modifier pour ajouter des applications.")
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(folderApps) { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenApp(app) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniAppIcon(app)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ChevronRight, null, tint = accent)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Modifier") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Fermer") }
            }
        }
    )
}

@Composable
private fun MiniAppIcon(app: InstalledApp) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(10.dp), color = Color(0x332A3A50)) {
        if (icon != null) {
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                update = { it.setImageDrawable(icon) },
                modifier = Modifier.padding(3.dp)
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}
