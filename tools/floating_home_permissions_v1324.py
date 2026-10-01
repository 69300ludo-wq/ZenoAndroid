from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# -----------------------------------------------------------------------------
# Accueil : petit voyant flottant. Un toucher ouvre directement les permissions
# microphone + superposition, sans passer par Personnalisation.
# -----------------------------------------------------------------------------
main = MAIN.read_text(encoding='utf-8')

if 'import android.net.Uri\n' not in main:
    anchor = 'import android.content.Intent\n'
    if anchor not in main:
        raise SystemExit('Import Intent introuvable')
    main = main.replace(anchor, anchor + 'import android.net.Uri\n', 1)
if 'import androidx.core.content.edit\n' not in main:
    anchor = 'import androidx.core.content.ContextCompat\n'
    if anchor not in main:
        raise SystemExit('Import ContextCompat introuvable')
    main = main.replace(anchor, anchor + 'import androidx.core.content.edit\n', 1)

# Ne plus afficher la grosse carte de permissions dans la liste de l'accueil.
main = main.replace(
    '            item { Spacer(Modifier.height(12.dp)); PermissionControlCard(accent) }\n',
    '',
    1,
)

# Remplacer l'ancienne carte par le menu flottant compact.
perm_start = main.find('@Composable\nprivate fun PermissionControlCard(accent: Color)')
perm_end = main.find('@Composable\nprivate fun ActionGrid', perm_start)
if perm_start < 0 or perm_end <= perm_start:
    raise SystemExit('PermissionControlCard introuvable')

floating_menu = r'''@Composable
private fun BoxScope.HomeVoiceFloatingMenu(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }

    fun hasMicrophone(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasOverlay(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    var expanded by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(hasMicrophone()) }
    var overlayGranted by remember { mutableStateOf(hasOverlay()) }
    var voiceState by remember { mutableStateOf(if (micGranted) "idle" else "permission") }

    fun activateZeno() {
        prefs.edit { putBoolean("voice_enabled", true) }
        runCatching { startVoiceZeno(context) }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted || hasMicrophone()
        if (micGranted) activateZeno()
    }

    val overlayPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayGranted = hasOverlay()
        if (micGranted) activateZeno()
    }

    val appSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        micGranted = hasMicrophone()
        overlayGranted = hasOverlay()
        if (micGranted) activateZeno()
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == FloatingZenoService.ACTION_VOICE_STATE) {
                    voiceState = intent.getStringExtra(FloatingZenoService.EXTRA_STATE) ?: "idle"
                }
            }
        }
        val filter = android.content.IntentFilter(FloatingZenoService.ACTION_VOICE_STATE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val dotColor = when {
        !micGranted -> Color(0xFFFF7043)
        voiceState == "command" -> Color(0xFF9B5CFF)
        voiceState == "speaking" -> Color(0xFFC084FC)
        voiceState == "error" || voiceState == "permission" -> Color(0xFFFF7043)
        voiceState == "off" -> Color(0xFF607D8B)
        else -> Color(0xFF168CFF)
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (expanded) {
            Surface(
                modifier = Modifier.widthIn(min = 270.dp, max = 320.dp),
                color = Color(0xF20A1835),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .85f)),
                tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(13.dp).background(dotColor, CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Zeno vocal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Permissions directes depuis l'accueil", color = Color(0xFFAFC3E8), fontSize = 11.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = if (micGranted) Color(0xFF34D399) else Color(0xFFFFA24A))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Microphone", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(if (micGranted) "Autorisé" else "À autoriser", color = Color(0xFF9DB3DA), fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (micGranted) activateZeno()
                                else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        ) { Text(if (micGranted) "Actif" else "Autoriser") }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, null, tint = if (overlayGranted) Color(0xFF34D399) else Color(0xFFFFA24A))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Superposition autres applis", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (overlayGranted) "Autorisée" else "À autoriser pour le petit voyant global",
                                color = Color(0xFF9DB3DA),
                                fontSize = 11.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (!overlayGranted) {
                                    val overlayIntent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    runCatching { overlayPermission.launch(overlayIntent) }
                                        .onFailure {
                                            appSettings.launch(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                            )
                                        }
                                } else if (micGranted) activateZeno()
                            }
                        ) { Text(if (overlayGranted) "Actif" else "Autoriser") }
                    }

                    Button(
                        onClick = {
                            micGranted = hasMicrophone()
                            overlayGranted = hasOverlay()
                            if (micGranted) activateZeno()
                            else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Activer Zeno")
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.size(58.dp).clickable { expanded = !expanded },
            shape = CircleShape,
            color = Color(0xF20B1732),
            border = androidx.compose.foundation.BorderStroke(2.dp, dotColor.copy(alpha = .9f)),
            shadowElevation = 9.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(20.dp).background(dotColor, CircleShape))
            }
        }
    }
}

'''
main = main[:perm_start] + floating_menu + main[perm_end:]

