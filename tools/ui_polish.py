from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
s = p.read_text(encoding='utf-8')

# Les vraies icônes Android des applications installées.
if 'import android.widget.ImageView' not in s:
    s = s.replace('import android.speech.tts.TextToSpeech\n', 'import android.speech.tts.TextToSpeech\nimport android.widget.ImageView\n', 1)
if 'import androidx.compose.ui.viewinterop.AndroidView' not in s:
    s = s.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.viewinterop.AndroidView\n', 1)

# Descendre la barre supérieure sous la barre d’état Android.
top_old = 'Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),'
top_new = 'Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),'
if top_old in s:
    s = s.replace(top_old, top_new, 1)

# Cacher le petit menu supérieur de l'écran d'accueil.
# Les modules Paramètres / À propos restent accessibles depuis les modules de l'accueil.
home_top_menu = '''                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp).clickable { navigate(Screen.SETTINGS) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x99101D3C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24548A))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, "Paramètres", tint = Color.White) }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x99251A32),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF76502C))
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFC64B)) }
                    }
                }

'''
if home_top_menu in s:
    s = s.replace(home_top_menu, '', 1)

# Agrandir uniquement le robot de l’accueil, sans changer le reste du design.
robot_old = '''                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(185.dp)) {
                    Box(
                        Modifier.size(148.dp).background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1EC8FF), accent, Color(0xFF8A2DFF), Color(0xFF071B4D))
                            ),
                            CircleShape
                        )
                    )
                    Box(
                        Modifier.size(132.dp).background(Color(0xFF071F62), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = "Zeno",
                            modifier = Modifier.size(118.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }'''
robot_new = '''                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    Box(
                        Modifier.size(180.dp).background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1EC8FF), accent, Color(0xFF8A2DFF), Color(0xFF071B4D))
                            ),
                            CircleShape
                        )
                    )
                    Box(
                        Modifier.size(164.dp).background(Color(0xFF071F62), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.zeno_robot),
                            contentDescription = "Zeno",
                            modifier = Modifier.size(150.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }'''
if robot_old in s:
    s = s.replace(robot_old, robot_new, 1)

# Remplacer la lettre factice par l’icône réelle fournie par Android.
icon_old = '''                        Surface(shape = CircleShape, color = accent.copy(alpha = .18f), modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(app.label.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold)
                            }
                        }'''
icon_new = '''                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                                        setPadding(2, 2, 2, 2)
                                    }
                                },
                                update = { imageView ->
                                    imageView.setImageDrawable(
                                        runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }'''
if icon_old not in s:
    raise SystemExit('Bloc icône des applications introuvable')
s = s.replace(icon_old, icon_new, 1)

p.write_text(s, encoding='utf-8')
print('UI Zeno ajustée : menu accueil caché, barre haute descendue, robot agrandi, vraies icônes applications')
