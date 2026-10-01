from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# --- Manifest : restaurer l'autorisation spéciale de superposition supprimée par la v1.3.9.
manifest = MANIFEST.read_text(encoding='utf-8')
perm = '    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n'
if 'android.permission.SYSTEM_ALERT_WINDOW' not in manifest:
    anchor = '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'
    if anchor not in manifest:
        raise SystemExit('Permission RECORD_AUDIO introuvable dans le manifeste')
    manifest = manifest.replace(anchor, anchor + perm, 1)
MANIFEST.write_text(manifest, encoding='utf-8')

# --- Service : petit voyant global non tactile au-dessus des autres applications.
service = SERVICE.read_text(encoding='utf-8')

imports_anchor = 'import android.content.pm.PackageManager\n'
imports = '''import android.content.pm.PackageManager\nimport android.graphics.PixelFormat\nimport android.graphics.drawable.GradientDrawable\nimport android.provider.Settings\nimport android.view.Gravity\nimport android.view.View\nimport android.view.WindowManager\n'''
if 'import android.view.WindowManager' not in service:
    if imports_anchor not in service:
        raise SystemExit('Imports du service introuvables')
    service = service.replace(imports_anchor, imports, 1)

field_anchor = '    private lateinit var brain: ZenoBrain\n'
fields = '''    private lateinit var brain: ZenoBrain\n    private var overlayManager: WindowManager? = null\n    private var overlayDot: View? = null\n'''
if 'private var overlayDot:' not in service:
    if field_anchor not in service:
        raise SystemExit('Champs du service introuvables')
    service = service.replace(field_anchor, fields, 1)

# Afficher le voyant après que le service de premier plan a démarré.
if '        showOverlayDot()\n\n        tts = runCatching {' not in service:
    marker = '        tts = runCatching {\n'
    if marker not in service:
        raise SystemExit('Initialisation TTS introuvable')
    service = service.replace(marker, '        showOverlayDot()\n\n' + marker, 1)

# Au retour depuis l'écran Android de permission, un nouveau démarrage du service doit créer le voyant.
onstart_marker = '''        if (intent?.action == ACTION_DIRECT_COMMAND) {\n'''
if '        showOverlayDot()\n\n        if (intent?.action == ACTION_DIRECT_COMMAND)' not in service:
    if onstart_marker not in service:
        raise SystemExit('onStartCommand final introuvable')
    service = service.replace(onstart_marker, '        showOverlayDot()\n\n' + onstart_marker, 1)

# Le voyant suit l'état réel du moteur vocal.
if '        updateOverlayDot(state)\n' not in service:
    state_marker = '''    private fun broadcastState(state: String) {\n        runCatching {\n'''
    state_replacement = '''    private fun broadcastState(state: String) {\n        updateOverlayDot(state)\n        runCatching {\n'''
    if state_marker not in service:
        raise SystemExit('broadcastState introuvable')
    service = service.replace(state_marker, state_replacement, 1)

if 'private fun showOverlayDot()' not in service:
    methods_anchor = '    override fun onDestroy() {\n'
    methods = r'''    private fun showOverlayDot() {
        if (!Settings.canDrawOverlays(this) || overlayDot != null) return
        val wm = runCatching {
            getSystemService(WINDOW_SERVICE) as WindowManager
        }.getOrNull() ?: return

        val density = resources.displayMetrics.density
        val size = (11f * density).toInt().coerceAtLeast(9)
        val dot = View(this).apply {
            contentDescription = "Voyant Zeno"
            alpha = .88f
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (7f * density).toInt()
        }

        runCatching { wm.addView(dot, params) }
            .onSuccess {
                overlayManager = wm
                overlayDot = dot
            }
    }

    private fun updateOverlayDot(state: String) {
        if (overlayDot == null && Settings.canDrawOverlays(this)) showOverlayDot()
        val dot = overlayDot ?: return
        val color = when (state) {
            "command" -> 0xFF24E6FF.toInt()
            "speaking" -> 0xFF9B5CFF.toInt()
            "permission", "error" -> 0xFFFF7043.toInt()
            "off" -> 0xFF607D8B.toInt()
            else -> 0xFF168CFF.toInt()
        }
        val bg = dot.background as? GradientDrawable
        bg?.setColor(color)
        dot.animate()
            .alpha(if (state == "command") 1f else .82f)
            .scaleX(if (state == "command") 1.35f else 1f)
            .scaleY(if (state == "command") 1.35f else 1f)
            .setDuration(160)
            .start()
    }

    private fun removeOverlayDot() {
        val dot = overlayDot
        val wm = overlayManager
        overlayDot = null
        overlayManager = null
        if (dot != null && wm != null) runCatching { wm.removeView(dot) }
    }

'''
    if methods_anchor not in service:
        raise SystemExit('onDestroy du service introuvable')
    service = service.replace(methods_anchor, methods + methods_anchor, 1)

