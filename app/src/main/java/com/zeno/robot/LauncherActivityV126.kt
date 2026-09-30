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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class LauncherActivityV126 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoZenHome() }
    }
}

private enum class ZenPage { HOME, APPS, CUSTOMIZE }

@Composable
private fun ZenoZenHome() {
    val context = LocalContext.current
    val prefs = remember { LauncherPreferences(context.applicationContext) }
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(ZenPage.HOME) }
    var settings by remember { mutableStateOf(prefs.loadHomeSettings()) }

    fun save(value: HomeSettings) {
        settings = value
        prefs.saveHomeSettings(value)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = Color(0xFF02040A),
            surface = Color(0xFF0A1220)
        )
    ) {
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(296.dp),
                    drawerContainerColor = Color(0xF5060B14),
                    drawerContentColor = Color.White
                ) {
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text("HOME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    ZenDrawerItem(Icons.Default.Home, "Accueil", page == ZenPage.HOME) {
                        page = ZenPage.HOME
                        scope.launch { drawer.close() }
                    }
                    ZenDrawerItem(Icons.Default.Apps, "Tiroir d'applications", page == ZenPage.APPS) {
                        page = ZenPage.APPS
                        scope.launch { drawer.close() }
                    }
                    ZenDrawerItem(Icons.Default.Tune, "Personnaliser l'accueil", page == ZenPage.CUSTOMIZE) {
                        page = ZenPage.CUSTOMIZE
                        scope.launch { drawer.close() }
                    }
                    ZenDrawerItem(Icons.Default.Mic, "Parler à Zeno", false) {
                        context.startActivity(Intent(context, VoiceCommandActivity::class.java))
                        scope.launch { drawer.close() }
                    }
                    ZenDrawerItem(Icons.Default.Settings, "Réglages Zeno", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawer.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Zeno ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .35f), fontSize = 11.sp, modifier = Modifier.padding(20.dp))
                }
            }
        ) {
            when (page) {
                ZenPage.HOME -> ZenHomeScreen(
                    accent = accent,
                    settings = settings,
                    onSettingsChange = ::save,
                    openMenu = { scope.launch { drawer.open() } },
                    openApps = { page = ZenPage.APPS },
                    customize = { page = ZenPage.CUSTOMIZE },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) }
                )
                ZenPage.APPS -> ZenAppsScreen(
                    accent = accent,
                    prefs = prefs,
                    openMenu = { scope.launch { drawer.open() } },
                    back = { page = ZenPage.HOME }
                )
                ZenPage.CUSTOMIZE -> ZenCustomize(
                    accent = accent,
                    settings = settings,
                    onChange = ::save,
                    back = { page = ZenPage.HOME }
                )
            }
        }
    }
}

@Composable
private fun ZenDrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        icon = { Icon(icon, null) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
    )
}

