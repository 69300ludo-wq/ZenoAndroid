from pathlib import Path

brain_path = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
main_path = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
strings_path = Path('app/src/main/res/values/strings.xml')

brain = brain_path.read_text(encoding='utf-8')
brain = brain.replace('Settings.ACTION_TETHER_SETTINGS', '"android.settings.TETHER_SETTINGS"')
brain = brain.replace('Settings.ACTION_NOTIFICATION_SETTINGS', '"android.settings.NOTIFICATION_SETTINGS"')
brain = brain.replace('Uri.parse("package:${context.packageName}")', '"package:${context.packageName}".toUri()')
if '.toUri()' in brain and 'import androidx.core.net.toUri' not in brain:
    anchor = 'import androidx.core.content.ContextCompat\n'
    if anchor in brain:
        brain = brain.replace(anchor, anchor + 'import androidx.core.net.toUri\n', 1)
    else:
        brain = brain.replace('package com.zeno.robot.data\n', 'package com.zeno.robot.data\n\nimport androidx.core.net.toUri', 1)
if 'Uri.' not in brain:
    brain = brain.replace('import android.net.Uri\n', '')
brain_path.write_text(brain, encoding='utf-8')

main = main_path.read_text(encoding='utf-8')
main = main.replace('Uri.parse("package:${context.packageName}")', '"package:${context.packageName}".toUri()')
if '.toUri()' in main and 'import androidx.core.net.toUri' not in main:
    anchor = 'import androidx.core.content.ContextCompat\n'
    if anchor in main:
        main = main.replace(anchor, anchor + 'import androidx.core.net.toUri\n', 1)
    else:
        main = main.replace('package com.zeno.robot\n', 'package com.zeno.robot\n\nimport androidx.core.net.toUri', 1)
if 'Uri.' not in main:
    main = main.replace('import android.net.Uri\n', '')
main_path.write_text(main, encoding='utf-8')

strings = strings_path.read_text(encoding='utf-8')
strings = strings.replace('    <string name="channel_description">Maintient « Salut Zeno » actif en arrière-plan.</string>\n', '')
strings = strings.replace('    <string name="channel_description">Maintient « Zeno ouvre » actif en arrière-plan.</string>\n', '')
strings_path.write_text(strings, encoding='utf-8')

print('Actions Android et lint final v1.3.14 corrigés')
