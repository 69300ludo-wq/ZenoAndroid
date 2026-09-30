@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.zeno.robot

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.alpha
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
    var page by remember { mutableStateOf(V124Page.HOME) }
    var settings by remember { mutableStateOf(prefs.loadHomeSettings()) }

    fun save(value: HomeSettings) {
        settings = value
        prefs.saveHomeSettings(value)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = Color(0xFF05070B),
            surface = Color(0xFF101318),
            surfaceVariant = Color(0xFF1A1E24)
        )
    ) {
        when (page) {
            V124Page.HOME -> V124Home(
                accent = accent,
                settings = settings,
                onSettingsChange = ::save,
                openApps = { page = V124Page.APPS },
                openCustomize = { page = V124Page.CUSTOMIZE }
            )
            V124Page.APPS -> V124Apps(
                accent = accent,
                settings = settings,
                onSettingsChange = ::save,
                prefs = prefs,
                back = { page = V124Page.HOME },
                openCustomize = { page = V124Page.CUSTOMIZE }
            )
            V124Page.CUSTOMIZE -> V124Customize(
                accent = accent,
                settings = settings,
                onChange = ::save,
                back = { page = V124Page.HOME }
            )
        }
    }
}

@Composable
private fun V124Home(
    accent: Color,
    settings: HomeSettings,
    onSettingsChange: (HomeSettings) -> Unit,
    openApps: () -> Unit,
    openCustomize: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var query by remember { mutableStateOf("") }
    var showManage by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val homeApps = remember(apps, settings.homePackages) {
        settings.homePackages.mapNotNull { appMap[it] }
    }
    val visibleApps = remember(homeApps, apps, query, settings.hiddenPackages) {
        if (query.isBlank()) homeApps
        else apps.filter { it.packageName !in settings.hiddenPackages && it.label.contains(query, true) }.take(20)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF030509), Color(0xFF0A1420), Color(0xFF060A10), Color(0xFF020305))
                )
            )
            .combinedClickable(onClick = {}, onLongClick = { showManage = true })
    ) {
        if (settings.showRobot) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 112.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size((settings.robotSize + 28).dp)
                        .alpha(.12f)
                        .background(
                            Brush.radialGradient(listOf(accent.copy(alpha = .75f), Color.Transparent)),
                            CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.zeno_robot),
                    contentDescription = "Zeno",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(settings.robotSize.dp)
                        .combinedClickable(
                            onClick = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) },
                            onLongClick = { showManage = true }
                        )
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box {
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
                    }
                    DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Toutes les applications") },
                            leadingIcon = { Icon(Icons.Default.Apps, null) },
                            onClick = { showTopMenu = false; openApps() }
                        )
                        DropdownMenuItem(
                            text = { Text("Gérer l'accueil") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showTopMenu = false; showManage = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showTopMenu = false; openCustomize() }
                        )
                    }
                }

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (settings.showClock) {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)).replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = .90f),
                            fontSize = 14.sp
                        )
                    }
                }

                IconButton(onClick = openCustomize) {
                    Icon(Icons.Default.Settings, "Paramètres", tint = Color.White)
                }
            }

            Spacer(Modifier.height(if (settings.showRobot) 300.dp else 54.dp))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .80f)) },
                    trailingIcon = {
                        IconButton(onClick = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) }) {
                            Icon(Icons.Default.Mic, null, tint = Color.White.copy(alpha = .85f))
                        }
                    },
                    placeholder = { Text("Recherche", color = Color.White.copy(alpha = .55f)) },
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xB516191E),
                        unfocusedContainerColor = Color(0xA514171C),
                        focusedBorderColor = Color.White.copy(alpha = .34f),
                        unfocusedBorderColor = Color.White.copy(alpha = .16f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
            }

            if (settings.showApps) {
                if (query.isBlank() && homeApps.isEmpty()) {
                    TextButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter des applications", color = Color.White)
                    }
                } else {
                    visibleApps.take(settings.homeColumns * 3).chunked(settings.homeColumns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { app ->
                                V124AppIcon(
                                    app = app,
                                    modifier = Modifier.weight(1f),
                                    size = settings.iconSize,
                                    showLabel = settings.showLabels,
                                    onClick = { launcher.openByPackage(app.packageName) },
                                    onLongClick = {
                                        if (query.isBlank()) {
                                            onSettingsChange(settings.copy(homePackages = settings.homePackages.filterNot { it == app.packageName }))
                                        }
                                    }
                                )
                            }
                            repeat(settings.homeColumns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (settings.showDock) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = Color(0x6613171D),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V124DockButton(Icons.Default.Phone) { launcher.openByName("téléphone") }
                        V124DockButton(Icons.Default.Message) { launcher.openByName("messages") }
                        V124DockButton(Icons.Default.Apps, large = true, onClick = openApps)
                        V124DockButton(Icons.Default.Public) { launcher.openByName("chrome") }
                        V124DockButton(Icons.Default.CameraAlt) { launcher.openByName("appareil photo") }
                    }
                }
            }
        }
    }

    if (showManage) {
        ModalBottomSheet(onDismissRequest = { showManage = false }, containerColor = Color(0xFF16191E)) {
            Text("Gérer l'accueil", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Text("Appui long sur une zone vide pour retrouver ce menu.", color = Color.White.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                V124ManageAction(Icons.Default.Wallpaper, "Fonds\nd'écran") {
                    showManage = false
                    runCatching { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
                }
                V124ManageAction(Icons.Default.Widgets, "Widgets") { showManage = false; showWidgets = true }
                V124ManageAction(Icons.Default.Palette, "Packs\nd'icônes") {
                    showManage = false
                    context.startActivity(Intent(context, MainActivity::class.java))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                V124ManageAction(Icons.Default.Home, "Écran\nd'accueil") { showManage = false; openCustomize() }
                V124ManageAction(Icons.Default.Apps, "Applications") { showManage = false; showPicker = true }
                V124ManageAction(Icons.Default.Settings, "Préférences") { showManage = false; openCustomize() }
            }
            Spacer(Modifier.height(26.dp))
        }
    }

    if (showWidgets) {
        AlertDialog(
            onDismissRequest = { showWidgets = false },
            title = { Text("Widgets Zeno") },
            text = {
                Column {
                    V124DialogSwitch("Horloge et date", settings.showClock) { onSettingsChange(settings.copy(showClock = it)) }
                    V124DialogSwitch("Barre de recherche", settings.showSearch) { onSettingsChange(settings.copy(showSearch = it)) }
                    V124DialogSwitch("Robot Zeno", settings.showRobot) { onSettingsChange(settings.copy(showRobot = it)) }
                }
            },
            confirmButton = { TextButton(onClick = { showWidgets = false }) { Text("Fermer") } }
        )
    }

    if (showPicker) {
        V124HomeAppsDialog(
            apps = apps.filter { it.packageName !in settings.hiddenPackages },
            selected = settings.homePackages,
            accent = accent,
            onDismiss = { showPicker = false },
            onSave = {
                onSettingsChange(settings.copy(homePackages = it.take(20), appCount = it.size.coerceIn(4, 20)))
                showPicker = false
            }
        )
    }
}

@Composable
private fun V124ManageAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(96.dp).combinedClickable(onClick = onClick, onLongClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = Color(0xFF272B31), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun V124DialogSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
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
        title = { Text("Ajouter à l'écran d'accueil") },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(apps) { app ->
                    val checked = app.packageName in picked
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = { if (checked) picked.remove(app.packageName) else if (picked.size < 20) picked.add(app.packageName) },
                            onLongClick = { if (checked) picked.remove(app.packageName) else if (picked.size < 20) picked.add(app.packageName) }
                        ).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V124MiniIcon(app)
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toList()) }) { Text("OK", color = accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun V124AppIcon(
    app: InstalledApp,
    modifier: Modifier,
    size: Int,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        V124RawIcon(app, size)
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun V124DockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, large: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(if (large) 58.dp else 50.dp).combinedClickable(onClick = onClick, onLongClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (large) 34.dp else 28.dp))
    }
}

