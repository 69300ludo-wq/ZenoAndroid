from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

# --- Accueil : le petit voyant se déplace directement au doigt, sans passer par un menu.
main = MAIN.read_text(encoding='utf-8')

imports = {
    'import androidx.compose.foundation.gestures.detectDragGestures\n': 'import androidx.compose.foundation.Image\n',
    'import androidx.compose.ui.input.pointer.pointerInput\n': 'import androidx.compose.ui.zIndex\n',
    'import androidx.compose.ui.unit.IntOffset\n': 'import androidx.compose.ui.unit.sp\n',
    'import kotlin.math.roundToInt\n': 'import java.util.Locale\n',
}
for line, anchor in imports.items():
    if line not in main:
        if anchor not in main:
            raise SystemExit(f'Import anchor introuvable pour {line.strip()}')
        main = main.replace(anchor, anchor + line, 1)

main = main.replace('            VoiceStatusDot()\n', '            VoiceStatusDot(draggable = screen == Screen.HOME)\n', 1)
# Plus de carte/menu pour choisir une position : tout se fait directement sur le voyant.
main = main.replace('            item { Spacer(Modifier.height(12.dp)); IndicatorPositionCard(accent) }\n', '', 1)

pattern = re.compile(
    r'@Composable\nprivate fun BoxScope\.VoiceStatusDot\([^)]*\) \{.*?\n\}\n\n',
    re.S,
)
# Compatibilité avec l'ancienne signature sans paramètre.
if not pattern.search(main):
    pattern = re.compile(
        r'@Composable\nprivate fun BoxScope\.VoiceStatusDot\(\) \{.*?\n\}\n\n',
        re.S,
    )

indicator = r'''@Composable
private fun BoxScope.VoiceStatusDot(draggable: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    var revision by remember { mutableIntStateOf(0) }
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

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision += 1 }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    if (revision < 0) return

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val visualSize = prefs.getInt("indicator_size", 10).coerceIn(7, 18)
    val touchSize = 48f
    val maxX = (screenWidthDp - touchSize).coerceAtLeast(0f)
    val maxY = (screenHeightDp - touchSize - 24f).coerceAtLeast(0f)

    val storedX = prefs.getFloat("indicator_free_x_dp", Float.NaN)
    val storedY = prefs.getFloat("indicator_free_y_dp", Float.NaN)
    var dotX by remember(screenWidthDp) {
        mutableFloatStateOf(if (storedX.isFinite()) storedX.coerceIn(0f, maxX) else (maxX / 2f))
    }
    var dotY by remember(screenHeightDp) {
        mutableFloatStateOf(if (storedY.isFinite()) storedY.coerceIn(0f, maxY) else 4f)
    }

    val opacity = prefs.getFloat("indicator_opacity", .92f).coerceIn(.35f, 1f)
    val selectedColor = Color(prefs.getLong("indicator_color", 0xFF168CFFL))
    val userPulse = prefs.getBoolean("indicator_pulse", true)

    // Bleu = Zeno attend / entend « Zeno ouvre ».
    // Violet = la phrase a été détectée et Zeno attend maintenant la demande.
    val baseColor = when (voiceState) {
        "wake" -> Color(0xFF168CFF)
        "command" -> Color(0xFF9B5CFF)
        "speaking" -> Color(0xFFC084FC)
        "error", "permission" -> Color(0xFFFF7043)
        "off" -> Color(0xFF607D8B)
        else -> selectedColor
    }
    val targetOpacity = when (voiceState) {
        "command" -> 1f
        "wake" -> opacity
        "speaking" -> .88f
        "error", "permission" -> .92f
        "off" -> .38f
        else -> .62f
    }
    val active = voiceState == "wake" || voiceState == "command" || voiceState == "speaking"
    val alpha = if (userPulse && active) {
        val transition = rememberInfiniteTransition(label = "zenoVoiceDot")
        val animated by transition.animateFloat(
            initialValue = (targetOpacity * .58f).coerceAtLeast(.30f),
            targetValue = targetOpacity,
            animationSpec = infiniteRepeatable(
                tween(if (voiceState == "command") 380 else 720),
                RepeatMode.Reverse
            ),
            label = "zenoVoiceDotAlpha"
        )
        animated
    } else targetOpacity

    fun saveFreePosition() {
        prefs.edit {
            putString("indicator_position", "free")
            putFloat("indicator_free_x_dp", dotX)
            putFloat("indicator_free_y_dp", dotY)
        }
        val updateIntent = Intent(
            context,
            FloatingZenoService::class.java
        ).apply {
            action = FloatingZenoService.ACTION_UPDATE_INDICATOR_POSITION
            putExtra(FloatingZenoService.EXTRA_INDICATOR_POSITION, "free")
            putExtra(FloatingZenoService.EXTRA_INDICATOR_X_DP, dotX)
            putExtra(FloatingZenoService.EXTRA_INDICATOR_Y_DP, dotY)
        }
        runCatching { ContextCompat.startForegroundService(context, updateIntent) }
    }

    val dragModifier = if (draggable) {
        Modifier.pointerInput(screenWidthDp, screenHeightDp, visualSize) {
            detectDragGestures(
                onDragEnd = { saveFreePosition() },
                onDragCancel = { saveFreePosition() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / density.density
                    val dy = dragAmount.y / density.density
                    dotX = (dotX + dx).coerceIn(0f, maxX)
                    dotY = (dotY + dy).coerceIn(0f, maxY)
                }
            )
        }
    } else Modifier

    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    (dotX * density.density).roundToInt(),
                    (dotY * density.density).roundToInt()
                )
            }
            .size(touchSize.dp)
            .zIndex(100f)
            .then(dragModifier)
            .clickable { startVoiceCommand(context) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size((visualSize + if (voiceState == "command") 3 else 0).dp)
                .background(baseColor.copy(alpha = alpha), CircleShape)
        )
    }
}

'''
main, count = pattern.subn(indicator, main, count=1)
if count != 1:
    raise SystemExit('VoiceStatusDot final introuvable')

