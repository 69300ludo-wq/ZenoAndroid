from pathlib import Path

MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')

# Le service vocal tourne dans :voice. Les SharedPreferences du processus principal
# ne sont pas fiables pour une mise à jour immédiate entre processus. On envoie donc
# explicitement la nouvelle position au service, qui la sauvegarde lui-même et déplace
# le voyant tout de suite.
service = SERVICE.read_text(encoding='utf-8')

if 'import androidx.core.content.edit\n' not in service:
    anchor = 'import androidx.core.content.ContextCompat\n'
    if anchor not in service:
        raise SystemExit('Import ContextCompat service introuvable')
    service = service.replace(anchor, anchor + 'import androidx.core.content.edit\n', 1)

old_on_start = '''    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlayDot()
        if (!hasMic()) {
            broadcastState("permission")
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_DIRECT_COMMAND -> handler.post { beginListening(Mode.COMMAND, resetRetry = true) }
            else -> handler.postDelayed({ startWakeListening() }, 250)
        }
        return START_STICKY
    }
'''
new_on_start = '''    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_INDICATOR_POSITION) {
            val requested = intent.getStringExtra(EXTRA_INDICATOR_POSITION)
            if (!requested.isNullOrBlank()) {
                getSharedPreferences("zeno_indicator", MODE_PRIVATE).edit {
                    putString("indicator_position", requested)
                }
            }
            showOverlayDot()
            return START_STICKY
        }

        showOverlayDot()
        if (!hasMic()) {
            broadcastState("permission")
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_DIRECT_COMMAND -> handler.post { beginListening(Mode.COMMAND, resetRetry = true) }
            else -> handler.postDelayed({ startWakeListening() }, 250)
        }
        return START_STICKY
    }
'''
if old_on_start not in service:
    raise SystemExit('onStartCommand v1.3.15 introuvable')
service = service.replace(old_on_start, new_on_start, 1)

old_constants = '''        const val ACTION_DIRECT_COMMAND = "com.zeno.robot.action.DIRECT_COMMAND"
        const val ACTION_VOICE_STATE = "com.zeno.robot.action.VOICE_STATE"
        const val EXTRA_STATE = "voice_state"
'''
new_constants = '''        const val ACTION_DIRECT_COMMAND = "com.zeno.robot.action.DIRECT_COMMAND"
        const val ACTION_UPDATE_INDICATOR_POSITION = "com.zeno.robot.action.UPDATE_INDICATOR_POSITION"
        const val ACTION_VOICE_STATE = "com.zeno.robot.action.VOICE_STATE"
        const val EXTRA_INDICATOR_POSITION = "indicator_position"
        const val EXTRA_STATE = "voice_state"
'''
if old_constants not in service:
    raise SystemExit('Constantes service introuvables')
service = service.replace(old_constants, new_constants, 1)
SERVICE.write_text(service, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')
old_select = '''    fun selectPosition(id: String) {
        selected = id
        prefs.edit { putString("indicator_position", id) }
        runCatching { startVoiceZeno(context) }
    }
'''
new_select = '''    fun selectPosition(id: String) {
        selected = id
        prefs.edit { putString("indicator_position", id) }
        val updateIntent = Intent(
            context,
            com.zeno.robot.service.FloatingZenoService::class.java
        ).apply {
            action = com.zeno.robot.service.FloatingZenoService.ACTION_UPDATE_INDICATOR_POSITION
            putExtra(com.zeno.robot.service.FloatingZenoService.EXTRA_INDICATOR_POSITION, id)
        }
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(context, updateIntent)
        }
    }
'''
if old_select not in main:
    raise SystemExit('Sélecteur de position v1.3.15 introuvable')
main = main.replace(old_select, new_select, 1)
main = main.replace('Zeno Android v1.3.15', 'Zeno Android v1.3.16')
main = main.replace('Version 1.3.15', 'Version 1.3.16')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.16 : position du voyant envoyée directement au service :voice + lint KTX')
