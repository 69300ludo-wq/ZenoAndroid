package com.zeno.robot

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                    drawerContainerColor = Color(0xFF05101F),
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(304.dp)
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(58.dp),
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
                            Text("ZENO", color = accent, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.4.sp)
                            Text("ASSISTANT GALAXY", color = Color(0xFF91A9C9), fontSize = 10.sp, letterSpacing = 1.2.sp)
                            Text("Version 1.1.8", color = Color.White.copy(alpha = .50f), fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Spacer(Modifier.height(8.dp))
                    DrawerItem(Icons.Default.Home, "Accueil Zeno", page == LauncherPage.HOME) {
                        page = LauncherPage.HOME
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Apps, "Mes applications", page == LauncherPage.APPS) {
                        page = LauncherPage.APPS
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
                    DrawerItem(Icons.Default.Bolt, "Zeno complet", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = Color(0xFF0A1930),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("THÈME ACTIF", color = Color(0xFF8EA8CC), fontSize = 10.sp, letterSpacing = 1.4.sp)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(accent, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text("ZENO GALAXY", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("Spatial • Cyan • Violet", color = Color.White.copy(alpha = .58f), fontSize = 11.sp)
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
                            listOf(accent.copy(alpha = .18f), Color(0x223E38FF), Color.Transparent),
                            radius = 980f
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
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        icon = { Icon(icon, null) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
    )
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0x8A08152A),
                    border = BorderStroke(1.dp, accent.copy(alpha = .28f))
                ) {
                    IconButton(onClick = openMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ZENO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = 3.5.sp)
                    Text("VOTRE ASSISTANT IA", color = accent, fontSize = 10.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = CircleShape, color = Color(0x3316FFB1), border = BorderStroke(1.dp, Color(0x5572FFC8))) {
                    Text("● PRÊT", color = Color(0xFF72FFC8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0x66061328),
                border = BorderStroke(1.dp, accent.copy(alpha = .30f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    Box(
                        Modifier.size(340.dp).background(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = .38f), Color(0x443F47FF), Color(0x1100C8FF), Color.Transparent)
                            ),
                            CircleShape
                        )
                    )
                    Image(
                        painter = painterResource(R.drawable.zeno_robot),
                        contentDescription = "Zeno",
                        modifier = Modifier.size(328.dp),
                        contentScale = ContentScale.Fit
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xC2071328),
                        border = BorderStroke(1.dp, accent.copy(alpha = .35f))
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Dis : « Salut Zeno »", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("TOUJOURS AVEC VOUS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center, letterSpacing = 1.1.sp)
            Text(
                "Parle à Zeno, ouvre tes applis et retrouve toutes les fonctions de ton assistant depuis un seul écran.",
                color = Color(0xFFA9BFDF),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = speak,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF00131A)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .18f))
            ) {
                Icon(Icons.Default.ChatBubble, null)
                Spacer(Modifier.width(10.dp))
                Text("PARLER À ZENO", fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = .5.sp)
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HomeShortcut(Icons.Default.Apps, "Mes applis", accent, Modifier.weight(1f), openApps)
                HomeShortcut(Icons.Default.SmartToy, "Zeno flottant", Color(0xFF9B6CFF), Modifier.weight(1f), floating)
                HomeShortcut(Icons.Default.Bolt, "Zeno complet", accent, Modifier.weight(1f), openZeno)
                HomeShortcut(Icons.Default.Menu, "Menu", Color(0xFF9B6CFF), Modifier.weight(1f), openMenu)
            }
            Spacer(Modifier.height(18.dp))
            Text("ACCÈS RAPIDE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), letterSpacing = 1.sp)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LauncherCard(Icons.Default.Chat, "Chat IA", "Dans Zeno complet", accent, Modifier.weight(1f), openZeno)
                LauncherCard(Icons.Default.Translate, "Traduction", "Français ↔ Anglais", Color(0xFF9B6CFF), Modifier.weight(1f), openZeno)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LauncherCard(Icons.Default.Public, "Recherche web", "Recherche avec Zeno", accent, Modifier.weight(1f), openZeno)
                LauncherCard(Icons.Default.Palette, "Personnalisation", "Thèmes et icône", Color(0xFF9B6CFF), Modifier.weight(1f), openZeno)
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(88.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xB308172E),
        border = BorderStroke(1.dp, tint.copy(alpha = .42f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(7.dp))
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
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
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Color(0xFF829DC3), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text("MES APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text("ZENO GALAXY", color = accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
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
