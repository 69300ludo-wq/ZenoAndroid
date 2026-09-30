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

class LauncherActivityV123 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoLauncherV123() }
    }
}

private enum class V123Page { HOME, APPS, CUSTOMIZE }

@Composable
private fun ZenoLauncherV123() {
    val context = LocalContext.current
    val preferences = remember { LauncherPreferences(context.applicationContext) }
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(V123Page.HOME) }
    var settings by remember { mutableStateOf(preferences.loadHomeSettings()) }

    fun saveSettings(newValue: HomeSettings) {
        settings = newValue
        preferences.saveHomeSettings(newValue)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            secondary = Color(0xFF9B6CFF),
            background = Color(0xFF01030A),
            surface = Color(0xFF07162B)
        )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(304.dp),
                    drawerContainerColor = Color(0xF2050A16),
                    drawerContentColor = Color.White
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(62.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 2.6.sp)
                            Text("GALAXY HOME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                            Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .48f), fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    V123DrawerItem(Icons.Default.Home, "Accueil", page == V123Page.HOME) {
                        page = V123Page.HOME
                        scope.launch { drawerState.close() }
                    }
                    V123DrawerItem(Icons.Default.Apps, "Applications et dossiers", page == V123Page.APPS) {
                        page = V123Page.APPS
                        scope.launch { drawerState.close() }
                    }
                    V123DrawerItem(Icons.Default.Tune, "Personnaliser l'accueil", page == V123Page.CUSTOMIZE) {
                        page = V123Page.CUSTOMIZE
                        scope.launch { drawerState.close() }
                    }
                    V123DrawerItem(Icons.Default.Mic, "Parler à Zeno", false) {
                        context.startActivity(Intent(context, VoiceCommandActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V123DrawerItem(Icons.Default.SmartToy, "Zeno flottant", false) {
                        context.startActivity(Intent(context, SetupActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V123DrawerItem(Icons.Default.Settings, "Zeno complet", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Appuie sur le robot pour lui parler.",
                        color = Color.White.copy(alpha = .45f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        ) {
            when (page) {
                V123Page.HOME -> V123Home(
                    accent = accent,
                    settings = settings,
                    openMenu = { scope.launch { drawerState.open() } },
                    openApps = { page = V123Page.APPS },
                    customize = { page = V123Page.CUSTOMIZE },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) },
                    openZeno = { context.startActivity(Intent(context, MainActivity::class.java)) }
                )
                V123Page.APPS -> V123Apps(
                    accent = accent,
                    preferences = preferences,
                    openMenu = { scope.launch { drawerState.open() } },
                    back = { page = V123Page.HOME }
                )
                V123Page.CUSTOMIZE -> V123Customize(
                    accent = accent,
                    settings = settings,
                    onChange = ::saveSettings,
                    back = { page = V123Page.HOME }
                )
            }
        }
    }
}

@Composable
private fun V123DrawerItem(
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
private fun V123Home(
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

    val life = rememberInfiniteTransition(label = "zenoLife")
    val scale by life.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(2300), RepeatMode.Reverse),
        label = "scale"
    )
    val floatY by life.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "float"
    )
    val tilt by life.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "tilt"
    )
    val glow by life.animateFloat(
        initialValue = .16f,
        targetValue = .34f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF01020A),
                        Color(0xFF07142D),
                        Color(0xFF101038),
                        Color(0xFF071226),
                        Color(0xFF02040C)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(Color(0x3321C9FF), Color(0x225744FF), Color.Transparent),
                    radius = 1100f
                )
            )
    ) {
        if (settings.showRobot) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(610.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size((settings.robotSize + 30).dp)
                        .alpha(glow)
                        .background(
                            Brush.radialGradient(
                                listOf(accent, Color(0xFF7B4DFF), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.zeno_robot),
                    contentDescription = "Zeno",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(settings.robotSize.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationY = floatY,
                            rotationZ = tilt
                        )
                        .clickable(onClick = speak)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = openMenu, modifier = Modifier.padding(top = 3.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White.copy(alpha = .92f))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (settings.showClock) {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            now.format(DateTimeFormatter.ofPattern("EEE. d MMMM", Locale.FRENCH)).replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = .92f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = customize) {
                        Icon(Icons.Default.Tune, contentDescription = "Personnaliser", tint = Color.White.copy(alpha = .92f))
                    }
                    Text("● ZENO PRÊT", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(if (settings.showRobot) (settings.robotSize * .60f).dp else 48.dp))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .9f)) },
                    trailingIcon = {
                        IconButton(onClick = speak) { Icon(Icons.Default.Mic, null, tint = accent) }
                    },
                    placeholder = { Text("Rechercher une application...", color = Color.White.copy(alpha = .60f)) },
                    shape = RoundedCornerShape(32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xA5101830),
                        unfocusedContainerColor = Color(0x8F0B1324),
                        focusedBorderColor = accent.copy(alpha = .85f),
                        unfocusedBorderColor = accent.copy(alpha = .35f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(18.dp))
            }

            if (settings.showApps) {
                shownApps.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { app ->
                            V123AppIcon(app, Modifier.weight(1f)) {
                                launcher.openByPackage(app.packageName)
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            if (settings.showDock) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V123DockButton(Icons.Default.Phone, Color(0xFF45E6B5)) { launcher.openByName("téléphone") }
                    V123DockButton(Icons.Default.ChatBubble, Color(0xFF4FA3FF), openZeno)
                    V123DockButton(Icons.Default.Apps, Color.White, openApps, large = true)
                    V123DockButton(Icons.Default.Public, Color(0xFF50C8FF)) { launcher.openByName("chrome") }
                    V123DockButton(Icons.Default.CameraAlt, Color(0xFFB7C8E7)) { launcher.openByName("appareil photo") }
                }
            }
        }
    }
}

