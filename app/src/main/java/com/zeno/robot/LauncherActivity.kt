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
import androidx.compose.ui.draw.clip
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
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.IconManager
import com.zeno.robot.model.InstalledApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoLauncher() }
    }
}

private enum class LauncherPage { HOME, APPS }

@Composable
private fun ZenoLauncher() {
    val context = LocalContext.current
    val accent = Color(remember { IconManager(context.applicationContext).current().accent })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(LauncherPage.HOME) }

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
                    DrawerItem(Icons.Default.Apps, "Toutes les applications", page == LauncherPage.APPS) {
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
                    DrawerItem(Icons.Default.Palette, "Personnalisation", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Default.Settings, "Réglages Zeno", false) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Zeno Home 1.2.1",
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
                    openMenu = { scope.launch { drawerState.open() } },
                    openApps = { page = LauncherPage.APPS },
                    speak = { context.startActivity(Intent(context, VoiceCommandActivity::class.java)) },
                    openZeno = { context.startActivity(Intent(context, MainActivity::class.java)) }
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
    openMenu: () -> Unit,
    openApps: () -> Unit,
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

    val shownApps = remember(apps, query) {
        if (query.isBlank()) apps.take(8)
        else apps.filter { it.label.contains(query, ignoreCase = true) }.take(8)
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
                    listOf(
                        Color(0xFF01030A),
                        Color(0xFF071C35),
                        Color(0xFF0A1830),
                        Color(0xFF081020),
                        Color(0xFF02050B)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(Color(0x3318D8FF), Color(0x221B70FF), Color.Transparent),
                    radius = 1050f
                )
            )
    ) {
        // Fond robot géant, comme un wallpaper vivant.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .align(Alignment.TopCenter)
                .offset(y = 122.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(430.dp)
                    .alpha(glow)
                    .background(
                        Brush.radialGradient(
                            listOf(accent, Color(0xFF635BFF), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Image(
                painter = painterResource(R.drawable.zeno_robot),
                contentDescription = "Zeno",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(430.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clickable(onClick = speak)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = openMenu, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White.copy(alpha = .88f))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    Text(
                        "● ZENO PRÊT",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }
                IconButton(onClick = openZeno, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Réglages", tint = Color.White.copy(alpha = .88f))
                }
            }

            Spacer(Modifier.height(280.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = .78f)) },
                trailingIcon = {
                    IconButton(onClick = speak) {
                        Icon(Icons.Default.Mic, null, tint = accent)
                    }
                },
                placeholder = { Text("Rechercher apps, contacts, web...", color = Color.White.copy(alpha = .55f)) },
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

            val rows = shownApps.chunked(4)
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { app ->
                        HomeAppIcon(app = app, modifier = Modifier.weight(1f)) {
                            launcher.openByPackage(app.packageName)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.weight(1f))

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
                    DockButton(Icons.Default.Settings, accent, openMenu)
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
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        modifier = Modifier
            .size(if (large) 58.dp else 48.dp)
            .clickable(onClick = onClick),
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

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF02050B), Color(0xFF071729), Color(0xFF02050B)))
        )
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = openMenu) { Icon(Icons.Default.Menu, "Menu", tint = Color.White) }
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Retour", tint = accent) }
                Column(Modifier.weight(1f)) {
                    Text("APPLICATIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("ZENO HOME", color = accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                placeholder = { Text("Rechercher une application") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
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
}
