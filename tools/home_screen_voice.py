from pathlib import Path

SERVICE = Path("app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt")
s = SERVICE.read_text(encoding="utf-8")

old_create = '''        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 1000)
        } else stopSelf()
'''
new_create = '''        // L'écoute vocale ne dépend plus de la bulle flottante :
        // elle reste active quand l'utilisateur revient sur l'écran d'accueil Android.
        if (Settings.canDrawOverlays(this)) showBubble()
        prepareSpeechRecognizer()
        if (hasMic()) handler.postDelayed({ startWakeListening() }, 700)
'''
if old_create not in s:
    raise SystemExit("Bloc onCreate vocal introuvable")
s = s.replace(old_create, new_create, 1)

old_start = '''        if (Settings.canDrawOverlays(this)) {
            showBubble()
            prepareSpeechRecognizer()
            handler.postDelayed({ startWakeListening() }, 700)
        }
        return START_STICKY
'''
new_start = '''        if (Settings.canDrawOverlays(this)) showBubble()
        prepareSpeechRecognizer()
        if (hasMic()) handler.postDelayed({ startWakeListening() }, 350)
        return START_STICKY
'''
if old_start not in s:
    raise SystemExit("Bloc onStartCommand vocal introuvable")
s = s.replace(old_start, new_start, 1)

old_open = '''    private fun openZeno() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
'''
new_open = '''    private fun openZeno() {
        // Depuis l'écran d'accueil Android, la phrase choisie ramène Zeno au premier plan.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }
'''
if old_open not in s:
    raise SystemExit("Bloc openZeno introuvable")
s = s.replace(old_open, new_open, 1)

SERVICE.write_text(s, encoding="utf-8")
print("Réveil vocal depuis l'écran d'accueil Android activé")
