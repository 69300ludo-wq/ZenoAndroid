package com.zeno.robot

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.IconManager
import com.zeno.robot.model.InstalledApp
import kotlinx.coroutines.launch

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoGalaxyLauncher() }
    }
}

private enum class LauncherPage { HOME, APPS }

@Composable
private fun ZenoGalaxyLauncher() {
    val context = LocalContext.current
    val iconManager = remember { IconManager(context.applicationContext) }
    val accent = Color(iconManager.current().accent)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(LauncherPage.HOME) }

    val scheme = darkColorScheme(
        primary = accent,
        secondary = Color(0xFF9B6CFF),
        background = Color(0xFF020713),
        surface = Color(0xFF07162B),
        surfaceVariant = Color(0xFF0C2440)
    )

    MaterialTheme(colorScheme = scheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF06101F),
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = accent.copy(alpha = .16f),
                            border = BorderStroke(1.dp, accent.copy(alpha = .75f))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.zeno_robot),
                                contentDescription = null,
                                modifier = Modifier.padding(5.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("ZENO", color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
                            Text("GALAXY LAUNCHER", color = Color(0xFF91A9C9), fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    NavigationDrawerItem(
                        label = { Text("Accueil Zeno") },
                        selected = page == LauncherPage.HOME,
                        icon = { Icon(Icons.Default.Home, null) },
                        onClick = { page = LauncherPage.HOME; scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Mes applications") },
                        selected = page == LauncherPage.APPS,
                        icon = { Icon(Icons.Default.Apps, null) },
                        onClick = { page = LauncherPage.APPS; scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Parler à Zeno") },
                        selected = false,
                        icon = { Icon(Icons.Default.Mic, null) },
                        onClick = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)); scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Zeno flottant") },
                        selected = false,
                        icon = { Icon(Icons.Default.SmartToy, null) },
                        onClick = { context.startActivity(Intent(context, SetupActivity::class.java)); scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Zeno complet") },
                        selected = false,
                        icon = { Icon(Icons.Default.Bolt, null) },
                        onClick = { context.startActivity(Intent(context, MainActivity::class.java)); scope.launch { drawerState.close() } },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = accent.copy(alpha = .10f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("THÈME ACTIF", color = Color(0xFF8EA8CC), fontSize = 10.sp, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("ZENO GALAXY", color = accent, fontWeight = FontWeight.Bold)
                            Text("Cyan • Violet • Spatial", color = Color.White.copy(alpha = .70f), fontSize = 12.sp)
                        }
                    }
                }
            }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF01030A), Color(0xFF06142E), Color(0xFF100822), Color(0xFF020611))
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = .20f), Color(0x222A57FF), Color.Transparent),
                            radius = 950f
                        )
                    )
            ) {
                when (page) {
                    LauncherPage.HOME -> LauncherHome(
                        accent = accent,
                        openMenu = { scope.launch { drawerState.open() } },
                        openApps = { page = LauncherPage.APPS },
                        speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) },
                        openZeno = { context.startActivity(Intent(context, MainActivity::class.java)) },
                        floating = { context.startActivity(Intent(context, SetupActivity::class.java)) }
                    )
                    LauncherPage.APPS -> LauncherApps(
                        accent = accent,
                        openMenu = { scope.launch { drawerState.open() } },
                        back = { page = LauncherPage.HOME }
                    )
                }
            }
        }
    }
}

@Composable
private fun LauncherHome(
    accent: Color,
    openMenu: () -> Unit,
    openApps: () -> Unit,
    speak: () -> Unit,
    openZeno: () -> Unit,
    floating: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 12.dp, bottom = 30.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("ZENO", color = accent, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 3.sp)
                    Text("GALAXY LAUNCHER", color = Color(0xFF8EA8CC), fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Surface(shape = CircleShape, color = Color(0x3316FFB1)) {
                    Text("● EN LIGNE", color = Color(0xFF72FFC8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(345.dp)) {
                Box(
                    Modifier.size(330.dp).background(
                        Brush.radialGradient(listOf(accent.copy(alpha = .34f), Color(0x333B5CFF), Color.Transparent)),
                        CircleShape
                    )
                )
                Image(
                    painter = painterResource(R.drawable.zeno_robot),
                    contentDescription = "Zeno",
                    modifier = Modifier.size(315.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text("TON UNIVERS. TON ZENO.", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Parle à Zeno, ouvre tes applications et garde toutes les fonctions de ton assistant au même endroit.",
                color = Color(0xFFAAC0E1),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = speak,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF00131A))
            ) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(10.dp))
                Text("PARLER À ZENO", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = openApps,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = .65f))
                ) {
                    Icon(Icons.Default.Apps, null, tint = accent)
                    Spacer(Modifier.width(7.dp))
                    Text("APPLIS", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = floating,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF9B6CFF).copy(alpha = .75f))
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = Color(0xFFB999FF))
                    Spacer(Modifier.width(7.dp))
                    Text("FLOTTANT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("MENU ZENO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LauncherCard(Icons.Default.Apps, "Mes applis", "Launcher", accent, Modifier.weight(1f), openApps)
                LauncherCard(Icons.Default.Chat, "Chat IA", "Zeno complet", accent, Modifier.weight(1f), openZeno)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LauncherCard(Icons.Default.Translate, "Traduction", "Zeno complet", Color(0xFF9B6CFF), Modifier.weight(1f), openZeno)
                LauncherCard(Icons.Default.Public, "Recherche web", "Zeno complet", Color(0xFF9B6CFF), Modifier.weight(1f), openZeno)
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = openZeno),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xB30A1831),
                border = BorderStroke(1.dp, accent.copy(alpha = .30f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("OUVRIR ZENO COMPLET", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Chat, traduction, web, personnalisation, communauté et réglages", color = Color(0xFF8EA8CC), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = .55f))
                }
            }
        }
    }
}

@Composable
private fun LauncherCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB30A1831),
        border = BorderStroke(1.dp, tint.copy(alpha = .40f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.height(9.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF829DC3), fontSize = 10.sp)
        }
    }
}

@Composable
private fun LauncherApps(accent: Color, openMenu: () -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AppLauncher(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    LaunchedEffect(Unit) {
        apps = launcher.listLaunchableApps().filter { it.packageName != context.packageName }
    }
    val filtered = remember(apps, query) {
        apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
            Column(Modifier.weight(1f)) {
                Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text("ZENO GALAXY LAUNCHER", color = Color(0xFF829DC3), fontSize = 10.sp, letterSpacing = 1.3.sp)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
            placeholder = { Text("Rechercher une application") },
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            items(filtered.chunked(3)) { rowApps ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowApps.forEach { app ->
                        AppTile(app, accent, Modifier.weight(1f)) { launcher.openByPackage(app.packageName) }
                    }
                    repeat(3 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: InstalledApp, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    }
    Surface(
        modifier = modifier.height(112.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xC20A1831),
        border = BorderStroke(1.dp, accent.copy(alpha = .22f))
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (appIcon != null) {
                AndroidView(
                    factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                    update = { it.setImageDrawable(appIcon) },
                    modifier = Modifier.size(50.dp)
                )
            } else {
                Surface(shape = CircleShape, color = accent.copy(alpha = .16f), modifier = Modifier.size(50.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(app.label.take(1).uppercase(), color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                app.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
