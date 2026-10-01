from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# --- Interface : aucun voyant sur Accueil ni ailleurs.
main = MAIN.read_text(encoding='utf-8')
main = re.sub(
    r'^\s*(?:if \(screen == Screen\.HOME\)\s*)?VoiceStatusDot\([^\n]*\)\s*\n',
    '',
    main,
    flags=re.M,
)

start = main.find('@Composable\nprivate fun BoxScope.VoiceStatusDot')
if start >= 0:
    end = main.find('@Composable\n', start + 12)
    if end < 0:
        raise SystemExit('Fin de VoiceStatusDot introuvable')
    main = main[:start] + main[end:]

# Remplacer le bloc de permissions par une version micro uniquement, sans voyant,
# sans permission de superposition et sans indicateur lumineux.
perm_start = main.find('@Composable\nprivate fun PermissionControlCard(accent: Color)')
perm_end = main.find('@Composable\nprivate fun ActionGrid', perm_start)
if perm_start >= 0 and perm_end > perm_start:
    permission_card = r'''@Composable
private fun PermissionControlCard(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }

    fun hasMicrophone(): Boolean = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    var micGranted by remember { mutableStateOf(hasMicrophone()) }

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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE60A1835),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (micGranted) accent else Color(0xFFFFA24A)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Autorisation vocale Zeno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Seul le microphone est nécessaire pour la reconnaissance vocale.",
                color = Color(0xFFAFC3E8),
                fontSize = 11.sp
            )

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

            Button(
                onClick = {
                    micGranted = hasMicrophone()
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

'''
    main = main[:perm_start] + permission_card + main[perm_end:]

# Nettoyer les textes de personnalisation qui mentionnent encore les voyants.
main = main.replace(
    'Text("Dis simplement « Zeno ». Les voyants utilisent maintenant leur configuration par défaut.", color = Color(0xFF9AB4D8))',
    'Text("Dis simplement « Zeno » pour lancer une commande vocale.", color = Color(0xFF9AB4D8))'
)
main = main.replace('Micro Android et voyant du tiroir', 'Micro Android')
main = main.replace('Zeno Android v1.3.22', 'Zeno Android v1.3.23')
main = main.replace('Version 1.3.22', 'Version 1.3.23')
MAIN.write_text(main, encoding='utf-8')

# --- Manifest : plus aucune permission de superposition.
manifest = MANIFEST.read_text(encoding='utf-8')
manifest = re.sub(
    r'^\s*<uses-permission android:name="android\.permission\.SYSTEM_ALERT_WINDOW"\s*/>\s*\n',
    '',
    manifest,
    flags=re.M,
)
MANIFEST.write_text(manifest, encoding='utf-8')

# --- Service : ne jamais créer ni mettre à jour de voyant global.
service = SERVICE.read_text(encoding='utf-8')
service = service.replace('        showOverlayDot()\n', '        removeOverlayDot()\n')
service = service.replace('        updateOverlayDot(state)\n', '')
service = service.replace(
    'Service vocal Zeno avec voyant de superposition optionnel.',
    'Service vocal Zeno sans voyant de superposition.'
)
SERVICE.write_text(service, encoding='utf-8')

print('Zeno 1.3.23 : tous les voyants supprimés, vocal conservé')
