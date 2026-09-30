from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = SERVICE.read_text(encoding='utf-8')

# Le point flottant et la phrase "Salut Zeno" utilisent désormais le même
# SpeechRecognizer du service. Aucun lancement d'une seconde activité vocale.
if 'view.setOnClickListener { openReliableVoiceCommand() }' in s:
    s = s.replace(
        'view.setOnClickListener { openReliableVoiceCommand() }',
        'view.setOnClickListener { beginFloatingCommand() }',
        1,
    )
elif 'if (duration >= 650) openZeno() else openReliableVoiceCommand()' in s:
    s = s.replace(
        'if (duration >= 650) openZeno() else openReliableVoiceCommand()',
        'if (duration >= 650) openZeno() else beginFloatingCommand()',
        1,
    )
else:
    raise SystemExit('Gestion du clic du point flottant introuvable')

# Remplace complètement le chemin qui détruisait le micro du service puis lançait
# VoiceCommandActivity. C'était le dernier endroit où deux chemins micro pouvaient
# se télescoper sur certains téléphones.
pattern = re.compile(
    r'''    private fun openReliableVoiceCommand\(\) \{.*?'''
    r'''    private fun setBubbleListening\(active: Boolean\) \{''',
    re.S,
)
replacement = '''    private fun beginFloatingCommand() {
        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            setBubbleListening(false)
            Toast.makeText(this, "Microphone indisponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Reste dans le service flottant : un seul moteur vocal possède le micro.
        pauseWakeUntil = 0L
        wakeTriggered = false
        wakeMode = false
        runCatching { speechRecognizer?.cancel() }
        listening = false
        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        setBubbleListening(true)
        handler.postDelayed({ runCatching { beginCommandListening() } }, 220)
    }

    private fun setBubbleListening(active: Boolean) {'''
s, count = pattern.subn(replacement, s, count=1)
if count != 1:
    raise SystemExit('Ancien chemin VoiceCommandActivity du flottant introuvable')

# Le réveil vocal reste lui aussi dans le service et allume le point pendant la
# transition vers l'écoute de commande.
for delay in ('550', '350'):
    old_trigger = f'''        wakeTriggered = false\n        handler.postDelayed({{ beginCommandListening() }}, {delay})\n'''
    new_trigger = '''        wakeTriggered = false
        setBubbleListening(true)
        handler.postDelayed({ beginCommandListening() }, 420)
'''
    if old_trigger in s:
        s = s.replace(old_trigger, new_trigger, 1)
        break

# L'activité vocale reste disponible uniquement depuis le bouton dédié de l'app,
# mais le service flottant ne la lance plus directement.
s = s.replace('import com.zeno.robot.VoiceCommandActivity\n', '')

SERVICE.write_text(s, encoding='utf-8')
print('Zeno flottant unifié : un seul SpeechRecognizer pour le point et Salut Zeno')
