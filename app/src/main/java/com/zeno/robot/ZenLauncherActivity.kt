package com.zeno.robot

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.HomeSettings
import com.zeno.robot.data.LauncherPreferences
import com.zeno.robot.model.InstalledApp
import kotlin.math.roundToInt

class ZenLauncherActivity : ComponentActivity() {
    private val homePulse = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { ZenoLauncher(homePulse.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homePulse.intValue++
    }
}

private enum class ZenoLauncherPage { HOME, DRAWER }

@Composable
private fun ZenoLauncher(homePulse: Int) {
    val context = LocalContext.current
    val prefs = remember { LauncherPreferences(context.applicationContext) }
    var page by remember { mutableStateOf(ZenoLauncherPage.HOME) }
    var settings by remember { mutableStateOf(prefs.loadHomeSettings()) }

    fun save(value: HomeSettings) {
        settings = value
        prefs.saveHomeSettings(value)
    }

    LaunchedEffect(homePulse) { page = ZenoLauncherPage.HOME }
    BackHandler(enabled = page == ZenoLauncherPage.DRAWER) { page = ZenoLauncherPage.HOME }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF76D9FF),
            background = Color.Transparent,
            surface = Color(0xF21A1C20)
        )
    ) {
        when (page) {
            ZenoLauncherPage.HOME -> ZenoHome(
                settings = settings,
                onSettingsChange = ::save,
                openDrawer = { page = ZenoLauncherPage.DRAWER }
            )
            ZenoLauncherPage.DRAWER -> ZenoDrawer(
                backHome = { page = ZenoLauncherPage.HOME }
            )
        }
    }
}

@Composable
private fun ZenoHome(
    settings: HomeSettings,
    onSettingsChange: (HomeSettings) -> Unit,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var showMenu by remember { mutableStateOf(false) }
    var showAppsPicker by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<InstalledApp?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val iconBounds = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val homeApps = remember(apps, settings.homePackages) {
        settings.homePackages.mapNotNull { byPackage[it] }
    }

    fun reorder(source: InstalledApp) {
        val target = iconBounds.entries
            .firstOrNull { (pkg, rect) -> pkg != source.packageName && rect.contains(dragPosition) }
            ?.key ?: return
        val next = settings.homePackages.toMutableList()
        val from = next.indexOf(source.packageName)
        val to = next.indexOf(target)
        if (from >= 0 && to >= 0 && from != to) {
            next.removeAt(from)
            next.add(to.coerceAtMost(next.size), source.packageName)
            onSettingsChange(settings.copy(homePackages = next))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            if (homeApps.isEmpty()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Appui long pour ajouter tes applications",
                        color = Color.White.copy(alpha = .66f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homeApps.chunked(4)) { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { app ->
                                ZenoHomeIcon(
                                    app = app,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { iconBounds[app.packageName] = it.boundsInRoot() },
                                    onOpen = { launcher.openByPackage(app.packageName) },
                                    onDragStart = {
                                        dragging = app
                                        dragPosition = iconBounds[app.packageName]?.center ?: Offset.Zero
                                    },
                                    onDrag = { dragPosition += it },
                                    onDragEnd = {
                                        reorder(app)
                                        dragging = null
                                    }
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            ZenoDock(
                apps = apps,
                launcher = launcher,
                openDrawer = openDrawer
            )
            Spacer(Modifier.height(8.dp))
        }

        dragging?.let { app ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPosition.x - 32f).roundToInt(),
                            (dragPosition.y - 32f).roundToInt()
                        )
                    }
                    .size(64.dp)
                    .alpha(.85f),
                contentAlignment = Alignment.Center
            ) {
                ZenoRawIcon(app, 60)
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Zeno · Écran d'accueil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ZenoMenuRow(Icons.Default.Add, "Choisir mes applications") {
                        showMenu = false
                        showAppsPicker = true
                    }
                    ZenoMenuRow(Icons.Default.Wallpaper, "Fond d'écran") {
                        showMenu = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
                    }
                    ZenoMenuRow(Icons.Default.Apps, "Toutes les applications") {
                        showMenu = false
                        openDrawer()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMenu = false }) { Text("Fermer") } }
        )
    }

    if (showAppsPicker) {
        ZenoAppsPicker(
            apps = apps,
            selected = settings.homePackages,
            onDismiss = { showAppsPicker = false },
            onSave = {
                onSettingsChange(settings.copy(homePackages = it))
                showAppsPicker = false
            }
        )
    }
}

