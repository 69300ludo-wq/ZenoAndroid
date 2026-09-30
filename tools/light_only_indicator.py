from pathlib import Path
import runpy

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = SERVICE.read_text(encoding='utf-8')

old = '''        val size = (112 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageResource(R.drawable.zeno_robot)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            elevation = 20f
            contentDescription = "Zeno flottant"
        }
'''
new = '''        // Aucun robot ni anneau visible au repos sur l'écran d'accueil.
        // L'anneau apparaît uniquement lorsque Zeno détecte réellement la voix.
        val size = (72 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageDrawable(null)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(
                    (5 * resources.displayMetrics.density).toInt().coerceAtLeast(2),
                    android.graphics.Color.rgb(50, 230, 255)
                )
            }
            alpha = 0f
            elevation = 18f
            contentDescription = "Anneau lumineux de détection vocale Zeno"
        }
'''
if old not in s:
    raise SystemExit('Bloc du robot flottant introuvable')
s = s.replace(old, new, 1)

# Le témoin reste placé au bord mais totalement invisible tant qu'aucune voix n'est détectée.
s = s.replace('''            x = 20
            y = 260
''', '''            x = 28
            y = 190
''', 1)

# L'anneau s'allume au début de la parole, pas simplement quand le micro attend.
s = s.replace(
    '''                override fun onBeginningOfSpeech() = Unit''',
    '''                override fun onBeginningOfSpeech() {
                    setBubbleListening(true)
                }''',
    1
)

# Le texte de notification explique que la lumière ne s'affiche qu'à la détection.
s = s.replace(
    '''            .setContentText("Dis « ${currentWakePhrase()} » ou touche le robot pour parler.")''',
    '''            .setContentText("Dis « ${currentWakePhrase()} ». La lumière apparaît seulement quand Zeno détecte ta voix.")''',
    1
)

# Invisible au repos ; visible et lumineux uniquement pendant la détection de voix.
s = s.replace(
'''        bubble?.animate()
            ?.scaleX(if (active) 1.2f else 1f)
            ?.scaleY(if (active) 1.2f else 1f)
            ?.alpha(if (active) .88f else 1f)
''',
'''        bubble?.animate()
            ?.scaleX(if (active) 1.30f else 1f)
            ?.scaleY(if (active) 1.30f else 1f)
            ?.alpha(if (active) 1f else 0f)
''',
1
)

SERVICE.write_text(s, encoding='utf-8')
print('Accueil propre : anneau invisible au repos, visible seulement à la détection vocale')

# Garde la voix parlée de Zeno dans la même version compilée.
runpy.run_path('tools/zeno_spoken_voice.py', run_name='__main__')