@Composable
private fun V124Customize(
    accent: Color,
    settings: HomeSettings,
    onChange: (HomeSettings) -> Unit,
    back: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0D11))) {
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White) }
                    Column {
                        Text("Paramètres écran d'accueil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Style ZenUI", color = accent, fontSize = 11.sp)
                    }
                }
            }
            item { V124SettingSwitch("Horloge et date", settings.showClock) { onChange(settings.copy(showClock = it)) } }
            item { V124SettingSwitch("Robot Zeno", settings.showRobot) { onChange(settings.copy(showRobot = it)) } }
            item { V124SettingSwitch("Barre de recherche", settings.showSearch) { onChange(settings.copy(showSearch = it)) } }
            item { V124SettingSwitch("Applications sur l'accueil", settings.showApps) { onChange(settings.copy(showApps = it)) } }
            item { V124SettingSwitch("Dock", settings.showDock) { onChange(settings.copy(showDock = it)) } }
            item { V124SettingSwitch("Afficher le nom des applications", settings.showLabels) { onChange(settings.copy(showLabels = it)) } }

            if (settings.showRobot) {
                item {
                    V124SettingSlider("Taille du robot", settings.robotSize.toFloat(), 280f..520f, settings.robotSize.toString()) {
                        onChange(settings.copy(robotSize = it.roundToInt().coerceIn(280, 520)))
                    }
                }
            }
            item {
                V124SettingSlider("Taille des icônes", settings.iconSize.toFloat(), 46f..72f, "${settings.iconSize} dp") {
                    onChange(settings.copy(iconSize = it.roundToInt().coerceIn(46, 72)))
                }
            }
            item {
                V124ColumnChooser("Grille de l'accueil", settings.homeColumns) { onChange(settings.copy(homeColumns = it)) }
            }
            item {
                V124ColumnChooser("Grille de toutes les applications", settings.drawerColumns) { onChange(settings.copy(drawerColumns = it)) }
            }
            item {
                OutlinedButton(onClick = { onChange(settings.copy(homePackages = emptyList())) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vider l'écran d'accueil")
                }
            }
        }
    }
}

