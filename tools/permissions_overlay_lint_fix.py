from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
s = p.read_text(encoding='utf-8')

s = s.replace(
    'android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(context)',
    'Settings.canDrawOverlays(context)'
)

if 'import androidx.core.net.toUri\n' not in s:
    anchor = 'import androidx.core.content.edit\n'
    if anchor not in s:
        raise SystemExit('Import KTX edit introuvable')
    s = s.replace(anchor, anchor + 'import androidx.core.net.toUri\n', 1)

s = s.replace(
    'Uri.parse("package:${context.packageName}")',
    '"package:${context.packageName}".toUri()'
)

p.write_text(s, encoding='utf-8')
print('Lint permissions/superposition corrigé')
