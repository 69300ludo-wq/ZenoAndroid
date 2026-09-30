from pathlib import Path

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
new = '''        // Aucun robot à l'écran : seulement un petit témoin lumineux.
        // Sa présence signifie que le service vocal Zeno est bien actif.
        val size = (38 * resources.displayMetrics.density).toInt()
        val view = ImageView(this).apply {
            setImageDrawable(null)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.argb(210, 45, 220, 255))
                setStroke(
                    (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                    android.graphics.Color.argb(245, 170, 245, 255)
                )
            }
            elevation = 12f
            contentDescription = "Témoin lumineux Zeno actif"
        }
        android.animation.ObjectAnimator.ofFloat(view, View.ALPHA, 0.30f, 1f).apply {
            duration = 950L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
'''
if old not in s:
    raise SystemExit('Bloc du robot flottant introuvable')
s = s.replace(old, new, 1)

# Place le témoin près du bord de l'écran, discret et non gênant.
s = s.replace('''            x = 20
            y = 260
''', '''            x = 18
            y = 170
''', 1)

# Le texte de notification ne doit plus parler de robot.
s = s.replace(
    '''            .setContentText("Dis « ${currentWakePhrase()} » ou touche le robot pour parler.")''',
    '''            .setContentText("Dis « ${currentWakePhrase()} ». La lumière indique que Zeno écoute.")''',
    1
)

# Quand Zeno entend activement une commande, la lumière grossit légèrement.
s = s.replace(
'''        bubble?.animate()
            ?.scaleX(if (active) 1.2f else 1f)
            ?.scaleY(if (active) 1.2f else 1f)
            ?.alpha(if (active) .88f else 1f)
''',
'''        bubble?.animate()
            ?.scaleX(if (active) 1.45f else 1f)
            ?.scaleY(if (active) 1.45f else 1f)
            ?.alpha(if (active) 1f else .85f)
''',
1
)

SERVICE.write_text(s, encoding='utf-8')
print('Robot flottant retiré : témoin lumineux vocal uniquement')
