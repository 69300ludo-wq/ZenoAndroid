from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/VoiceCommandActivity.kt')
s = p.read_text(encoding='utf-8')

state_old = '''    private var statusText by mutableStateOf("Préparation de Zeno…")
    private var heardText by mutableStateOf("")
'''
state_new = '''    private var statusText by mutableStateOf("Préparation de Zeno…")
    private var heardText by mutableStateOf("")
    private var voiceLevel by mutableStateOf(0f)
    private var voiceDetected by mutableStateOf(false)
'''
if state_old not in s:
    raise SystemExit('Etat vocal introuvable')
s = s.replace(state_old, state_new, 1)

glow_old = '''                        Box(
                            modifier = Modifier
                                .size(270.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0x6632E6FF), Color.Transparent)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
'''
glow_new = '''                        Box(
                            modifier = Modifier
                                .size((270f + (voiceLevel * 52f)).dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF32E6FF).copy(
                                                alpha = if (voiceDetected) 0.28f + (voiceLevel * 0.55f) else 0.12f
                                            ),
                                            Color(0xFF7C4DFF).copy(
                                                alpha = if (voiceDetected) 0.16f + (voiceLevel * 0.30f) else 0.05f
                                            ),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
'''
if glow_old not in s:
    raise SystemExit('Halo Zeno introuvable')
s = s.replace(glow_old, glow_new, 1)

s = s.replace(
'''        handled = false
        statusText = "Je t’écoute…"
        heardText = ""
''',
'''        handled = false
        statusText = "Je t’écoute…"
        heardText = ""
        voiceDetected = false
        voiceLevel = 0.08f
''',
1
)

listener_old = '''                override fun onReadyForSpeech(params: Bundle?) {
                    statusText = "Je t’écoute…"
                }

                override fun onBeginningOfSpeech() {
                    statusText = "Je t’écoute…"
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
'''
listener_new = '''                override fun onReadyForSpeech(params: Bundle?) {
                    statusText = "Je t’écoute…"
                    voiceDetected = false
                    voiceLevel = 0.10f
                }

                override fun onBeginningOfSpeech() {
                    statusText = "Voix détectée"
                    voiceDetected = true
                    voiceLevel = 0.55f
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    voiceLevel = if (voiceDetected) level.coerceAtLeast(0.18f) else level * 0.35f
                    if (level > 0.18f) {
                        voiceDetected = true
                        statusText = "Voix détectée"
                    }
                }
'''
if listener_old not in s:
    raise SystemExit('Ecouteur vocal introuvable')
s = s.replace(listener_old, listener_new, 1)

s = s.replace(
'''                override fun onEndOfSpeech() {
                    statusText = "Je réfléchis…"
                }
''',
'''                override fun onEndOfSpeech() {
                    statusText = "Je réfléchis…"
                    voiceLevel = 0.25f
                }
''',
1
)

s = s.replace(
'''                override fun onError(error: Int) {
                    if (handled) return
                    handled = true
                    statusText = when (error) {
''',
'''                override fun onError(error: Int) {
                    if (handled) return
                    handled = true
                    voiceDetected = false
                    voiceLevel = 0f
                    statusText = when (error) {
''',
1
)

s = s.replace(
'''                    heardText = candidates.first()
                    statusText = "Commande reçue"
''',
'''                    heardText = candidates.first()
                    voiceDetected = true
                    voiceLevel = 1f
                    statusText = "Voix reconnue ✓"
''',
1
)

s = s.replace(
'''                    if (partial.isNotBlank()) heardText = partial
''',
'''                    if (partial.isNotBlank()) {
                        heardText = partial
                        voiceDetected = true
                        voiceLevel = voiceLevel.coerceAtLeast(0.65f)
                    }
''',
1
)

p.write_text(s, encoding='utf-8')
print('Effet lumineux vocal ajouté à VoiceCommandActivity')
