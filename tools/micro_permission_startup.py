from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
main = MAIN.read_text(encoding='utf-8')

pattern = re.compile(
    r'''class MainActivity : ComponentActivity\(\) \{\n'''
    r'''    override fun onCreate\(savedInstanceState: Bundle\?\) \{.*?'''
    r'''\n    \}\n\}''',
    re.S,
)

replacement = '''class MainActivity : ComponentActivity() {
    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val prefs = getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE)
        if (granted) {
            prefs.edit { putBoolean("voice_enabled", true) }
            runCatching { startVoiceZeno(this) }
            Toast.makeText(this, "Microphone autorisé. Zeno vocal est actif.", Toast.LENGTH_LONG).show()
        } else {
            prefs.edit { putBoolean("voice_enabled", false) }
            Toast.makeText(
                this,
                "Autorise le microphone pour utiliser la reconnaissance vocale Zeno.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val prefs = getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE)

        if (micGranted) {
            if (prefs.getBoolean("voice_enabled", true)) {
                prefs.edit { putBoolean("voice_enabled", true) }
                runCatching { startVoiceZeno(this) }
            }
        } else {
            // Demande Android visible dès l'ouverture : aucune recherche manuelle
            // dans les paramètres n'est nécessaire.
            microphonePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        setContent { ZenoApp() }
    }
}'''

main, count = pattern.subn(replacement, main, count=1)
if count != 1:
    raise SystemExit('MainActivity/onCreate introuvable pour la permission micro')

# Version affichée correspondant à cette correction.
main = main.replace('Zeno Android v1.3.10', 'Zeno Android v1.3.11')
main = main.replace('Version 1.3.10', 'Version 1.3.11')
MAIN.write_text(main, encoding='utf-8')
print('Permission micro : demande Android automatique au premier démarrage')
