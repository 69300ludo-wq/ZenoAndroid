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
new = '''        // Aucun robot : un anneau lumineux bien visible indique que Zeno fonctionne.
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
            elevation = 18f
            contentDescription = "Anneau lumineux Zeno actif"
        }
        // Pulsation permanente : visible = service Zeno actif.
        android.animation.ObjectAnimator.ofFloat(view, View.ALPHA, 0.55f, 1f).apply {
            duration = 800L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
        android.animation.ObjectAnimator.ofFloat(view, View.SCALE_X, 0.92f, 1.10f).apply {
            duration = 800L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
        android.animation.ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.92f, 1.10f).apply {
            duration = 800L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
'''
if old not in s:
    raise SystemExit('Bloc du robot flottant introuvable')
s = s.replace(old, new, 1)

# Place l'anneau près du bord de l'écran, mais suffisamment loin pour rester visible.
s = s.replace('''            x = 20
            y = 260
''', '''            x = 28
            y = 190
''', 1)

# Le texte de notification ne doit plus parler de robot.
s = s.replace(
    '''            .setContentText("Dis « ${currentWakePhrase()} » ou touche le robot pour parler.")''',
    '''            .setContentText("Dis « ${currentWakePhrase()} ». L’anneau lumineux indique que Zeno écoute.")''',
    1
)

# Quand Zeno détecte réellement la voix, l'anneau devient nettement plus grand.
s = s.replace(
'''        bubble?.animate()
            ?.scaleX(if (active) 1.2f else 1f)
            ?.scaleY(if (active) 1.2f else 1f)
            ?.alpha(if (active) .88f else 1f)
''',
'''        bubble?.animate()
            ?.scaleX(if (active) 1.35f else 1f)
            ?.scaleY(if (active) 1.35f else 1f)
            ?.alpha(if (active) 1f else .85f)
''',
1
)

SERVICE.write_text(s, encoding='utf-8')
print('Anneau lumineux Zeno rendu visible, sans robot')

# Garde la voix parlée de Zeno dans la même version compilée.
runpy.run_path('tools/zeno_spoken_voice.py', run_name='__main__')
