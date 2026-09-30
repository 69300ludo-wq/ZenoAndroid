package com.zeno.robot

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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

class LauncherActivityV124 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoLauncherV124() }
    }
}

private enum class V124Page { HOME, APPS, CUSTOMIZE }

@Composable
private fun ZenoLauncherV124() {
    val context = LocalContext.current
    val prefs = remember { LauncherPreferences(context.applicationContext) }
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(V124Page.HOME) }
    var settings by remember { mutableStateOf(prefs.loadHomeSettings()) }

    fun saveSettings(value: HomeSettings) {
        settings = value
        prefs.saveHomeSettings(value)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            secondary = Color(0xFF8B6DFF),
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
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(62.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Text("GALAXY HOME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .45f), fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    V124DrawerItem(Icons.Default.Home, "Accueil", page == V124Page.HOME) {
                        page = V124Page.HOME
                        scope.launch { drawerState.close() }
                    }
                    V124DrawerItem(Icons.Default.Apps, "Applications et dossiers", page == V124Page.APPS) {
                        page = V124Page.APPS
                        scope.launch { drawerState.close() }
                    }
                    V124DrawerItem(Icons.Default.Tune, "Personnaliser l'accueil", page == V124Page.CUSTOMIZE) {
                        page = V124Page.CUSTOMIZE
                        scope.launch { drawerState.close() }
                    }
                    V124DrawerItem(Icons.Default.Mic, "Parler à Zeno", false) {
                        context.startActivity(Intent(context, VoiceCommandActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V124DrawerItem(Icons.Default.SmartToy, "Zeno flottant", false) {
                        context.startActivity(Intent(context, SetupActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V124DrawerItem(Icons.Default.Settings, "Zeno complet", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Accueil libre : rien n'est ajouté sans toi.",
                        color = Color.White.copy(alpha = .42f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        ) {
            when (page) {
                V124Page.HOME -> V124Home(
                    accent = accent,
                    settings = settings,
                    onSettingsChange = ::saveSettings,
                    openMenu = { scope.launch { drawerState.open() } },
                    openApps = { page = V124Page.APPS },
                    customize = { page = V124Page.CUSTOMIZE },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) }
                )
                V124Page.APPS -> V124Apps(
                    accent = accent,
                    prefs = prefs,
                    openMenu = { scope.launch { drawerState.open() } },
                    back = { page = V124Page.HOME }
                )
                V124Page.CUSTOMIZE -> V124Customize(
                    accent = accent,
                    settings = settings,
                    onChange = ::saveSettings,
                    back = { page = V124Page.HOME }
                )
            }
        }
    }
}

@Composable
private fun V124DrawerItem(
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
private fun V124Home(
    accent: Color,
    settings: HomeSettings,
    onSettingsChange: (HomeSettings) -> Unit,
    openMenu: () -> Unit,
    openApps: () -> Unit,
    customize: () -> Unit,
    speak: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val homeApps = remember(apps, settings.homePackages) {
        settings.homePackages.mapNotNull { byPackage[it] }
    }
    val visibleApps = remember(homeApps, apps, query) {
        if (query.isBlank()) homeApps
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(12)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF01020A), Color(0xFF07142D), Color(0xFF101038), Color(0xFF071226), Color(0xFF02040C))
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
            Image(
                painter = painterResource(R.drawable.zeno_robot),
                contentDescription = "Zeno",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 112.dp)
                    .size(settings.robotSize.dp)
                    .clickable(onClick = speak)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = openMenu) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
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
                            fontSize = 15.sp
                        )
                    }
                }
                IconButton(onClick = customize) {
                    Icon(Icons.Default.Tune, "Personnaliser", tint = Color.White)
                }
            }

            Spacer(Modifier.height(if (settings.showRobot) (settings.robotSize * .58f).dp else 42.dp))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .85f)) },
                    trailingIcon = { IconButton(onClick = speak) { Icon(Icons.Default.Mic, null, tint = accent) } },
                    placeholder = { Text("Rechercher une application...", color = Color.White.copy(alpha = .55f)) },
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xA5101830),
                        unfocusedContainerColor = Color(0x8F0B1324),
                        focusedBorderColor = accent.copy(alpha = .80f),
                        unfocusedBorderColor = accent.copy(alpha = .30f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            if (settings.showApps) {
                if (query.isBlank() && homeApps.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = accent)
                            Spacer(Modifier.width(8.dp))
                            Text("Ajouter des applis à l'accueil", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                visibleApps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            V124AppIcon(app, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (query.isBlank() && homeApps.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        IconButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.AddCircle, "Ajouter à l'accueil", tint = accent)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (settings.showDock) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V124DockButton(Icons.Default.Phone, Color(0xFF45E6B5)) { launcher.openByName("téléphone") }
                    V124DockButton(Icons.Default.ChatBubble, Color(0xFF4FA3FF)) { context.startActivity(Intent(context, MainActivity::class.java)) }
                    V124DockButton(Icons.Default.Apps, Color.White, large = true, onClick = openApps)
                    V124DockButton(Icons.Default.Public, Color(0xFF50C8FF)) { launcher.openByName("chrome") }
                    V124DockButton(Icons.Default.CameraAlt, Color(0xFFB7C8E7)) { launcher.openByName("appareil photo") }
                }
            }
        }
    }

    if (showAdd) {
        V124HomeAppsDialog(
            apps = apps,
            selected = settings.homePackages,
            accent = accent,
            onDismiss = { showAdd = false },
            onSave = {
                onSettingsChange(settings.copy(homePackages = it, appCount = it.size.coerceAtLeast(4)))
                showAdd = false
            }
        )
    }
}

