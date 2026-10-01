from pathlib import Path

p = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('Settings.ACTION_TETHER_SETTINGS', '"android.settings.TETHER_SETTINGS"')
s = s.replace('Settings.ACTION_NOTIFICATION_SETTINGS', '"android.settings.NOTIFICATION_SETTINGS"')
p.write_text(s, encoding='utf-8')
print('Actions Android des commandes téléphone corrigées')
