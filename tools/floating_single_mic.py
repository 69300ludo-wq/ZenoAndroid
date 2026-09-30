from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SETUP = Path('app/src/main/java/com/zeno/robot/SetupActivity.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')
STRINGS = Path('app/src/main/res/values/strings.xml')

# --- Service : plus aucun overlay, le même SpeechRecognizer écoute Salut Zeno puis la commande.
s = SERVICE.read_text(encoding='utf-8')
s = s.replace('        if (Settings.canDrawOverlays(this)) runCatching { showBubble() }\n', '')
s = s.replace('        if (Settings.canDrawOverlays(this) && bubble == null) runCatching { showBubble() }\n', '')
s = s.replace('Dis « ${currentWakePhrase()} ». Le point lumineux indique que Zeno est actif.', 'Dis « Salut Zeno » pour parler.')
s = s.replace('Dis « ${currentWakePhrase()} » ou touche le robot pour parler.', 'Dis « Salut Zeno » pour parler.')

# Aucun écran vocal secondaire depuis le service : un seul moteur possède le micro.
s = s.replace('''        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            openReliableVoiceCommand()
            return
        }
''', '''        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handler.postDelayed({ startWakeListening() }, 900)
            return
        }
''')
s = s.replace('''.onFailure { openReliableVoiceCommand() }''', '''.onFailure {
                    listening = false
                    handler.postDelayed({ startWakeListening() }, 700)
                }''')

trigger = re.compile(r'''    private fun triggerWakePhrase\(\) \{.*?\n    \}\n\n    private fun hasMic''', re.S)
replacement = '''    private fun triggerWakePhrase() {
        if (wakeTriggered) return
        wakeTriggered = true
        wakeMode = false
        runCatching { speechRecognizer?.cancel() }
        listening = false
        handler.postDelayed({
            wakeTriggered = false
            runCatching { beginCommandListening() }
        }, 420)
    }

    private fun hasMic'''
s, count = trigger.subn(replacement, s, count=1)
if count != 1:
    raise SystemExit('Déclenchement Salut Zeno introuvable')
SERVICE.write_text(s, encoding='utf-8')

# --- Setup : microphone uniquement, aucune permission de superposition.
SETUP.write_text('''package com.zeno.robot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zeno.robot.service.FloatingZenoService

class SetupActivity : ComponentActivity() {
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) activateVoice() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            activateVoice()
        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun activateVoice() {
        getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit().putBoolean("voice_enabled", true).apply()
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, FloatingZenoService::class.java))
        }.onSuccess {
            Toast.makeText(this, "Écoute Zeno activée : dis « Salut Zeno ».", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
''', encoding='utf-8')

# --- Interface : petit rond en haut, personnalisable dans Personnalisation.
main = MAIN.read_text(encoding='utf-8')
if 'import androidx.compose.animation.core.*\n' not in main:
    main = main.replace('import androidx.compose.foundation.Image\n', 'import androidx.compose.animation.core.*\nimport androidx.compose.foundation.Image\n', 1)
if 'import androidx.compose.ui.zIndex\n' not in main:
    main = main.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.zIndex\n', 1)

old_create = '''class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZenoApp() }
    }
}
'''
new_create = '''class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val micGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val prefs = getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE)
        if (micGranted && prefs.getBoolean("voice_enabled", true)) {
            prefs.edit().putBoolean("voice_enabled", true).apply()
            runCatching { startVoiceZeno(this) }
        }
        setContent { ZenoApp() }
    }
}
'''
if old_create not in main:
    raise SystemExit('onCreate introuvable')
main = main.replace(old_create, new_create, 1)

root = '''        ) {
            if (screen == Screen.HOME) {
'''
if root not in main:
    raise SystemExit('Box racine introuvable')
main = main.replace(root, '''        ) {
            VoiceStatusDot()
            if (screen == Screen.HOME) {
''', 1)

