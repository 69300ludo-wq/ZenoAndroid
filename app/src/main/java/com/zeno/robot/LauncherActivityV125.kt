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

class LauncherActivityV125 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoLauncherV125() }
    }
}

private enum class V125Page { HOME, APPS, CUSTOMIZE }

@Composable
private fun ZenoLauncherV125() {
    val context = LocalContext.current
    val prefs = remember { LauncherPreferences(context.applicationContext) }
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(V125Page.HOME) }
    var settings by remember { mutableStateOf(prefs.loadHomeSettings()) }

    fun saveSettings(value: HomeSettings) {
        settings = value
        prefs.saveHomeSettings(value)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            secondary = Color(0xFF886BFF),
            background = Color(0xFF010208),
            surface = Color(0xFF071326)
        )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color(0xF2050913),
                    drawerContentColor = Color.White
                ) {
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(58.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = 2.sp)
                            Text("GALAXY HOME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .45f), fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    V125DrawerItem(Icons.Default.Home, "Accueil", page == V125Page.HOME) {
                        page = V125Page.HOME
                        scope.launch { drawerState.close() }
                    }
                    V125DrawerItem(Icons.Default.Apps, "Tiroir d'applications", page == V125Page.APPS) {
                        page = V125Page.APPS
                        scope.launch { drawerState.close() }
                    }
                    V125DrawerItem(Icons.Default.Tune, "Personnaliser l'accueil", page == V125Page.CUSTOMIZE) {
                        page = V125Page.CUSTOMIZE
                        scope.launch { drawerState.close() }
                    }
                    V125DrawerItem(Icons.Default.Mic, "Parler à Zeno", false) {
                        context.startActivity(Intent(context, VoiceCommandActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V125DrawerItem(Icons.Default.SmartToy, "Zeno flottant", false) {
                        context.startActivity(Intent(context, SetupActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    V125DrawerItem(Icons.Default.Settings, "Réglages Zeno", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Accueil libre : seules tes applis choisies apparaissent.",
                        color = Color.White.copy(alpha = .42f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        ) {
            when (page) {
                V125Page.HOME -> V125Home(
                    accent = accent,
                    settings = settings,
                    onSettingsChange = ::saveSettings,
                    openMenu = { scope.launch { drawerState.open() } },
                    openApps = { page = V125Page.APPS },
                    customize = { page = V125Page.CUSTOMIZE },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) }
                )
                V125Page.APPS -> V125Apps(
                    accent = accent,
                    prefs = prefs,
                    openMenu = { scope.launch { drawerState.open() } },
                    back = { page = V125Page.HOME }
                )
                V125Page.CUSTOMIZE -> V125Customize(
                    accent = accent,
                    settings = settings,
                    onChange = ::saveSettings,
                    back = { page = V125Page.HOME }
                )
            }
        }
    }
}

@Composable
private fun V125DrawerItem(
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
private fun V125Home(
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
            .background(Color(0xFF010208))
    ) {
        // Zeno sert de vrai fond d'écran : grand, statique et centré.
        if (settings.showRobot) {
            Image(
                painter = painterResource(R.drawable.zeno_robot),
                contentDescription = "Zeno",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = speak)
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF010208), Color(0xFF07152B), Color(0xFF02050C))
                        )
                    )
            )
        }

        // Voiles légers comme sur un vrai launcher pour garder l'heure et les icônes lisibles.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xB8000000),
                        0.18f to Color(0x33000000),
                        0.56f to Color.Transparent,
                        0.78f to Color(0x22000000),
                        1.0f to Color(0xA8000000)
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                IconButton(onClick = openMenu) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.White.copy(alpha = .92f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    if (settings.showClock) {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 72.sp,
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
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = customize) {
                        Icon(Icons.Default.Tune, "Personnaliser", tint = Color.White.copy(alpha = .92f))
                    }
                    Text("ZENO", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                }
            }

            // Garde une grande zone centrale libre pour voir le robot en plein écran.
            Spacer(Modifier.weight(1f))

            if (settings.showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .90f)) },
                    trailingIcon = { IconButton(onClick = speak) { Icon(Icons.Default.Mic, null, tint = accent) } },
                    placeholder = { Text("Rechercher", color = Color.White.copy(alpha = .62f)) },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x99101824),
                        unfocusedContainerColor = Color(0x88101824),
                        focusedBorderColor = Color.White.copy(alpha = .38f),
                        unfocusedBorderColor = Color.White.copy(alpha = .20f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(14.dp))
            }

            if (settings.showApps) {
                if (query.isBlank() && homeApps.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { showAdd = true }) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Ajouter à l'accueil", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    visibleApps.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { app ->
                                V125AppIcon(app, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (query.isBlank()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            IconButton(onClick = { showAdd = true }) {
                                Icon(Icons.Default.AddCircle, "Ajouter", tint = Color.White.copy(alpha = .92f))
                            }
                        }
                    }
                }
            }

            if (settings.showDock) {
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V125DockButton(Icons.Default.Phone, Color.White) { launcher.openByName("téléphone") }
                    V125DockButton(Icons.Default.ChatBubble, Color.White) { context.startActivity(Intent(context, MainActivity::class.java)) }
                    V125DockButton(Icons.Default.Apps, Color.White, large = true, onClick = openApps)
                    V125DockButton(Icons.Default.Public, Color.White) { launcher.openByName("chrome") }
                    V125DockButton(Icons.Default.CameraAlt, Color.White) { launcher.openByName("appareil photo") }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showAdd) {
        V125HomeAppsDialog(
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
private fun V125HomeAppsDialog(
    apps: List<InstalledApp>,
    selected: List<String>,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val picked = remember(selected) { mutableStateListOf<String>().apply { addAll(selected) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à l'accueil") },
        text = {
            Column {
                Text("Coche uniquement les applis que tu veux sur ta page.")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 430.dp)) {
                    items(apps) { app ->
                        val checked = app.packageName in picked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) picked.remove(app.packageName) else picked.add(app.packageName)
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            V125MiniIcon(app)
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
private fun V125AppIcon(app: InstalledApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (drawable != null) {
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                update = { it.setImageDrawable(drawable) },
                modifier = Modifier.size(58.dp)
            )
        } else {
            Box(Modifier.size(58.dp).background(Color(0x552B74FF), CircleShape), contentAlignment = Alignment.Center) {
                Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
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
private fun V125DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    large: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(if (large) 60.dp else 52.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(if (large) 35.dp else 29.dp))
    }
}

@Composable
private fun V125Customize(
    accent: Color,
    settings: HomeSettings,
    onChange: (HomeSettings) -> Unit,
    back: () -> Unit
) {
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
                        Text("PAGE D'ACCUEIL", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { V125Switch("Horloge et date", settings.showClock) { onChange(settings.copy(showClock = it)) } }
            item { V125Switch("Robot en fond plein écran", settings.showRobot) { onChange(settings.copy(showRobot = it)) } }
            item { V125Switch("Barre de recherche", settings.showSearch) { onChange(settings.copy(showSearch = it)) } }
            item { V125Switch("Mes applis choisies", settings.showApps) { onChange(settings.copy(showApps = it)) } }
            item { V125Switch("Dock du bas", settings.showDock) { onChange(settings.copy(showDock = it)) } }
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
private fun V125Switch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xA50A1422)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun V125Apps(
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

    val packagesInFolders = remember(folders) { folders.flatMap { it.packages }.toSet() }
    val rootApps = remember(apps, folders, query) {
        val source = if (query.isBlank()) apps.filterNot { it.packageName in packagesInFolders } else apps
        source.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

    fun dropApp(source: InstalledApp) {
        val folderTarget = folderBounds.entries.firstOrNull { (_, rect) -> rect.contains(dragPosition) }?.key
        if (folderTarget != null) {
            saveFolders(
                folders.map { folder ->
                    if (folder.id == folderTarget && source.packageName !in folder.packages) {
                        folder.copy(packages = folder.packages + source.packageName)
                    } else folder
                }
            )
            return
        }

        val targetPackage = appBounds.entries.firstOrNull { (pkg, rect) -> pkg != source.packageName && rect.contains(dragPosition) }?.key
        if (targetPackage != null) {
            val target = apps.firstOrNull { it.packageName == targetPackage } ?: return
            saveFolders(
                folders + AppFolder(
                    id = System.currentTimeMillis().toString(),
                    name = "Dossier",
                    packages = listOf(source.packageName, target.packageName)
                )
            )
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
                placeholder = { Text("Rechercher une application") },
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
                                V125FolderIcon(
                                    folder = folder,
                                    accent = accent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { folderBounds[folder.id] = it.boundsInRoot() },
                                    onClick = { openedFolder = folder }
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item {
                        HorizontalDivider(color = Color.White.copy(alpha = .08f))
                        Text("TOUTES LES APPLIS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }

                items(rootApps.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            V125DraggableApp(
                                app = app,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { appBounds[app.packageName] = it.boundsInRoot() },
                                onClick = { launcher.openByPackage(app.packageName) },
                                onDragStart = { local ->
                                    dragging = app
                                    val rect = appBounds[app.packageName]
                                    dragPosition = if (rect != null) rect.topLeft + local else local
                                },
                                onDrag = { dragPosition += it },
                                onDragEnd = {
                                    dropApp(app)
                                    dragging = null
                                },
                                onDragCancel = { dragging = null }
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
                    .offset { IntOffset((dragPosition.x - 30).roundToInt(), (dragPosition.y - 30).roundToInt()) }
                    .size(60.dp)
                    .alpha(.82f),
                contentAlignment = Alignment.Center
            ) {
                V125MiniIcon(app, 56)
            }
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
                            V125MiniIcon(app)
                            Spacer(Modifier.width(10.dp))
                            Text(app.label, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = accent)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openedFolder = null }) { Text("Fermer") }
            },
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
private fun V125DraggableApp(
    app: InstalledApp,
    modifier: Modifier,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(app.packageName) {
            detectDragGesturesAfterLongPress(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onDrag = { _, amount -> onDrag(amount) }
            )
        }
    ) {
        V125AppIcon(app, Modifier.fillMaxWidth(), onClick)
    }
}

@Composable
private fun V125FolderIcon(folder: AppFolder, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Folder, null, tint = accent, modifier = Modifier.size(50.dp))
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
private fun V125MiniIcon(app: InstalledApp, size: Int = 38) {
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
        Box(Modifier.size(size.dp).background(Color(0x552B74FF), CircleShape), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