if '        removeOverlayDot()\n' not in service:
    destroy_marker = '''    override fun onDestroy() {\n        handler.removeCallbacksAndMessages(null)\n'''
    destroy_replacement = '''    override fun onDestroy() {\n        handler.removeCallbacksAndMessages(null)\n        removeOverlayDot()\n'''
    if destroy_marker not in service:
        raise SystemExit('Début onDestroy introuvable')
    service = service.replace(destroy_marker, destroy_replacement, 1)

service = service.replace('Service vocal Zeno sans overlay.', 'Service vocal Zeno avec voyant de superposition optionnel.')
SERVICE.write_text(service, encoding='utf-8')

# --- Accueil : menu visible pour demander directement les deux autorisations.
main = MAIN.read_text(encoding='utf-8')
if 'import androidx.core.content.edit\n' not in main:
    anchor = 'import androidx.core.content.ContextCompat\n'
    if anchor not in main:
        raise SystemExit('Import ContextCompat introuvable')
    main = main.replace(anchor, anchor + 'import androidx.core.content.edit\n', 1)

home_anchor = '            item { ActionGrid(actions, navigate) }\n'
if 'PermissionControlCard(accent)' not in main:
    if home_anchor not in main:
        raise SystemExit('ActionGrid accueil introuvable')
    main = main.replace(
        home_anchor,
        home_anchor + '            item { Spacer(Modifier.height(12.dp)); PermissionControlCard(accent) }\n',
        1
    )

if 'private fun PermissionControlCard(accent: Color)' not in main:
    function_anchor = '@Composable\nprivate fun ActionGrid(items: List<HomeAction>, navigate: (Screen) -> Unit) {'
    permission_function = r'''@Composable
private fun PermissionControlCard(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }

    fun hasMicrophone(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasOverlay(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    var micGranted by remember { mutableStateOf(hasMicrophone()) }
    var overlayGranted by remember { mutableStateOf(hasOverlay()) }

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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE60A1835),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (micGranted && overlayGranted) accent else Color(0xFFFFA24A)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(13.dp).background(
                        if (micGranted && overlayGranted) Color(0xFF24E6FF) else Color(0xFFFFA24A),
                        CircleShape
                    )
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Autorisations Zeno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Active le micro et le petit voyant au-dessus des autres applis.",
                        color = Color(0xFFAFC3E8),
                        fontSize = 11.sp
                    )
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
                    Text("Superposition sur les autres applis", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (overlayGranted) "Autorisée : le voyant peut rester visible" else "À autoriser pour le voyant global",
                        color = Color(0xFF9DB3DA),
                        fontSize = 11.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (!overlayGranted) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            runCatching { overlayPermission.launch(intent) }
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
                Text("Activer Zeno + voyant")
            }

            TextButton(
                onClick = {
                    appSettings.launch(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ouvrir les réglages Android de Zeno")
            }
        }
    }
}

'''
    if function_anchor not in main:
        raise SystemExit('Fonction ActionGrid introuvable')
    main = main.replace(function_anchor, permission_function + function_anchor, 1)

main = main.replace('Zeno Android v1.3.12', 'Zeno Android v1.3.13')
main = main.replace('Version 1.3.12', 'Version 1.3.13')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.13 : menu permissions accueil + permission superposition + voyant global')
