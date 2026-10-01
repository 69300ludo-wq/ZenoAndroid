from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

# --- Service : le voyant garde son apparence, mais sa position vient des préférences de l'accueil.
service = SERVICE.read_text(encoding='utf-8')
pattern = re.compile(
    r'    private fun showOverlayDot\(\) \{.*?\n    \}\n\n    private fun updateOverlayDot\(state: String\) \{',
    re.S,
)
replacement = r'''    private fun showOverlayDot() {
        if (!Settings.canDrawOverlays(this)) return
        val wm = runCatching {
            getSystemService(WINDOW_SERVICE) as WindowManager
        }.getOrNull() ?: return

        val density = resources.displayMetrics.density
        val prefs = getSharedPreferences("zeno_indicator", MODE_PRIVATE)
        val position = prefs.getString("indicator_position", "top_center") ?: "top_center"

        val existing = overlayDot
        if (existing != null) {
            val params = existing.layoutParams as? WindowManager.LayoutParams ?: return
            applyOverlayPosition(params, position, density)
            runCatching { wm.updateViewLayout(existing, params) }
            overlayManager = wm
            return
        }

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
        )
        applyOverlayPosition(params, position, density)

        runCatching { wm.addView(dot, params) }
            .onSuccess {
                overlayManager = wm
                overlayDot = dot
            }
    }

    private fun applyOverlayPosition(
        params: WindowManager.LayoutParams,
        position: String,
        density: Float
    ) {
        val edge = (10f * density).toInt().coerceAtLeast(6)
        params.x = 0
        params.y = 0
        params.gravity = when (position) {
            "top_left" -> Gravity.TOP or Gravity.START
            "top_right" -> Gravity.TOP or Gravity.END
            "middle_left" -> Gravity.CENTER_VERTICAL or Gravity.START
            "middle_center" -> Gravity.CENTER
            "middle_right" -> Gravity.CENTER_VERTICAL or Gravity.END
            "bottom_left" -> Gravity.BOTTOM or Gravity.START
            "bottom_center" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            "bottom_right" -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        if (position.startsWith("top_") || position.startsWith("bottom_")) params.y = edge
        if (position.endsWith("_left") || position.endsWith("_right")) params.x = edge
    }

    private fun updateOverlayDot(state: String) {'''
service, count = pattern.subn(replacement, service, count=1)
if count != 1:
    raise SystemExit('Fonction showOverlayDot finale introuvable')
SERVICE.write_text(service, encoding='utf-8')

# --- Accueil : carte de réglage de la position du voyant, avec 9 positions.
main = MAIN.read_text(encoding='utf-8')
permission_item = '            item { Spacer(Modifier.height(12.dp)); PermissionControlCard(accent) }\n'
position_item = '            item { Spacer(Modifier.height(12.dp)); IndicatorPositionCard(accent) }\n'
if 'IndicatorPositionCard(accent)' not in main:
    if permission_item not in main:
        raise SystemExit('Carte permissions accueil introuvable')
    main = main.replace(permission_item, permission_item + position_item, 1)

if 'private fun IndicatorPositionCard(accent: Color)' not in main:
    anchor = '@Composable\nprivate fun ActionGrid(items: List<HomeAction>, navigate: (Screen) -> Unit) {'
    function = r'''@Composable
private fun IndicatorPositionCard(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    var selected by remember {
        mutableStateOf(prefs.getString("indicator_position", "top_center") ?: "top_center")
    }

    val positions = listOf(
        Triple("top_left", "Haut gauche", "↖"),
        Triple("top_center", "Haut centre", "↑"),
        Triple("top_right", "Haut droite", "↗"),
        Triple("middle_left", "Milieu gauche", "←"),
        Triple("middle_center", "Centre", "•"),
        Triple("middle_right", "Milieu droite", "→"),
        Triple("bottom_left", "Bas gauche", "↙"),
        Triple("bottom_center", "Bas centre", "↓"),
        Triple("bottom_right", "Bas droite", "↘")
    )

    fun selectPosition(id: String) {
        selected = id
        prefs.edit { putString("indicator_position", id) }
        runCatching { startVoiceZeno(context) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE60A1835),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).background(Color(0xFF168CFF), CircleShape)
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Position du voyant Zeno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Choisis où le petit voyant apparaît sur ton écran. Le changement est appliqué immédiatement.",
                        color = Color(0xFFAFC3E8),
                        fontSize = 11.sp
                    )
                }
            }

            positions.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (id, label, arrow) ->
                        OutlinedButton(
                            onClick = { selectPosition(id) },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selected == id) 2.dp else 1.dp,
                                if (selected == id) Color(0xFF24E6FF) else Color(0xFF5A6D91)
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(arrow, color = if (selected == id) Color(0xFF24E6FF) else Color.White, fontSize = 18.sp)
                                Text(label, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }
            }

            val currentLabel = positions.firstOrNull { it.first == selected }?.second ?: "Haut centre"
            Text(
                "Position actuelle : $currentLabel",
                color = Color(0xFF9DB3DA),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

'''
    if anchor not in main:
        raise SystemExit('Point insertion réglage position introuvable')
    main = main.replace(anchor, function + anchor, 1)

main = main.replace('Zeno Android v1.3.14', 'Zeno Android v1.3.15')
main = main.replace('Version 1.3.14', 'Version 1.3.15')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.15 : position du voyant personnalisable depuis l accueil, 9 positions')