@Composable
private fun V124HomeAppsDialog(
    apps: List<InstalledApp>,
    selected: List<String>,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val picked = remember(selected) { mutableStateListOf<String>().apply { addAll(selected) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Applis de l'accueil") },
        text = {
            Column {
                Text("Choisis uniquement ce que tu veux afficher.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(apps) { app ->
                        val checked = app.packageName in picked
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (checked) picked.remove(app.packageName) else picked.add(app.packageName)
                            }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V124MiniIcon(app)
                            Spacer(Modifier.width(10.dp))
                            Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toList()) }) { Text("Enregistrer", color = accent) } },
        dismissButton = {
            Row {
                TextButton(onClick = { picked.clear() }) { Text("Tout retirer") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

@Composable
private fun V124AppIcon(app: InstalledApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
            Box(Modifier.size(58.dp).background(Color(0x332D7FFF), CircleShape), contentAlignment = Alignment.Center) {
                Text(app.label.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun V124DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    large: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(if (large) 58.dp else 52.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(if (large) 34.dp else 30.dp))
    }
}

@Composable
private fun V124Customize(
    accent: Color,
    settings: HomeSettings,
    onChange: (HomeSettings) -> Unit,
    back: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02050B), Color(0xFF071729), Color(0xFF02050B))))) {
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                    Column {
                        Text("PERSONNALISER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("ACCUEIL ZENO", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { V124Switch("Horloge et date", settings.showClock) { onChange(settings.copy(showClock = it)) } }
            item { V124Switch("Robot Zeno statique", settings.showRobot) { onChange(settings.copy(showRobot = it)) } }
            item { V124Switch("Barre de recherche", settings.showSearch) { onChange(settings.copy(showSearch = it)) } }
            item { V124Switch("Applications choisies", settings.showApps) { onChange(settings.copy(showApps = it)) } }
            item { V124Switch("Dock du bas", settings.showDock) { onChange(settings.copy(showDock = it)) } }
            if (settings.showRobot) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xA50A1422)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("Taille du robot", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(settings.robotSize.toString(), color = accent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = settings.robotSize.toFloat(),
                                onValueChange = { onChange(settings.copy(robotSize = it.roundToInt().coerceIn(300, 520))) },
                                valueRange = 300f..520f
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { onChange(settings.copy(homePackages = emptyList())) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vider les applis de l'accueil")
                }
            }
        }
    }
}

@Composable
private fun V124Switch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xA50A1422)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun V124Apps(
    accent: Color,
    prefs: LauncherPreferences,
    openMenu: () -> Unit,
    back: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var folders by remember { mutableStateOf(prefs.loadFolders()) }
    var query by remember { mutableStateOf("") }
    var openedFolder by remember { mutableStateOf<AppFolder?>(null) }
    var dragging by remember { mutableStateOf<InstalledApp?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val appBounds = remember { mutableStateMapOf<String, Rect>() }
    val folderBounds = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    fun saveFolders(value: List<AppFolder>) {
        folders = value
        prefs.saveFolders(value)
    }

    fun dropApp(source: InstalledApp) {
        val folderTarget = folderBounds.entries.firstOrNull { (_, rect) -> rect.contains(dragPosition) }?.key
        if (folderTarget != null) {
            val next = folders.map { folder ->
                if (folder.id == folderTarget && source.packageName !in folder.packages) {
                    folder.copy(packages = folder.packages + source.packageName)
                } else folder
            }
            saveFolders(next)
            return
        }

        val targetPackage = appBounds.entries.firstOrNull { (pkg, rect) -> pkg != source.packageName && rect.contains(dragPosition) }?.key
        if (targetPackage != null) {
            val target = apps.firstOrNull { it.packageName == targetPackage } ?: return
            val folder = AppFolder(
                id = System.currentTimeMillis().toString(),
                name = "Dossier",
                packages = listOf(target.packageName, source.packageName)
            )
            saveFolders(folders + folder)
        }
    }

    val packagesInFolders = remember(folders) { folders.flatMap { it.packages }.toSet() }
    val rootApps = remember(apps, folders, query) {
        apps.filter { it.packageName !in packagesInFolders && it.label.contains(query, ignoreCase = true) }
    }
    val shownFolders = remember(folders, query) {
        if (query.isBlank()) folders else folders.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02040B), Color(0xFF07162B), Color(0xFF02040B))))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                Column(Modifier.weight(1f)) {
                    Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("MAINTIENS + GLISSE POUR CRÉER UN DOSSIER", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                placeholder = { Text("Rechercher une application") },
                shape = RoundedCornerShape(28.dp)
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                if (shownFolders.isNotEmpty()) {
                    item { Text("DOSSIERS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    items(shownFolders.chunked(4)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { folder ->
                                V124FolderIcon(
                                    folder = folder,
                                    accent = accent,
                                    modifier = Modifier.weight(1f).onGloballyPositioned { folderBounds[folder.id] = it.boundsInRoot() },
                                    onClick = { openedFolder = folder }
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item { HorizontalDivider(color = Color.White.copy(alpha = .08f)) }
                }

                items(rootApps.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            V124DraggableApp(
                                app = app,
                                modifier = Modifier.weight(1f).onGloballyPositioned { appBounds[app.packageName] = it.boundsInRoot() },
                                onClick = { launcher.openByPackage(app.packageName) },
                                onDragStart = {
                                    dragging = app
                                    dragPosition = appBounds[app.packageName]?.center ?: Offset.Zero
                                },
                                onDrag = { delta -> dragPosition += delta },
                                onDragEnd = {
                                    dropApp(app)
                                    dragging = null
                                }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        dragging?.let { app ->
            Box(
                modifier = Modifier
                    .offset { IntOffset((dragPosition.x - 34f).roundToInt(), (dragPosition.y - 34f).roundToInt()) }
                    .size(68.dp)
                    .background(accent.copy(alpha = .18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                V124RawIcon(app, 56)
            }
        }
    }

    openedFolder?.let { folder ->
        V124FolderDialog(
            folder = folder,
            apps = apps,
            accent = accent,
            onDismiss = { openedFolder = null },
            onOpen = { launcher.openByPackage(it.packageName) },
            onRename = { newName ->
                saveFolders(folders.map { if (it.id == folder.id) it.copy(name = newName) else it })
                openedFolder = folders.firstOrNull { it.id == folder.id }?.copy(name = newName)
            },
            onRemoveApp = { pkg ->
                val next = folders.mapNotNull { f ->
                    if (f.id != folder.id) f
                    else {
                        val remaining = f.packages.filterNot { it == pkg }
                        if (remaining.size < 2) null else f.copy(packages = remaining)
                    }
                }
                saveFolders(next)
                openedFolder = next.firstOrNull { it.id == folder.id }
            },
            onDelete = {
                saveFolders(folders.filterNot { it.id == folder.id })
                openedFolder = null
            }
        )
    }
}

@Composable
private fun V124DraggableApp(
    app: InstalledApp,
    modifier: Modifier,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = modifier
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        V124RawIcon(app, 58)
        Spacer(Modifier.height(5.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun V124RawIcon(app: InstalledApp, size: Int) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    if (drawable != null) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = { it.setImageDrawable(drawable) },
            modifier = Modifier.size(size.dp)
        )
    } else {
        Box(Modifier.size(size.dp).background(Color(0x332D7FFF), CircleShape), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V124FolderIcon(folder: AppFolder, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Folder, null, tint = accent, modifier = Modifier.size(50.dp))
            Text(
                folder.packages.size.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xCC09111F), CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V124FolderDialog(
    folder: AppFolder,
    apps: List<InstalledApp>,
    accent: Color,
    onDismiss: () -> Unit,
    onOpen: (InstalledApp) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDelete: () -> Unit
) {
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val folderApps = folder.packages.mapNotNull { byPackage[it] }
    var name by remember(folder.id, folder.name) { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(28) },
                singleLine = true,
                label = { Text("Nom du dossier") },
                trailingIcon = {
                    IconButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) }) {
                        Icon(Icons.Default.Check, null, tint = accent)
                    }
                }
            )
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(folderApps) { app ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable { onOpen(app) }) { V124MiniIcon(app) }
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onRemoveApp(app.packageName) }) {
                            Icon(Icons.Default.RemoveCircleOutline, "Retirer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Supprimer le dossier", color = MaterialTheme.colorScheme.error) } }
    )
}

@Composable
private fun V124MiniIcon(app: InstalledApp) {
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