@Composable
private fun ZenoMenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 16.sp)
    }
}

@Composable
private fun ZenoAppsPicker(
    apps: List<InstalledApp>,
    selected: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val picked = remember(selected) { mutableStateListOf<String>().apply { addAll(selected) } }
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personnaliser l'accueil") },
        text = {
            Column {
                Text(
                    "${picked.size} application${if (picked.size > 1) "s" else ""} sélectionnée${if (picked.size > 1) "s" else ""}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Rechercher une application") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 390.dp)) {
                    items(filtered) { app ->
                        val checked = app.packageName in picked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) picked.remove(app.packageName) else picked.add(app.packageName)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ZenoRawIcon(app, 40)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) picked.remove(app.packageName) else picked.add(app.packageName)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked.toList()) }) { Text("Enregistrer") } },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    picked.clear()
                    picked.addAll(apps.map { it.packageName })
                }) { Text("Tout ajouter") }
                TextButton(onClick = { picked.clear() }) { Text("Tout retirer") }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    )
}

@Composable
private fun ZenoHomeIcon(
    app: InstalledApp,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = modifier
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            }
            .clickable(onClick = onOpen)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZenoRawIcon(app, 62)
        Spacer(Modifier.height(5.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color(0x33000000), RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun ZenoDock(apps: List<InstalledApp>, launcher: AppLauncher, openDrawer: () -> Unit) {
    val contacts = remember(apps) { findDockApp(apps, listOf("contacts", "contact"), listOf("com.google.android.contacts")) }
    val store = remember(apps) { findDockApp(apps, listOf("play store", "store"), listOf("com.android.vending")) }
    val messages = remember(apps) { findDockApp(apps, listOf("messages", "message", "sms"), listOf("com.google.android.apps.messaging")) }
    val phone = remember(apps) { findDockApp(apps, listOf("téléphone", "telephone", "phone"), listOf("com.google.android.dialer")) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZenoDockApp(contacts, Icons.Default.Contacts) { if (contacts != null) launcher.openByPackage(contacts.packageName) else launcher.openByName("contacts") }
        ZenoDockApp(store, Icons.Default.Shop) { if (store != null) launcher.openByPackage(store.packageName) else launcher.openByName("play store") }
        Box(
            Modifier
                .size(66.dp)
                .clickable(onClick = openDrawer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Apps, "Applications", tint = Color.White, modifier = Modifier.size(39.dp))
        }
        ZenoDockApp(messages, Icons.Default.ChatBubble) { if (messages != null) launcher.openByPackage(messages.packageName) else launcher.openByName("messages") }
        ZenoDockApp(phone, Icons.Default.Phone) { if (phone != null) launcher.openByPackage(phone.packageName) else launcher.openByName("téléphone") }
    }
}

@Composable
private fun ZenoDockApp(app: InstalledApp?, fallback: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(66.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) ZenoRawIcon(app, 60)
        else Icon(fallback, null, tint = Color.White, modifier = Modifier.size(38.dp))
    }
}

private fun findDockApp(apps: List<InstalledApp>, labels: List<String>, packages: List<String>): InstalledApp? {
    apps.firstOrNull { it.packageName in packages }?.let { return it }
    return apps.firstOrNull { app -> labels.any { app.label.lowercase(Locale.getDefault()).contains(it) } }
}

@Composable
private fun ZenoDrawer(backHome: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xF2131418))) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = backHome) { Icon(Icons.Default.ArrowBack, "Accueil", tint = Color.White) }
                Text("Toutes les applications", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Rechercher") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable { launcher.openByPackage(app.packageName) }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ZenoRawIcon(app, 58)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    app.label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenoRawIcon(app: InstalledApp, size: Int) {
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
        Box(
            Modifier.size(size.dp).background(Color(0x55000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(app.label.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