@Composable
private fun V123AppIcon(app: InstalledApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (drawable != null) {
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                update = { it.setImageDrawable(drawable) },
                modifier = Modifier.size(58.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(58.dp).background(Color(0x332D7FFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(app.label.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun V123DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    large: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(if (large) 58.dp else 52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (large) 34.dp else 30.dp)
        )
    }
}

@Composable
private fun V123Customize(
    accent: Color,
    settings: HomeSettings,
    onChange: (HomeSettings) -> Unit,
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
                        Text("PERSONNALISER L'ACCUEIL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        Text("ZENO GALAXY HOME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    }
                }
            }

            item { V123Switch("Horloge et date", settings.showClock) { onChange(settings.copy(showClock = it)) } }
            item { V123Switch("Robot Zeno vivant", settings.showRobot) { onChange(settings.copy(showRobot = it)) } }
            item { V123Switch("Barre de recherche", settings.showSearch) { onChange(settings.copy(showSearch = it)) } }
            item { V123Switch("Applications sur l'accueil", settings.showApps) { onChange(settings.copy(showApps = it)) } }
            item { V123Switch("Dock du bas", settings.showDock) { onChange(settings.copy(showDock = it)) } }

            if (settings.showRobot) {
                item {
                    V123Slider(
                        title = "Taille du robot",
                        value = settings.robotSize.toFloat(),
                        range = 300f..520f,
                        label = "${settings.robotSize}",
                        onValueChange = { onChange(settings.copy(robotSize = it.roundToInt())) }
                    )
                }
            }

            if (settings.showApps) {
                item {
                    V123Slider(
                        title = "Nombre d'applications",
                        value = settings.appCount.toFloat(),
                        range = 4f..12f,
                        steps = 7,
                        label = settings.appCount.toString(),
                        onValueChange = {
                            val count = it.roundToInt().coerceIn(4, 12)
                            onChange(settings.copy(appCount = count, homePackages = settings.homePackages.take(count)))
                        }
                    )
                }
                item {
                    Text("CHOISIR LES APPLIS DE L'ACCUEIL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("${settings.homePackages.size}/${settings.appCount} sélectionnées", color = accent, fontSize = 11.sp)
                }
                items(apps) { app ->
                    val checked = app.packageName in settings.homePackages
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xA50A1422),
                        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = .50f) else Color.White.copy(alpha = .06f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                val next = settings.homePackages.toMutableList()
                                if (checked) next.remove(app.packageName)
                                else if (next.size < settings.appCount) next.add(app.packageName)
                                onChange(settings.copy(homePackages = next))
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V123MiniIcon(app)
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
private fun V123Switch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA50A1422),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun V123Slider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xA50A1422),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
        }
    }
}