# Afficher le menu flottant uniquement sur l'accueil, au-dessus du contenu.
root_marker = '''        }
    }
}

@Composable
private fun ZenoTopBar'''
if 'HomeVoiceFloatingMenu(accent)' not in main:
    if root_marker not in main:
        raise SystemExit('Fin de ZenoApp introuvable')
    main = main.replace(
        root_marker,
        '''            if (screen == Screen.HOME) HomeVoiceFloatingMenu(accent)
        }
    }
}

@Composable
private fun ZenoTopBar''',
        1,
    )

main = main.replace('Zeno Android v1.3.23', 'Zeno Android v1.3.24')
main = main.replace('Version 1.3.23', 'Version 1.3.24')
MAIN.write_text(main, encoding='utf-8')

# -----------------------------------------------------------------------------
# Manifest : restaurer l'option Android « afficher par-dessus les autres applis ».
# -----------------------------------------------------------------------------
manifest = MANIFEST.read_text(encoding='utf-8')
overlay_permission = '    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n'
if 'android.permission.SYSTEM_ALERT_WINDOW' not in manifest:
    anchor = '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'
    if anchor not in manifest:
        raise SystemExit('Permission RECORD_AUDIO introuvable')
    manifest = manifest.replace(anchor, anchor + overlay_permission, 1)
MANIFEST.write_text(manifest, encoding='utf-8')

# -----------------------------------------------------------------------------
# Service : remettre le petit voyant global uniquement si la permission de
# superposition est accordée. Il reste non tactile pour éviter les anciens bugs.
# -----------------------------------------------------------------------------
service = SERVICE.read_text(encoding='utf-8')

def replace_in_block(source: str, start_marker: str, end_marker: str, old: str, new: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + 1)
    if start < 0 or end < 0:
        raise SystemExit(f'Bloc introuvable: {start_marker}')
    block = source[start:end]
    if old in block:
        block = block.replace(old, new, 1)
    elif new not in block:
        raise SystemExit(f'Valeur introuvable dans bloc: {old.strip()}')
    return source[:start] + block + source[end:]

service = replace_in_block(
    service,
    '    override fun onCreate() {',
    '    override fun onStartCommand',
    '        removeOverlayDot()\n',
    '        showOverlayDot()\n',
)
service = replace_in_block(
    service,
    '    override fun onStartCommand',
    '    private fun hasMic',
    '        removeOverlayDot()\n',
    '        showOverlayDot()\n',
)

service, count = re.subn(
    r'(    private fun broadcastState\(state: String\) \{\n)(?!        updateOverlayDot\(state\)\n)',
    r'\1        updateOverlayDot(state)\n',
    service,
    count=1,
)
if count != 1 and 'private fun broadcastState(state: String) {\n        updateOverlayDot(state)\n' not in service:
    raise SystemExit('broadcastState introuvable')

service = service.replace(
    'Service vocal Zeno sans voyant de superposition.',
    'Service vocal Zeno avec petit voyant de superposition optionnel.',
)

# -----------------------------------------------------------------------------
# Bip : le SpeechRecognizer ne doit plus être recréé toutes les quelques secondes.
# On garde la session de réveil ouverte bien plus longtemps et on privilégie le
# moteur local/offline. Les résultats partiels permettent toujours de détecter
# « Zeno » immédiatement sans attendre la fin de la longue fenêtre.
# -----------------------------------------------------------------------------
wake_start = 'runCatching { current.startListening(recognitionIntent(partial = true, longWindow = true)) }'
if wake_start in service:
    service = service.replace(
        wake_start,
        '''val wakeIntent = recognitionIntent(partial = true, longWindow = true).apply {
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 45_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_500L)
        }
        runCatching { current.startListening(wakeIntent) }''',
        1,
    )
else:
    raise SystemExit('Démarrage écoute réveil introuvable')

service = service.replace(
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 900L',
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 5_000L',
    1,
)
service = service.replace(
    'else -> handler.postDelayed({ startWakeListening() }, 650)',
    'else -> handler.postDelayed({ startWakeListening() }, 2_500)',
    1,
)

SERVICE.write_text(service, encoding='utf-8')

print('Zeno 1.3.24 : petit menu vocal flottant accueil + micro/superposition + réveil beaucoup moins relancé')