main = main.replace('Zeno Android v1.3.16', 'Zeno Android v1.3.17')
main = main.replace('Version 1.3.16', 'Version 1.3.17')
MAIN.write_text(main, encoding='utf-8')

# --- Service global : synchroniser la position libre et les couleurs bleu/violet.
service = SERVICE.read_text(encoding='utf-8')
old_update_action = '''        if (intent?.action == ACTION_UPDATE_INDICATOR_POSITION) {
            val requested = intent.getStringExtra(EXTRA_INDICATOR_POSITION)
            if (!requested.isNullOrBlank()) {
                getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit {
                    putString("indicator_position", requested)
                }
            }
            showOverlayDot()
            return START_STICKY
        }
'''
new_update_action = '''        if (intent?.action == ACTION_UPDATE_INDICATOR_POSITION) {
            val requested = intent.getStringExtra(EXTRA_INDICATOR_POSITION)
            val xDp = intent.getFloatExtra(EXTRA_INDICATOR_X_DP, Float.NaN)
            val yDp = intent.getFloatExtra(EXTRA_INDICATOR_Y_DP, Float.NaN)
            getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit {
                if (!requested.isNullOrBlank()) putString("indicator_position", requested)
                if (xDp.isFinite()) putFloat("indicator_free_x_dp", xDp)
                if (yDp.isFinite()) putFloat("indicator_free_y_dp", yDp)
            }
            showOverlayDot()
            return START_STICKY
        }
'''
if old_update_action not in service:
    raise SystemExit('Action mise à jour position v1.3.16 introuvable')
service = service.replace(old_update_action, new_update_action, 1)

old_position = '''        val position = prefs.getString("indicator_position", "top_center") ?: "top_center"

        val existing = overlayDot
        if (existing != null) {
            val params = existing.layoutParams as? WindowManager.LayoutParams ?: return
            applyOverlayPosition(params, position, density)
'''
new_position = '''        val position = prefs.getString("indicator_position", "top_center") ?: "top_center"
        val freeX = prefs.getFloat("indicator_free_x_dp", 0f)
        val freeY = prefs.getFloat("indicator_free_y_dp", 0f)

        val existing = overlayDot
        if (existing != null) {
            val params = existing.layoutParams as? WindowManager.LayoutParams ?: return
            applyOverlayPosition(params, position, density, freeX, freeY)
'''
if old_position not in service:
    raise SystemExit('Position overlay existante introuvable')
service = service.replace(old_position, new_position, 1)
service = service.replace('        applyOverlayPosition(params, position, density)\n', '        applyOverlayPosition(params, position, density, freeX, freeY)\n', 1)

old_signature = '''    private fun applyOverlayPosition(
        params: WindowManager.LayoutParams,
        position: String,
        density: Float
    ) {
'''
new_signature = '''    private fun applyOverlayPosition(
        params: WindowManager.LayoutParams,
        position: String,
        density: Float,
        freeX: Float,
        freeY: Float
    ) {
'''
if old_signature not in service:
    raise SystemExit('Signature applyOverlayPosition introuvable')
service = service.replace(old_signature, new_signature, 1)

old_gravity = '''        params.gravity = when (position) {
            "top_left" -> Gravity.TOP or Gravity.START
'''
new_gravity = '''        if (position == "free") {
            params.gravity = Gravity.TOP or Gravity.START
            params.x = (freeX * density).toInt().coerceAtLeast(0)
            params.y = (freeY * density).toInt().coerceAtLeast(0)
            return
        }
        params.gravity = when (position) {
            "top_left" -> Gravity.TOP or Gravity.START
'''
if old_gravity not in service:
    raise SystemExit('Gravity overlay introuvable')
service = service.replace(old_gravity, new_gravity, 1)

service = service.replace('''            "command" -> 0xFF24E6FF.toInt()
            "speaking" -> 0xFF9B5CFF.toInt()
''', '''            "wake" -> 0xFF168CFF.toInt()
            "command" -> 0xFF9B5CFF.toInt()
            "speaking" -> 0xFFC084FC.toInt()
''', 1)

old_extras = '''        const val EXTRA_INDICATOR_POSITION = "indicator_position"
        const val EXTRA_STATE = "voice_state"
'''
new_extras = '''        const val EXTRA_INDICATOR_POSITION = "indicator_position"
        const val EXTRA_INDICATOR_X_DP = "indicator_x_dp"
        const val EXTRA_INDICATOR_Y_DP = "indicator_y_dp"
        const val EXTRA_STATE = "voice_state"
'''
if old_extras not in service:
    raise SystemExit('Extras position service introuvables')
service = service.replace(old_extras, new_extras, 1)
SERVICE.write_text(service, encoding='utf-8')

print('Zeno 1.3.17 : voyant accueil glissable au doigt + bleu réveil / violet commande')
