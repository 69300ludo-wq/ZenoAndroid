from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# -----------------------------------------------------------------------------
# Accueil : voyant par défaut fixe, sans menu de personnalisation du voyant.
# -----------------------------------------------------------------------------
main = MAIN.read_text(encoding='utf-8')
main = main.replace(
    '            if (screen == Screen.HOME) VoiceStatusDot(draggable = true)\n',
    '            if (screen == Screen.HOME) VoiceStatusDot()\n',
    1,
)
main = main.replace(
    '            VoiceStatusDot(draggable = screen == Screen.HOME)\n',
    '            if (screen == Screen.HOME) VoiceStatusDot()\n',
    1,
)

indicator_pattern = re.compile(
    r'@Composable\nprivate fun BoxScope\.VoiceStatusDot\([^)]*\) \{.*?\n\}\n\n',
    re.S,
)
indicator = r'''@Composable
private fun BoxScope.VoiceStatusDot() {
    val context = LocalContext.current
    val micGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var voiceState by remember { mutableStateOf(if (micGranted) "idle" else "permission") }

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

    val color = when (voiceState) {
        "command" -> Color(0xFF9B5CFF)
        "speaking" -> Color(0xFFC084FC)
        "permission", "error" -> Color(0xFFFF7043)
        "off" -> Color(0xFF607D8B)
        else -> Color(0xFF168CFF)
    }
    val targetAlpha = when (voiceState) {
        "command" -> 1f
        "speaking" -> .92f
        "permission", "error" -> .94f
        "off" -> .42f
        else -> .82f
    }
    val active = voiceState == "wake" || voiceState == "command" || voiceState == "speaking"
    val alpha = if (active) {
        val transition = rememberInfiniteTransition(label = "zenoDefaultDot")
        val animated by transition.animateFloat(
            initialValue = (targetAlpha * .62f).coerceAtLeast(.35f),
            targetValue = targetAlpha,
            animationSpec = infiniteRepeatable(
                tween(if (voiceState == "command") 360 else 760),
                RepeatMode.Reverse
            ),
            label = "zenoDefaultDotAlpha"
        )
        animated
    } else targetAlpha

    Box(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = 10.dp)
            .size(62.dp)
            .zIndex(100f)
            .clickable { startVoiceCommand(context) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(if (voiceState == "command") 36.dp else 30.dp)
                .background(color.copy(alpha = alpha), CircleShape)
        )
    }
}

'''
main, count = indicator_pattern.subn(indicator, main, count=1)
if count != 1:
    raise SystemExit('Voyant accueil final introuvable')

# Garder la personnalisation du thème, mais supprimer tous les réglages du voyant.
custom_pattern = re.compile(
    r'@Composable\nprivate fun CustomizeScreen\(.*?\n\}\n\n(?=@Composable\nprivate fun CommunityScreen)',
    re.S,
)
custom = r'''@Composable
private fun CustomizeScreen(current: ZenoTheme, accent: Color, onTheme: (ZenoTheme) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    val micGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var enabled by remember { mutableStateOf(prefs.getBoolean("voice_enabled", micGranted)) }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enabled = true
            prefs.edit().putBoolean("voice_enabled", true).apply()
            runCatching { startVoiceZeno(context) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Thème de l'application", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        items(ZenoTheme.entries) { theme ->
            Surface(
                Modifier.fillMaxWidth().clickable { onTheme(theme) },
                color = if (theme == current) Color(theme.accent).copy(alpha = .20f) else Color(0xFF0A1B3B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(theme.accent))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(theme.accent)))
                    Spacer(Modifier.width(12.dp))
                    Text(theme.label, color = Color.White, modifier = Modifier.weight(1f))
                    if (theme == current) Icon(Icons.Default.CheckCircle, null, tint = Color(theme.accent))
                }
            }
        }
        item {
            Text("Écoute vocale directe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Dis simplement « Zeno ». Les voyants utilisent maintenant leur configuration par défaut.", color = Color(0xFF9AB4D8))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (enabled) "Réveil vocal actif" else "Réveil vocal désactivé", color = Color.White, modifier = Modifier.weight(1f))
                Switch(enabled, { on ->
                    if (on) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            enabled = true
                            prefs.edit().putBoolean("voice_enabled", true).apply()
                            runCatching { startVoiceZeno(context) }
                        } else {
                            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        enabled = false
                        prefs.edit().putBoolean("voice_enabled", false).apply()
                        context.stopService(Intent(context, FloatingZenoService::class.java))
                    }
                })
            }
        }
    }
}

'''
main, count = custom_pattern.subn(custom, main, count=1)
if count != 1:
    raise SystemExit('Écran Personnalisation introuvable')