indicator = '''@Composable
private fun BoxScope.VoiceStatusDot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision += 1 }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    revision
    if (!prefs.getBoolean("voice_enabled", false)) return
    val size = prefs.getInt("indicator_size", 8).coerceIn(5, 16)
    val opacity = prefs.getFloat("indicator_opacity", .82f).coerceIn(.25f, 1f)
    val color = Color(prefs.getLong("indicator_color", 0xFF168CFFL))
    val position = prefs.getString("indicator_position", "center") ?: "center"
    val pulse = prefs.getBoolean("indicator_pulse", true)
    val alpha = if (pulse) {
        val transition = rememberInfiniteTransition(label = "zenoDot")
        val a by transition.animateFloat(
            initialValue = (opacity * .45f).coerceAtLeast(.2f), targetValue = opacity,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "zenoDotAlpha"
        )
        a
    } else opacity
    val align = when (position) { "left" -> Alignment.TopStart; "right" -> Alignment.TopEnd; else -> Alignment.TopCenter }
    Box(
        Modifier.align(align).padding(top = 6.dp, start = if (position == "left") 72.dp else 0.dp, end = if (position == "right") 72.dp else 0.dp)
            .size(size.dp).zIndex(100f).background(color.copy(alpha = alpha), CircleShape)
    )
}

'''
custom_start = main.index('@Composable\nprivate fun CustomizeScreen')
custom_end = main.index('@Composable\nprivate fun CommunityScreen', custom_start)
custom = '''@Composable
private fun CustomizeScreen(current: ZenoTheme, accent: Color, onTheme: (ZenoTheme) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    val micGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var enabled by remember { mutableStateOf(prefs.getBoolean("voice_enabled", micGranted)) }
    var dotColor by remember { mutableLongStateOf(prefs.getLong("indicator_color", 0xFF168CFFL)) }
    var dotSize by remember { mutableFloatStateOf(prefs.getInt("indicator_size", 8).toFloat()) }
    var dotPosition by remember { mutableStateOf(prefs.getString("indicator_position", "center") ?: "center") }
    var dotOpacity by remember { mutableFloatStateOf(prefs.getFloat("indicator_opacity", .82f)) }
    var dotPulse by remember { mutableStateOf(prefs.getBoolean("indicator_pulse", true)) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { enabled = true; prefs.edit().putBoolean("voice_enabled", true).apply(); runCatching { startVoiceZeno(context) } }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Thème de l'application", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        items(ZenoTheme.entries) { theme ->
            Surface(Modifier.fillMaxWidth().clickable { onTheme(theme) }, color = if (theme == current) Color(theme.accent).copy(alpha = .20f) else Color(0xFF0A1B3B), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.accent))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(theme.accent))); Spacer(Modifier.width(12.dp)); Text(theme.label, color = Color.White, modifier = Modifier.weight(1f)); if (theme == current) Icon(Icons.Default.CheckCircle, null, tint = Color(theme.accent))
                }
            }
        }
        item {
            Text("Écoute vocale directe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Sans Zeno flottant. Dis simplement « Salut Zeno ».", color = Color(0xFF9AB4D8))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (enabled) "Réveil vocal actif" else "Réveil vocal désactivé", color = Color.White, modifier = Modifier.weight(1f))
                Switch(enabled, { on ->
                    if (on) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) { enabled = true; prefs.edit().putBoolean("voice_enabled", true).apply(); runCatching { startVoiceZeno(context) } }
                        else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else { enabled = false; prefs.edit().putBoolean("voice_enabled", false).apply(); context.stopService(Intent(context, FloatingZenoService::class.java)) }
                })
            }
        }
        item {
            Text("Petit rond Zeno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Il reste dans l'interface, sans superposition Android.", color = Color(0xFF9AB4D8))
            Spacer(Modifier.height(10.dp)); Text("Couleur", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0xFF168CFFL, 0xFF19D8FFL, 0xFF9B5CFFL, 0xFF22D38AL, 0xFFFF55C8L).forEach { value ->
                    Surface(Modifier.size(36.dp).clickable { dotColor = value; prefs.edit().putLong("indicator_color", value).apply() }, CircleShape, Color(value), border = androidx.compose.foundation.BorderStroke(if (dotColor == value) 3.dp else 1.dp, Color.White)) {}
                }
            }
            Spacer(Modifier.height(12.dp)); Text("Taille : ${dotSize.toInt()} dp", color = Color.White)
            Slider(dotSize, { dotSize = it; prefs.edit().putInt("indicator_size", it.toInt()).apply() }, valueRange = 5f..16f, steps = 10)
            Text("Position", color = Color.White)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("left" to "Gauche", "center" to "Centre", "right" to "Droite").forEach { (key, label) ->
                    OutlinedButton({ dotPosition = key; prefs.edit().putString("indicator_position", key).apply() }, Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(if (dotPosition == key) 2.dp else 1.dp, if (dotPosition == key) accent else Color(0xFF35577F))) { Text(label, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(12.dp)); Text("Opacité : ${(dotOpacity * 100).toInt()} %", color = Color.White)
            Slider(dotOpacity, { dotOpacity = it; prefs.edit().putFloat("indicator_opacity", it).apply() }, valueRange = .25f..1f)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Pulsation douce", color = Color.White, modifier = Modifier.weight(1f)); Switch(dotPulse, { dotPulse = it; prefs.edit().putBoolean("indicator_pulse", it).apply() }) }
        }
    }
}

'''
main = main[:custom_start] + indicator + custom + main[custom_end:]

