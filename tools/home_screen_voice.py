from pathlib import Path

SERVICE = Path("app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt")
s = SERVICE.read_text(encoding="utf-8")

# Le script voice_wake_reliability.py a déjà rendu l'écoute indépendante
# de la bulle flottante. Ici on garantit que la phrase de réveil ramène
# simplement Zeno au premier plan depuis l'écran d'accueil Android.
old_open = '''    private fun openZeno() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
'''
new_open = '''    private fun openZeno() {
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
print("Déclenchement vocal depuis l'écran d'accueil Android activé")
