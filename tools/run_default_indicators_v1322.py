from pathlib import Path
import runpy

SCRIPT = Path('tools/default_indicators_quiet_wake_v1322.py')
source = SCRIPT.read_text(encoding='utf-8')

old = '''# Le voyant du tiroir suit l'état vocal au lieu d'être supprimé à chaque changement.
broadcast_start = service.find('    private fun broadcastState(state: String) {')
if broadcast_start < 0:
    raise SystemExit('broadcastState introuvable')
broadcast_end = service.find('\\n    private fun ', broadcast_start + 10)
if broadcast_end < 0:
    raise SystemExit('Fin broadcastState introuvable')
broadcast_block = service[broadcast_start:broadcast_end]
broadcast_block = broadcast_block.replace(
    '        if (overlayDot != null) removeOverlayDot()\\n',
    '        updateOverlayDot(state)\\n',
    1,
)
service = service[:broadcast_start] + broadcast_block + service[broadcast_end:]
'''

new = '''# Le voyant du tiroir suit l'état vocal au lieu d'être supprimé à chaque changement.
# broadcastState peut être la dernière fonction privée du service, donc on modifie
# directement son début au lieu de chercher une fonction privée suivante.
service, count = re.subn(
    r'(    private fun broadcastState\\(state: String\\) \\{\\n)(?:        if \\(overlayDot != null\\) removeOverlayDot\\(\\)\\n|        updateOverlayDot\\(state\\)\\n)?',
    r'\\1        updateOverlayDot(state)\\n',
    service,
    count=1,
)
if count != 1:
    raise SystemExit('broadcastState introuvable')
'''

if old not in source:
    raise SystemExit('Bloc broadcastState à corriger introuvable')
SCRIPT.write_text(source.replace(old, new, 1), encoding='utf-8')
runpy.run_path(str(SCRIPT), run_name='__main__')