# Le menu Accueil conserve les permissions utiles : remettre la superposition pour
# le voyant du tiroir si la ligne avait été masquée en v1.3.20.
permission_start = main.find('@Composable\nprivate fun PermissionControlCard(accent: Color)')
permission_end = main.find('@Composable\nprivate fun ActionGrid', permission_start)
if permission_start >= 0 and permission_end > permission_start:
    card = main[permission_start:permission_end]
    card = card.replace(
        '"Active le micro pour la reconnaissance vocale Zeno."',
        '"Active le micro et la superposition pour les deux voyants Zeno."',
    )
    if 'Superposition sur les autres applis' not in card:
        overlay_row = r'''            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, null, tint = if (overlayGranted) Color(0xFF34D399) else Color(0xFFFFA24A))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Superposition sur les autres applis", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (overlayGranted) "Autorisée : voyant du tiroir actif" else "À autoriser pour le voyant du tiroir",
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
                        } else if (micGranted) {
                            activateZeno()
                        }
                    }
                ) { Text(if (overlayGranted) "Actif" else "Autoriser") }
            }

'''
        marker_text = 'Text("Modifier les réglages système"'
        marker_index = card.find(marker_text)
        insert_at = -1
        if marker_index >= 0:
            insert_at = card.rfind(
                '            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {',
                0,
                marker_index,
            )
        if insert_at < 0:
            marker_index = card.find('Text("Activer Zeno + voyant")')
            if marker_index >= 0:
                insert_at = card.rfind('            Button(', 0, marker_index)
        if insert_at < 0:
            raise SystemExit('Emplacement ligne superposition introuvable')
        card = card[:insert_at] + overlay_row + card[insert_at:]
    main = main[:permission_start] + card + main[permission_end:]

main = main.replace('Thème et petit rond Zeno', "Thème de l'application")
main = main.replace('Micro Android, sans superposition', 'Micro Android et voyant du tiroir')
main = main.replace('Zeno Android v1.3.21', 'Zeno Android v1.3.22')
main = main.replace('Version 1.3.21', 'Version 1.3.22')
MAIN.write_text(main, encoding='utf-8')

# -----------------------------------------------------------------------------
# Manifest : remettre la permission du voyant de tiroir/superposition.
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
# Service : voyant de tiroir par défaut + suppression du bip répétitif.
# -----------------------------------------------------------------------------
service = SERVICE.read_text(encoding='utf-8')

# Restaurer le voyant global au démarrage du service.
def replace_in_block(source: str, start_marker: str, end_marker: str, old: str, new: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + 1)
    if start < 0 or end < 0:
        raise SystemExit(f'Bloc introuvable: {start_marker}')
    block = source[start:end]
    if old not in block:
        raise SystemExit(f'Valeur introuvable dans bloc: {old.strip()}')
    block = block.replace(old, new, 1)
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

# Le voyant du tiroir suit l'état vocal au lieu d'être supprimé à chaque changement.
broadcast_start = service.find('    private fun broadcastState(state: String) {')
if broadcast_start < 0:
    raise SystemExit('broadcastState introuvable')
broadcast_end = service.find('\n    private fun ', broadcast_start + 10)
if broadcast_end < 0:
    raise SystemExit('Fin broadcastState introuvable')
broadcast_block = service[broadcast_start:broadcast_end]
broadcast_block = broadcast_block.replace(
    '        if (overlayDot != null) removeOverlayDot()\n',
    '        updateOverlayDot(state)\n',
    1,
)
service = service[:broadcast_start] + broadcast_block + service[broadcast_end:]

# Voyant de tiroir fixe, non tactile et sans préférences de position/couleur.
show_pattern = re.compile(
    r'    private fun showOverlayDot\(\) \{.*?\n    \}\n\n(?=    private fun (?:applyOverlayPosition|updateOverlayDot))',
    re.S,
)
show_overlay = r'''    private fun showOverlayDot() {
        if (!Settings.canDrawOverlays(this)) return
        val wm = runCatching { getSystemService(WINDOW_SERVICE) as WindowManager }.getOrNull() ?: return
        val density = resources.displayMetrics.density
        val edge = (12f * density).toInt().coerceAtLeast(8)

        val existing = overlayDot
        if (existing != null) {
            val params = existing.layoutParams as? WindowManager.LayoutParams ?: return
            params.gravity = Gravity.TOP or Gravity.END
            params.x = edge
            params.y = edge
            runCatching { wm.updateViewLayout(existing, params) }
            overlayManager = wm
            return
        }

        val size = (13f * density).toInt().coerceAtLeast(10)
        val dot = View(this).apply {
            contentDescription = "Voyant Zeno tiroir"
            alpha = .86f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF168CFF.toInt())
                setStroke((1f * density).toInt().coerceAtLeast(1), 0x99FFFFFF.toInt())
            }
        }
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = edge
            y = edge
        }
        runCatching { wm.addView(dot, params) }
            .onSuccess {
                overlayManager = wm
                overlayDot = dot
            }
    }

'''
service, count = show_pattern.subn(show_overlay, service, count=1)
if count != 1:
    raise SystemExit('showOverlayDot introuvable')