@Composable
private fun ZenHomeScreen(
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
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val homeApps = remember(apps, settings.homePackages) { settings.homePackages.mapNotNull { appMap[it] } }
    val displayed = remember(homeApps, apps, query) {
        if (query.isBlank()) homeApps else apps.filter { it.label.contains(query, true) }.take(8)
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF02040A), Color(0xFF0A1B31), Color(0xFF071225), Color(0xFF02040A))
            )
        )
    ) {
        if (settings.showRobot) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(510.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size((settings.robotSize + 35).dp)
                        .alpha(.20f)
                        .background(
                            Brush.radialGradient(listOf(accent.copy(alpha = .8f), Color(0xFF785BFF).copy(alpha = .35f), Color.Transparent)),
                            CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.zeno_robot),
                    contentDescription = "Zeno",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(settings.robotSize.dp)
                        .clickable(onClick = speak)
                )
            }
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x88000000),
                    .18f to Color(0x22000000),
                    .60f to Color.Transparent,
                    1f to Color(0x99000000)
                )
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
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
                            color = Color.White.copy(alpha = .94f),
                            fontSize = 15.sp
                        )
                    }
                }
                IconButton(onClick = customize) { Icon(Icons.Default.Tune, "Personnaliser", tint = Color.White) }
            }

            Spacer(Modifier.height(if (settings.showRobot) (settings.robotSize * .55f).dp else 42.dp))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .85f)) },
                    trailingIcon = { IconButton(onClick = speak) { Icon(Icons.Default.Mic, null, tint = accent) } },
                    placeholder = { Text("Rechercher", color = Color.White.copy(alpha = .55f)) },
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xA7101723),
                        unfocusedContainerColor = Color(0x96101723),
                        focusedBorderColor = Color.White.copy(alpha = .34f),
                        unfocusedBorderColor = Color.White.copy(alpha = .16f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            if (settings.showApps) {
                if (query.isBlank() && homeApps.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { showPicker = true }) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Ajouter des applis", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    displayed.take(8).chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { app ->
                                ZenAppIcon(app, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    if (query.isBlank()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            IconButton(onClick = { showPicker = true }) {
                                Icon(Icons.Default.AddCircle, "Ajouter", tint = Color.White.copy(alpha = .92f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (settings.showDock) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    color = Color(0x7A101724),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZenDockButton(Icons.Default.Phone) { launcher.openByName("téléphone") }
                        ZenDockButton(Icons.Default.ChatBubble) { context.startActivity(Intent(context, MainActivity::class.java)) }
                        ZenDockButton(Icons.Default.Apps, large = true, onClick = openApps)
                        ZenDockButton(Icons.Default.Public) { launcher.openByName("chrome") }
                        ZenDockButton(Icons.Default.CameraAlt) { launcher.openByName("appareil photo") }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ZenHomePicker(
            apps = apps,
            selected = settings.homePackages,
            accent = accent,
            onDismiss = { showPicker = false },
            onSave = {
                onSettingsChange(settings.copy(homePackages = it))
                showPicker = false
            }
        )
    }
}

@Composable
private fun ZenHomePicker(
    apps: List<InstalledApp>,
    selected: List<String>,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val picked = remember(selected) { mutableStateListOf<String>().apply { addAll(selected) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Applications de l'accueil") },
        text = {
            LazyColumn(Modifier.heightIn(max = 430.dp)) {
                items(apps) { app ->
                    val checked = app.packageName in picked
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (checked) picked.remove(app.packageName) else picked.add(app.packageName)
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZenMiniIcon(app)
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Checkbox(checked = checked, onCheckedChange = null)
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
private fun ZenAppIcon(app: InstalledApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) { runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull() }
    Column(modifier.clickable(onClick = onClick).padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (drawable != null) {
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                update = { it.setImageDrawable(drawable) },
                modifier = Modifier.size(58.dp)
            )
        } else {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(app.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ZenDockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, large: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(if (large) 58.dp else 50.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (large) 34.dp else 28.dp))
    }
}

@Composable
private fun ZenCustomize(accent: Color, settings: HomeSettings, onChange: (HomeSettings) -> Unit, back: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02040A), Color(0xFF07162B), Color(0xFF02040A))))) {
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
                        Text("ÉCRAN D'ACCUEIL", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { ZenSwitch("Horloge et date", settings.showClock) { onChange(settings.copy(showClock = it)) } }
            item { ZenSwitch("Robot Zeno", settings.showRobot) { onChange(settings.copy(showRobot = it)) } }
            if (settings.showRobot) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xA50A1422)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("Taille du robot", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(settings.robotSize.toString(), color = accent, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = settings.robotSize.toFloat().coerceIn(280f, 500f),
                                onValueChange = { onChange(settings.copy(robotSize = it.roundToInt().coerceIn(280, 500))) },
                                valueRange = 280f..500f
                            )
                        }
                    }
                }
            }
            item { ZenSwitch("Barre de recherche", settings.showSearch) { onChange(settings.copy(showSearch = it)) } }
            item { ZenSwitch("Applications choisies", settings.showApps) { onChange(settings.copy(showApps = it)) } }
            item { ZenSwitch("Dock du bas", settings.showDock) { onChange(settings.copy(showDock = it)) } }
            item {
                OutlinedButton(onClick = { onChange(settings.copy(homePackages = emptyList())) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteSweep, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Vider l'accueil")
                }
            }
        }
    }
}

@Composable
private fun ZenSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xA50A1422)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ZenAppsScreen(accent: Color, prefs: LauncherPreferences, openMenu: () -> Unit, back: () -> Unit) {
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

    val inFolders = remember(folders) { folders.flatMap { it.packages }.toSet() }
    val rootApps = remember(apps, folders, query) {
        val source = if (query.isBlank()) apps.filterNot { it.packageName in inFolders } else apps
        source.filter { query.isBlank() || it.label.contains(query, true) }
    }

    fun drop(source: InstalledApp) {
        val folderId = folderBounds.entries.firstOrNull { it.value.contains(dragPosition) }?.key
        if (folderId != null) {
            saveFolders(folders.map { folder ->
                if (folder.id == folderId && source.packageName !in folder.packages) folder.copy(packages = folder.packages + source.packageName) else folder
            })
            return
        }
        val targetPackage = appBounds.entries.firstOrNull { it.key != source.packageName && it.value.contains(dragPosition) }?.key
        if (targetPackage != null) {
            saveFolders(folders + AppFolder(System.currentTimeMillis().toString(), "Dossier", listOf(source.packageName, targetPackage)))
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF02040A), Color(0xFF07162B), Color(0xFF02040A))))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                Column(Modifier.weight(1f)) {
                    Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("Maintiens puis glisse pour créer un dossier", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                placeholder = { Text("Rechercher") },
                shape = RoundedCornerShape(26.dp)
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                if (query.isBlank() && folders.isNotEmpty()) {
                    item { Text("DOSSIERS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    items(folders.chunked(4)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { folder ->
                                ZenFolderIcon(
                                    folder,
                                    accent,
                                    Modifier.weight(1f).onGloballyPositioned { folderBounds[folder.id] = it.boundsInRoot() }
                                ) { openedFolder = folder }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item { HorizontalDivider(color = Color.White.copy(alpha = .08f)) }
                }

                items(rootApps.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { appBounds[app.packageName] = it.boundsInRoot() }
                                    .pointerInput(app.packageName) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { local ->
                                                dragging = app
                                                val rect = appBounds[app.packageName]
                                                dragPosition = if (rect != null) rect.topLeft + local else local
                                            },
                                            onDrag = { _, amount -> dragPosition += amount },
                                            onDragEnd = {
                                                drop(app)
                                                dragging = null
                                            },
                                            onDragCancel = { dragging = null }
                                        )
                                    }
                            ) {
                                ZenAppIcon(app, Modifier.fillMaxWidth()) { launcher.openByPackage(app.packageName) }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        dragging?.let { app ->
            Box(
                Modifier.offset { IntOffset((dragPosition.x - 30).roundToInt(), (dragPosition.y - 30).roundToInt()) }.size(60.dp).alpha(.82f),
                contentAlignment = Alignment.Center
            ) { ZenMiniIcon(app, 56) }
        }
    }

    openedFolder?.let { folder ->
        val map = remember(apps) { apps.associateBy { it.packageName } }
        val folderApps = folder.packages.mapNotNull { map[it] }
        AlertDialog(
            onDismissRequest = { openedFolder = null },
            title = { Text(folder.name) },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(folderApps) { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable { launcher.openByPackage(app.packageName) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ZenMiniIcon(app)
                            Spacer(Modifier.width(10.dp))
                            Text(app.label, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { openedFolder = null }) { Text("Fermer") } },
            dismissButton = {
                TextButton(onClick = {
                    saveFolders(folders.filterNot { it.id == folder.id })
                    openedFolder = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            }
        )
    }
}

@Composable
private fun ZenFolderIcon(folder: AppFolder, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Folder, null, tint = accent, modifier = Modifier.size(48.dp))
            Text(
                folder.packages.size.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xCC08111D), CircleShape).padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(folder.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ZenMiniIcon(app: InstalledApp, size: Int = 38) {
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
