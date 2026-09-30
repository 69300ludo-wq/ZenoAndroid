from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = SERVICE.read_text(encoding='utf-8')

# Le point flottant et la phrase "Salut Zeno" utilisent le même SpeechRecognizer.
# Le clic appelle directement le mode commande : pas de seconde activité vocale.
if 'view.setOnClickListener { openReliableVoiceCommand() }' in s:
    s = s.replace(
        'view.setOnClickListener { openReliableVoiceCommand() }',
        'view.setOnClickListener { beginFloatingCommand() }',
        1,
    )
elif 'view.setOnClickListener { beginFloatingCommand() }' not in s:
    # Certaines variantes n'ont pas de listener explicite : le ACTION_UP ci-dessous suffit.
    pass

# Zone tactile plus grande sans agrandir visuellement le point lumineux.
# Le point visible reste environ 26dp mais la fenêtre tactile fait 56dp.
s = s.replace(
    'val size = (26 * resources.displayMetrics.density).toInt()',
    '''val size = (56 * resources.displayMetrics.density).toInt()
        val dotInset = (15 * resources.displayMetrics.density).toInt()''',
    1,
)

# Après quality_cleanup.py le point est une View avec un GradientDrawable en fond.
# On transforme ce fond en cercle centré avec marge transparente pour faciliter le toucher.
old_bg = '''            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(40, 220, 255))
                setStroke(
                    (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                    android.graphics.Color.WHITE
                )
            }
'''
new_bg = '''            background = android.graphics.drawable.InsetDrawable(
                android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.rgb(40, 220, 255))
                    setStroke(
                        (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                        android.graphics.Color.WHITE
                    )
                },
                dotInset
            )
'''
if old_bg in s:
    s = s.replace(old_bg, new_bg, 1)

# Appel direct au relâchement du doigt. On ne dépend plus de performClick(), qui peut
# être absorbé par le gestionnaire de déplacement sur certains appareils/OEM.
old_action = '''                    if (moved < 18f * resources.displayMetrics.density) {
                        if (duration >= 650) view.performLongClick() else view.performClick()
                    }
                    true
'''
new_action = '''                    if (moved < 22f * resources.displayMetrics.density) {
                        if (duration >= 650) openZeno() else beginFloatingCommand()
                    }
                    true
'''
if old_action in s:
    s = s.replace(old_action, new_action, 1)
elif 'if (duration >= 650) openZeno() else openReliableVoiceCommand()' in s:
    s = s.replace(
        'if (duration >= 650) openZeno() else openReliableVoiceCommand()',
        'if (duration >= 650) openZeno() else beginFloatingCommand()',
        1,
    )
elif 'if (duration >= 650) openZeno() else beginFloatingCommand()' not in s:
    raise SystemExit('Gestion du toucher du point flottant introuvable')

# Champ de transition : empêche onError() de relancer l'écoute du mot de réveil
# pendant qu'on bascule vers l'écoute de la commande.
field_anchor = '    private var pauseWakeUntil = 0L\n'
if 'private var commandTransition = false' not in s:
    if field_anchor not in s:
        raise SystemExit('Champ pauseWakeUntil introuvable')
    s = s.replace(field_anchor, field_anchor + '    private var commandTransition = false\n', 1)

# Pendant une transition vers la commande, ERROR_CLIENT/annulation est normal et ne
# doit pas relancer une deuxième écoute concurrente.
error_anchor = '''                override fun onError(error: Int) {
                    listening = false
                    setBubbleListening(false)
'''
if error_anchor in s and 'if (commandTransition) return' not in s:
    s = s.replace(
        error_anchor,
        '''                override fun onError(error: Int) {
                    listening = false
                    if (commandTransition) return
                    setBubbleListening(false)
''',
        1,
    )

# Remplace complètement l'ancien chemin qui détruisait le micro du service puis
# lançait VoiceCommandActivity.
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
        if (commandTransition) return

        // Le même recognizer passe de l'attente "Salut Zeno" à la commande.
        commandTransition = true
        pauseWakeUntil = Long.MAX_VALUE
        wakeTriggered = false
        wakeMode = false
        runCatching { speechRecognizer?.cancel() }
        listening = false
        if (speechRecognizer == null) runCatching { prepareSpeechRecognizer() }
        setBubbleListening(true)
        Toast.makeText(this, "Zeno t’écoute…", Toast.LENGTH_SHORT).show()

        handler.postDelayed({
            commandTransition = false
            pauseWakeUntil = 0L
            runCatching { beginCommandListening() }
        }, 420)
    }

    private fun setBubbleListening(active: Boolean) {'''
s, count = pattern.subn(replacement, s, count=1)
if count != 1:
    # Le script peut être rejoué sur une source déjà transformée lors d'un test local.
    if 'private fun beginFloatingCommand()' not in s:
        raise SystemExit('Ancien chemin VoiceCommandActivity du flottant introuvable')

# Le réveil vocal utilise exactement la même transition protégée que le toucher.
trigger_pattern = re.compile(
    r'''    private fun triggerWakePhrase\(\) \{.*?\n    \}\n\n    private fun hasMic''',
    re.S,
)
trigger_replacement = '''    private fun triggerWakePhrase() {
        if (wakeTriggered || commandTransition) return
        wakeTriggered = true
        wakeMode = false
        commandTransition = true
        pauseWakeUntil = Long.MAX_VALUE
        runCatching { speechRecognizer?.cancel() }
        listening = false
        setBubbleListening(true)

        handler.postDelayed({
            wakeTriggered = false
            commandTransition = false
            pauseWakeUntil = 0L
            runCatching { beginCommandListening() }
        }, 420)
    }

    private fun hasMic'''
s, trigger_count = trigger_pattern.subn(trigger_replacement, s, count=1)
if trigger_count != 1:
    raise SystemExit('Déclenchement Salut Zeno introuvable')

# L'activité vocale reste disponible depuis le bouton dédié de l'app uniquement.
s = s.replace('import com.zeno.robot.VoiceCommandActivity\n', '')

SERVICE.write_text(s, encoding='utf-8')
print('Toucher flottant fiabilisé : grande zone tactile, appel direct et transition mono-micro protégée')
