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
new = '''        // Petit point lumineux permanent et déplaçable : indique que Zeno fonctionne.
        val size = (26 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageDrawable(null)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(40, 220, 255))
                setStroke(
                    (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                    android.graphics.Color.WHITE
                )
            }
            alpha = .78f
            elevation = 18f
            contentDescription = "Point lumineux Zeno déplaçable"
        }
'''
if old not in s:
    raise SystemExit('Bloc du robot flottant introuvable')
s = s.replace(old, new, 1)

s = s.replace('''            x = 20
            y = 260
''', '''            x = 28
            y = 190
''', 1)

# Le point reste visible au repos et s'illumine davantage lorsque la voix est détectée.
s = s.replace(
    '''                override fun onBeginningOfSpeech() = Unit''',
    '''                override fun onBeginningOfSpeech() {
                    setBubbleListening(true)
                }''',
    1
)

s = s.replace(
    '''            .setContentText("Dis « ${currentWakePhrase()} » ou touche le robot pour parler.")''',
    '''            .setContentText("Dis « ${currentWakePhrase()} ». Le point lumineux indique que Zeno est actif.")''',
    1
)

s = s.replace(
'''        bubble?.animate()
            ?.scaleX(if (active) 1.2f else 1f)
            ?.scaleY(if (active) 1.2f else 1f)
            ?.alpha(if (active) .88f else 1f)
''',
'''        bubble?.animate()
            ?.scaleX(if (active) 1.35f else 1f)
            ?.scaleY(if (active) 1.35f else 1f)
            ?.alpha(if (active) 1f else .78f)
''',
1
)

SERVICE.write_text(s, encoding='utf-8')
print('Petit point lumineux Zeno visible, déplaçable et réactif à la voix')

runpy.run_path('tools/zeno_spoken_voice.py', run_name='__main__')