@Composable
private fun V123Apps(
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
    var openedFolder by remember { mutableStateOf<AppFolder?>(null) }
    var editFolder by remember { mutableStateOf<AppFolder?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    val filtered = remember(apps, query) { apps.filter { it.label.contains(query, ignoreCase = true) } }
    val visibleFolders = remember(folders, query) {
        if (query.isBlank()) folders else folders.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF02040B), Color(0xFF07162B), Color(0xFF02040B)))
        )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                Column(Modifier.weight(1f)) {
                    Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("DOSSIERS PERSONNALISÉS", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                IconButton(onClick = {
                    editFolder = null
                    showEditor = true
                }) { Icon(Icons.Default.CreateNewFolder, "Créer un dossier", tint = accent) }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                placeholder = { Text("Rechercher une application ou un dossier") },
                shape = RoundedCornerShape(28.dp)
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                if (visibleFolders.isNotEmpty()) {
                    item { Text("MES DOSSIERS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    items(visibleFolders.chunked(4)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { folder ->
                                V123FolderIcon(folder, accent, Modifier.weight(1f)) { openedFolder = folder }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item {
                        HorizontalDivider(color = Color.White.copy(alpha = .08f))
                        Text("TOUTES LES APPLIS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                items(filtered.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            V123AppIcon(app, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    if (showEditor) {
        V123FolderEditor(
            existing = editFolder,
            apps = apps,
            accent = accent,
            onDismiss = { showEditor = false },
            onSave = { folder ->
                val next = folders.toMutableList()
                val index = next.indexOfFirst { it.id == folder.id }
                if (index >= 0) next[index] = folder else next.add(folder)
                folders = next
                preferences.saveFolders(next)
                showEditor = false
            }
        )
    }

    openedFolder?.let { folder ->
        V123FolderContents(
            folder = folder,
            apps = apps,
            accent = accent,
            onDismiss = { openedFolder = null },
            onOpen = { launcher.openByPackage(it.packageName) },
            onEdit = {
                editFolder = folder
                openedFolder = null
                showEditor = true
            },
            onDelete = {
                folders = folders.filterNot { it.id == folder.id }
                preferences.saveFolders(folders)
                openedFolder = null
            }
        )
    }
}

@Composable
private fun V123FolderIcon(folder: AppFolder, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Folder, null, tint = accent, modifier = Modifier.size(48.dp))
            Text(
                folder.packages.size.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xCC0A1220), CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun V123FolderEditor(
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
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(apps) { app ->
                        val checked = app.packageName in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (checked) selected.remove(app.packageName) else selected.add(app.packageName)
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V123MiniIcon(app)
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
private fun V123FolderContents(
    folder: AppFolder,
    apps: List<InstalledApp>,
    accent: Color,
    onDismiss: () -> Unit,
    onOpen: (InstalledApp) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val folderApps = remember(folder, apps) { folder.packages.mapNotNull { byPackage[it] } }

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
                Text("Ce dossier est vide.")
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(folderApps) { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(app) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V123MiniIcon(app)
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
private fun V123MiniIcon(app: InstalledApp) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    if (icon != null) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = { it.setImageDrawable(icon) },
            modifier = Modifier.size(38.dp)
        )
    } else {
        Box(Modifier.size(38.dp).background(Color(0x332D7FFF), CircleShape), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
    }
}
