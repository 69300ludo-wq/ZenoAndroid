from pathlib import Path

main_path = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
main = main_path.read_text(encoding='utf-8')
replacements = {
    'prefs.edit().putBoolean("voice_enabled", true).apply()': 'prefs.edit { putBoolean("voice_enabled", true) }',
    'prefs.edit().putBoolean("voice_enabled", false).apply()': 'prefs.edit { putBoolean("voice_enabled", false) }',
    'prefs.edit().putLong("indicator_color", value).apply()': 'prefs.edit { putLong("indicator_color", value) }',
    'prefs.edit().putInt("indicator_size", it.toInt()).apply()': 'prefs.edit { putInt("indicator_size", it.toInt()) }',
    'prefs.edit().putString("indicator_position", key).apply()': 'prefs.edit { putString("indicator_position", key) }',
    'prefs.edit().putFloat("indicator_opacity", it).apply()': 'prefs.edit { putFloat("indicator_opacity", it) }',
    'prefs.edit().putBoolean("indicator_pulse", it).apply()': 'prefs.edit { putBoolean("indicator_pulse", it) }',
}
for old, new in replacements.items():
    main = main.replace(old, new)
main = main.replace('    revision\n    if (!prefs.getBoolean("voice_enabled", false)) return', '    if (revision < 0) return\n    if (!prefs.getBoolean("voice_enabled", false)) return')
main_path.write_text(main, encoding='utf-8')

setup_path = Path('app/src/main/java/com/zeno/robot/SetupActivity.kt')
setup = setup_path.read_text(encoding='utf-8')
if 'import androidx.core.content.edit\n' not in setup:
    setup = setup.replace('import androidx.core.content.ContextCompat\n', 'import androidx.core.content.ContextCompat\nimport androidx.core.content.edit\n', 1)
setup = setup.replace(
    'getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit().putBoolean("voice_enabled", true).apply()',
    'getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit { putBoolean("voice_enabled", true) }'
)
setup_path.write_text(setup, encoding='utf-8')
print('Lint KTX du vocal direct corrigé')
