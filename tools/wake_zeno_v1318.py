from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')

service = SERVICE.read_text(encoding='utf-8')

# Phrase de réveil courte : « Zeno ». Les variantes servent uniquement à tolérer
# les transcriptions fréquentes du SpeechRecognizer Android.
service, count = re.subn(
    r'private val wakePhrases = setOf\(\s*"zeno ouvre",\s*"seno ouvre",\s*"xeno ouvre",\s*"zino ouvre",\s*"zena ouvre"\s*\)',
    'private val wakePhrases = setOf(\n            "zeno", "seno", "xeno", "zino", "zena"\n        )',
    service,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit('Liste wakePhrases Zeno ouvre introuvable')

# Tous les textes utilisateur doivent refléter la nouvelle phrase.
replacements = {
    'Dis « Zeno ouvre », puis ta demande.': 'Dis « Zeno », puis ta demande.',
    'Écoute vocale Zeno : Zeno ouvre': 'Écoute vocale Zeno : dis Zeno',
    'Dis « Zeno ouvre » et réessaie.': 'Dis « Zeno » et réessaie.',
    '« Zeno ouvre »': '« Zeno »',
    '"Zeno ouvre"': '"Zeno"',
}
for old, new in replacements.items():
    service = service.replace(old, new)
SERVICE.write_text(service, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')
main = main.replace('Zeno ouvre', 'Zeno')
main = main.replace('Zeno Android v1.3.17', 'Zeno Android v1.3.18')
main = main.replace('Version 1.3.17', 'Version 1.3.18')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.18 : phrase de réveil = Zeno ; bleu en attente, violet pour la commande')