@Composable
private fun V124SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF15191E)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun V124SettingSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, label: String, onChange: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF15191E)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, color = Color.White, modifier = Modifier.weight(1f))
                Text(label, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = value, onValueChange = onChange, valueRange = range)
        }
    }
}

@Composable
private fun V124ColumnChooser(title: String, value: Int, onChange: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF15191E)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5).forEach { count ->
                    FilterChip(selected = value == count, onClick = { onChange(count) }, label = { Text("${count}×") })
                }
            }
        }
    }
}

@Composable
private fun V124Apps(
    accent: Color,
    settings: HomeSettings,
    onSettingsChange: (HomeSettings) -> Unit,
    prefs: LauncherPreferences,
    back: () -> Unit,
    openCustomize: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var folders by remember { mutableStateOf(prefs.loadFolders()) }
    var query by remember { mutableStateOf("") }
    var openedFolder by remember { mutableStateOf<AppFolder?>(null) }
    var dragging by remember { mutableStateOf<InstalledApp?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var showMenu by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var alphabetical by remember { mutableStateOf(true) }
    val appBounds = remember { mutableStateMapOf<String, Rect>() }
    val folderBounds = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    fun saveFolders(value: List<AppFolder>) {
        folders = value
        prefs.saveFolders(value)
    }

    val inFolders = remember(folders) { folders.flatMap { it.packages }.toSet() }
    val drawerApps = remember(apps, folders, query, settings.hiddenPackages, alphabetical) {
        val base = apps.filter { app ->
            app.packageName !in settings.hiddenPackages &&
                (query.isNotBlank() || app.packageName !in inFolders) &&
                (query.isBlank() || app.label.contains(query, true))
        }
        if (alphabetical) base.sortedBy { it.label.lowercase(Locale.getDefault()) } else base
    }
    val shownFolders = remember(folders, query) {
        if (query.isBlank()) folders else folders.filter { it.name.contains(query, true) }
    }

    fun dropApp(source: InstalledApp) {
        val folderTarget = folderBounds.entries.firstOrNull { it.value.contains(dragPosition) }?.key
        if (folderTarget != null) {
            saveFolders(folders.map { folder ->
                if (folder.id == folderTarget && source.packageName !in folder.packages) folder.copy(packages = folder.packages + source.packageName)
                else folder
            })
            return
        }
        val targetPackage = appBounds.entries.firstOrNull { it.key != source.packageName && it.value.contains(dragPosition) }?.key
        if (targetPackage != null) {
            val target = apps.firstOrNull { it.packageName == targetPackage } ?: return
            val unique = listOf(target.packageName, source.packageName).distinct()
            if (unique.size == 2) {
                saveFolders(folders + AppFolder(System.currentTimeMillis().toString(), "Dossier", unique))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF090C10))) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White) }
                Text("Toutes les applications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Options", tint = Color.White) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (alphabetical) "Ordre d'installation" else "Trier A-Z") },
                            leadingIcon = { Icon(Icons.Default.SortByAlpha, null) },
                            onClick = { alphabetical = !alphabetical; showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Masquer des applications") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                            onClick = { showMenu = false; showHidden = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; openCustomize() }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .75f)) },
                placeholder = { Text("Rechercher une application") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (shownFolders.isNotEmpty()) {
                    item { Text("DOSSIERS", color = Color.White.copy(alpha = .65f), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    items(shownFolders.chunked(settings.drawerColumns)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { folder ->
                                V124FolderIcon(
                                    folder = folder,
                                    accent = accent,
                                    modifier = Modifier.weight(1f).onGloballyPositioned { folderBounds[folder.id] = it.boundsInRoot() },
                                    showLabel = settings.showLabels,
                                    onClick = { openedFolder = folder }
                                )
                            }
                            repeat(settings.drawerColumns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item { HorizontalDivider(color = Color.White.copy(alpha = .08f)) }
                }

                items(drawerApps.chunked(settings.drawerColumns)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            V124DraggableApp(
                                app = app,
                                size = settings.iconSize,
                                showLabel = settings.showLabels,
                                modifier = Modifier.weight(1f).onGloballyPositioned { appBounds[app.packageName] = it.boundsInRoot() },
                                onClick = { launcher.openByPackage(app.packageName) },
                                onDragStart = {
                                    dragging = app
                                    dragPosition = appBounds[app.packageName]?.center ?: Offset.Zero
                                },
                                onDrag = { dragPosition += it },
                                onDragEnd = { dropApp(app); dragging = null }
                            )
                        }
                        repeat(settings.drawerColumns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        dragging?.let { app ->
            Box(
                Modifier
                    .offset { IntOffset((dragPosition.x - 32f).roundToInt(), (dragPosition.y - 32f).roundToInt()) }
                    .size(64.dp)
                    .alpha(.88f),
                contentAlignment = Alignment.Center
            ) { V124RawIcon(app, 58) }
        }
    }

    if (showHidden) {
        V124HiddenAppsDialog(
            apps = apps,
            hidden = settings.hiddenPackages,
            onDismiss = { showHidden = false },
            onSave = { onSettingsChange(settings.copy(hiddenPackages = it)); showHidden = false }
        )
    }

    openedFolder?.let { folder ->
        V124FolderDialog(
            folder = folder,
            apps = apps,
            accent = accent,
            onDismiss = { openedFolder = null },
            onOpen = { launcher.openByPackage(it.packageName) },
            onRename = { newName ->
                val next = folders.map { if (it.id == folder.id) it.copy(name = newName) else it }
                saveFolders(next)
                openedFolder = next.firstOrNull { it.id == folder.id }
            },
            onRemoveApp = { pkg ->
                val next = folders.mapNotNull { f ->
                    if (f.id != folder.id) f else {
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
private fun V124HiddenAppsDialog(apps: List<InstalledApp>, hidden: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val picked = remember(hidden) { mutableStateListOf<String>().apply { addAll(hidden) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masquer des applications") },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(apps) { app ->
                    val checked = app.packageName in picked
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = { if (checked) picked.remove(app.packageName) else picked.add(app.packageName) },
                            onLongClick = { if (checked) picked.remove(app.packageName) else picked.add(app.packageName) }
                        ).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V124MiniIcon(app)
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toList()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun V124DraggableApp(
    app: InstalledApp,
    size: Int,
    showLabel: Boolean,
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
                    onDrag = { change, amount -> change.consume(); onDrag(amount) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        V124RawIcon(app, size)
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun V124RawIcon(app: InstalledApp, size: Int) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) { runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull() }
    if (drawable != null) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = { it.setImageDrawable(drawable) },
            modifier = Modifier.size(size.dp)
        )
    } else {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V124FolderIcon(folder: AppFolder, accent: Color, modifier: Modifier, showLabel: Boolean, onClick: () -> Unit) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onClick).padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Folder, null, tint = accent.copy(alpha = .92f), modifier = Modifier.size(48.dp))
            Text(
                folder.packages.size.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xDD15191E), CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(folder.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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
    val map = remember(apps) { apps.associateBy { it.packageName } }
    val folderApps = folder.packages.mapNotNull { map[it] }
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
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(folderApps) { app ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.combinedClickable(onClick = { onOpen(app) }, onLongClick = { onOpen(app) })) { V124MiniIcon(app) }
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onRemoveApp(app.packageName) }) {
                            Icon(Icons.Default.RemoveCircleOutline, "Retirer")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) } }
    )
}

@Composable
private fun V124MiniIcon(app: InstalledApp) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) { runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull() }
    if (drawable != null) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = { it.setImageDrawable(drawable) },
            modifier = Modifier.size(38.dp)
        )
    } else {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold) }
    }
}