update_pattern = re.compile(
    r'    private fun updateOverlayDot\(state: String\) \{.*?\n    \}\n\n(?=    private fun removeOverlayDot)',
    re.S,
)
update_overlay = r'''    private fun updateOverlayDot(state: String) {
        if (overlayDot == null && Settings.canDrawOverlays(this)) showOverlayDot()
        val dot = overlayDot ?: return
        val color = when (state) {
            "command" -> 0xFF9B5CFF.toInt()
            "speaking" -> 0xFFC084FC.toInt()
            "permission", "error" -> 0xFFFF7043.toInt()
            "off" -> 0xFF607D8B.toInt()
            else -> 0xFF168CFF.toInt()
        }
        (dot.background as? GradientDrawable)?.setColor(color)
        dot.animate()
            .alpha(if (state == "command") 1f else .84f)
            .scaleX(if (state == "command") 1.35f else 1f)
            .scaleY(if (state == "command") 1.35f else 1f)
            .setDuration(160)
            .start()
    }

'''
service, count = update_pattern.subn(update_overlay, service, count=1)
if count != 1:
    raise SystemExit('updateOverlayDot introuvable')

# Le problème du bip venait surtout des redémarrages successifs du SpeechRecognizer.
# Au lieu de couper/restaurer musique + notifications toutes les 1-2 secondes,
# garder uniquement STREAM_SYSTEM silencieux pendant une session d'écoute et le
# restaurer dès qu'une commande est terminée ou que le service s'arrête.
service = service.replace(
    '    private var beepRestore: Runnable? = null\n    private val beepOriginalVolumes = mutableMapOf<Int, Int>()\n',
    '    private var recognizerSystemVolume: Int? = null\n    private var recognizerSystemWasMuted: Boolean = false\n',
    1,
)

helper_pattern = re.compile(
    r'''    private fun quietRecognizerBeep\(durationMs: Long = \d+L\) \{.*?\n    \}\n\n    private fun restoreRecognizerAudio\(\) \{.*?\n    \}\n\n''',
    re.S,
)
helpers = '''    private fun quietRecognizerBeep(durationMs: Long = 1800L) {
        if (durationMs >= 0L) holdRecognizerAudio()
    }

    private fun holdRecognizerAudio() {
        val audio = runCatching { getSystemService(AUDIO_SERVICE) as AudioManager }.getOrNull() ?: return
        if (recognizerSystemVolume == null) {
            recognizerSystemVolume = audio.getStreamVolume(AudioManager.STREAM_SYSTEM)
            recognizerSystemWasMuted = audio.isStreamMute(AudioManager.STREAM_SYSTEM)
        }
        runCatching {
            audio.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            audio.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
        }
    }

    private fun restoreRecognizerAudio() {
        val original = recognizerSystemVolume ?: return
        val audio = runCatching { getSystemService(AUDIO_SERVICE) as AudioManager }.getOrNull()
        if (audio != null) {
            runCatching {
                audio.setStreamVolume(AudioManager.STREAM_SYSTEM, original, 0)
                audio.adjustStreamVolume(
                    AudioManager.STREAM_SYSTEM,
                    if (recognizerSystemWasMuted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
        }
        recognizerSystemVolume = null
        recognizerSystemWasMuted = false
    }

'''
service, count = helper_pattern.subn(helpers, service, count=1)
if count != 1:
    raise SystemExit('Gestion bip v1.3.21 introuvable')

# Préférer le moteur vocal local Android quand il existe : moins de latence et
# moins de recréations audio sur de nombreux appareils.
if 'import android.os.Build\n' not in service:
    service = service.replace('import android.os.Bundle\n', 'import android.os.Build\nimport android.os.Bundle\n', 1)

recognizer_pattern = re.compile(
    r'''    private fun createRecognizer\(listener: RecognitionListener\): SpeechRecognizer\? = runCatching \{\n        SpeechRecognizer\.createSpeechRecognizer\(this\)\.apply \{ setRecognitionListener\(listener\) \}\n    \}\.getOrNull\(\)\n'''
)
recognizer_replacement = '''    private fun createRecognizer(listener: RecognitionListener): SpeechRecognizer? = runCatching {
        val engine = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }
        engine.apply { setRecognitionListener(listener) }
    }.getOrNull()
'''
service, _ = recognizer_pattern.subn(recognizer_replacement, service, count=1)

# Une session de réveil plus longue limite les redémarrages et donc les bips.
service = service.replace(
    'current.startListening(recognitionIntent(partial = true, longWindow = false))',
    'current.startListening(recognitionIntent(partial = true, longWindow = true))',
    1,
)
service = service.replace(
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 220L',
    'SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 900L',
    1,
)
service = service.replace(
    'else -> handler.postDelayed({ startWakeListening() }, 200)',
    'else -> handler.postDelayed({ startWakeListening() }, 650)',
    1,
)

# Le recognizer est détruit pendant que le flux système est silencieux, puis le
# volume est restauré avant la réponse de Zeno.
service = service.replace(
    '    private fun executeCandidates(candidates: List<String>) {\n        invalidateRecognizer()\n',
    '    private fun executeCandidates(candidates: List<String>) {\n        invalidateRecognizer()\n        restoreRecognizerAudio()\n',
    1,
)

SERVICE.write_text(service, encoding='utf-8')
print('Zeno 1.3.22 : voyants Accueil + tiroir par défaut, sans personnalisation voyant, réveil vocal silencieux stabilisé')