settings_start = main.index('@Composable\nprivate fun SettingsScreen')
settings_end = main.index('@Composable\nprivate fun SettingCard', settings_start)
main = main[:settings_start] + '''@Composable
private fun SettingsScreen(accent: Color, customize: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SettingCard(Icons.Default.Palette, "Apparence", "Thème et petit rond Zeno", accent, customize) }
        item { SettingCard(Icons.Default.Mic, "Voix", "Réveil direct : Salut Zeno", accent, customize) }
        item { SettingCard(Icons.Default.Translate, "Langues", "Traduction locale via ML Kit", accent) {} }
        item { SettingCard(Icons.Default.Apps, "Applications", "Zeno peut ouvrir les applications installées", accent) {} }
        item { SettingCard(Icons.Default.Security, "Confidentialité", "Micro Android, sans superposition", accent) {} }
        item { Text("Zeno Android v1.3.9", color = Color(0xFF7894C0), modifier = Modifier.padding(top = 14.dp)) }
    }
}

''' + main[settings_end:]
main = main.replace('Text("Version 1.3.1"', 'Text("Version 1.3.9"')

helper = re.compile(r'''private fun startFloatingZeno\(context: Context\) \{.*?\n\}''', re.S)
main, n = helper.subn('''private fun startVoiceZeno(context: Context) {
    ContextCompat.startForegroundService(context, Intent(context, FloatingZenoService::class.java))
}

private fun startFloatingZeno(context: Context) = startVoiceZeno(context)''', main, count=1)
if n != 1 and 'private fun startVoiceZeno(context: Context)' not in main:
    raise SystemExit('Helper vocal introuvable')
MAIN.write_text(main, encoding='utf-8')

manifest = MANIFEST.read_text(encoding='utf-8').replace('    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n', '')
MANIFEST.write_text(manifest, encoding='utf-8')
strings = STRINGS.read_text(encoding='utf-8').replace('Zeno flottant', 'Écoute vocale Zeno').replace('Maintient Zeno visible au-dessus de vos applications.', 'Maintient « Salut Zeno » actif en arrière-plan.')
STRINGS.write_text(strings, encoding='utf-8')

print('Zeno 1.3.9 : vocal direct sans overlay + petit rond personnalisable')
