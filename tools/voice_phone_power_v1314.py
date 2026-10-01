from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
BRAIN = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
MANIFEST = Path('app/src/main/AndroidManifest.xml')

# --- Permissions spéciales utiles aux commandes système.
manifest = MANIFEST.read_text(encoding='utf-8')
record_anchor = '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'
extra_permissions = [
    '    <uses-permission android:name="android.permission.WRITE_SETTINGS" />\n',
    '    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />\n',
]
for permission in extra_permissions:
    if permission.strip() not in manifest:
        if record_anchor not in manifest:
            raise SystemExit('Permission micro introuvable dans le manifeste')
        manifest = manifest.replace(record_anchor, record_anchor + permission, 1)
MANIFEST.write_text(manifest, encoding='utf-8')

# --- Service vocal final : phrase « Zeno ouvre », écoute plus longue et moteur robuste.
service = r'''package com.zeno.robot.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zeno.robot.MainActivity
import com.zeno.robot.R
import com.zeno.robot.data.AppLauncher
import com.zeno.robot.data.ZenoBrain
import java.text.Normalizer
import java.util.Locale

class FloatingZenoService : Service() {
    private enum class Mode { WAKE, COMMAND, FOLLOW_UP }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var sessionSerial = 0
    private var mode = Mode.WAKE
    private var retries = 0
    private lateinit var brain: ZenoBrain
    private var overlayManager: WindowManager? = null
    private var overlayDot: View? = null
    private var wakeTransition: Runnable? = null
    private var commandTimeout: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        brain = ZenoBrain(applicationContext)
        val foregroundStarted = runCatching {
            createChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        }.isSuccess
        if (!foregroundStarted) {
            broadcastState("error")
            stopSelf()
            return
        }

        showOverlayDot()
        tts = runCatching {
            TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) runCatching { tts?.language = Locale.FRENCH }
            }
        }.getOrNull()

        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastState("permission")
            return
        }
        broadcastState("idle")
        handler.postDelayed({ startWakeListening() }, 450)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    private fun hasMic(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun createNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zeno)
            .setContentTitle("Zeno écoute")
            .setContentText("Dis « Zeno ouvre », puis ta demande.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = "Écoute vocale Zeno : Zeno ouvre"
            }
        )
    }

    private fun recognitionIntent(partial: Boolean, longWindow: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 15)
            if (longWindow) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 900L)
            }
        }

    private fun invalidateRecognizer() {
        sessionSerial += 1
        wakeTransition?.let(handler::removeCallbacks)
        wakeTransition = null
        commandTimeout?.let(handler::removeCallbacks)
        commandTimeout = null
        val old = recognizer
        recognizer = null
        runCatching { old?.cancel() }
        runCatching { old?.destroy() }
    }

    private fun createRecognizer(listener: RecognitionListener): SpeechRecognizer? = runCatching {
        SpeechRecognizer.createSpeechRecognizer(this).apply { setRecognitionListener(listener) }
    }.getOrNull()

    private fun startWakeListening() {
        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastState("permission")
            return
        }
        mode = Mode.WAKE
        invalidateRecognizer()
        val token = sessionSerial
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (token == sessionSerial) broadcastState("wake")
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (token != sessionSerial || mode != Mode.WAKE) return
                broadcastState("idle")
                val delay = when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1200L
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        broadcastState("permission")
                        return
                    }
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 900L
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 220L
                    else -> 420L
                }
                handler.postDelayed({ startWakeListening() }, delay)
            }

            override fun onResults(results: Bundle?) {
                if (token != sessionSerial || mode != Mode.WAKE) return
                val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val payload = extractWakePayload(candidates)
                when {
                    payload != null && payload.isNotBlank() -> executeCandidates(listOf(payload))
                    containsWakePhrase(candidates) -> beginListening(Mode.FOLLOW_UP, resetRetry = true)
                    else -> handler.postDelayed({ startWakeListening() }, 200)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (token != sessionSerial || mode != Mode.WAKE) return
                val candidates = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val exact = candidates.any { normalize(it) in wakePhrases }
                val withPayload = extractWakePayload(candidates)
                if (withPayload != null && withPayload.isNotBlank()) {
                    wakeTransition?.let(handler::removeCallbacks)
                    wakeTransition = null
                    return
                }
                if (exact && wakeTransition == null) {
                    val transition = Runnable {
                        if (token == sessionSerial && mode == Mode.WAKE) beginListening(Mode.FOLLOW_UP, resetRetry = true)
                    }
                    wakeTransition = transition
                    handler.postDelayed(transition, 850L)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        recognizer = createRecognizer(listener)
        val current = recognizer
        if (current == null) {
            broadcastState("error")
            handler.postDelayed({ startWakeListening() }, 1000)
            return
        }
        broadcastState("idle")
        runCatching { current.startListening(recognitionIntent(partial = true, longWindow = false)) }
            .onFailure {
                broadcastState("error")
                handler.postDelayed({ startWakeListening() }, 900)
            }
    }

    private fun beginListening(targetMode: Mode, resetRetry: Boolean) {
        if (!hasMic()) {
            broadcastState("permission")
            Toast.makeText(this, "Autorise le microphone pour parler à Zeno.", Toast.LENGTH_LONG).show()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastState("error")
            Toast.makeText(this, "Reconnaissance vocale Android indisponible.", Toast.LENGTH_LONG).show()
            return
        }
        if (resetRetry) retries = 0
        mode = targetMode
        invalidateRecognizer()
        broadcastState("command")
        if (targetMode == Mode.FOLLOW_UP) {
            Toast.makeText(this, "Zeno écoute… dis ce que tu veux.", Toast.LENGTH_SHORT).show()
        }

        handler.postDelayed({
            if (mode != targetMode) return@postDelayed
            val token = sessionSerial
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (token == sessionSerial) broadcastState("command")
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (token != sessionSerial || mode != targetMode) return
                    commandTimeout?.let(handler::removeCallbacks)
                    commandTimeout = null
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        broadcastState("permission")
                        return
                    }
                    if (retries < 1 && error != SpeechRecognizer.ERROR_CLIENT) {
                        retries += 1
                        handler.postDelayed({ beginListening(targetMode, resetRetry = false) }, 480)
                    } else {
                        Toast.makeText(this@FloatingZenoService, "Je n’ai pas bien entendu. Dis « Zeno ouvre » et réessaie.", Toast.LENGTH_SHORT).show()
                        broadcastState("idle")
                        handler.postDelayed({ startWakeListening() }, 550)
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (token != sessionSerial || mode != targetMode) return
                    commandTimeout?.let(handler::removeCallbacks)
                    commandTimeout = null
                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        .map { it.trim() }.filter { it.isNotBlank() }
                    if (candidates.isEmpty()) {
                        if (retries < 1) {
                            retries += 1
                            handler.postDelayed({ beginListening(targetMode, resetRetry = false) }, 350)
                        } else {
                            handler.postDelayed({ startWakeListening() }, 450)
                        }
                        return
                    }
                    executeCandidates(candidates)
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }

            recognizer = createRecognizer(listener)
            val current = recognizer
            if (current == null) {
                broadcastState("error")
                handler.postDelayed({ startWakeListening() }, 900)
                return@postDelayed
            }
            val timeout = Runnable {
                if (token == sessionSerial && mode == targetMode) {
                    runCatching { current.stopListening() }
                }
            }
            commandTimeout = timeout
            handler.postDelayed(timeout, 12_000L)
            runCatching { current.startListening(recognitionIntent(partial = false, longWindow = true)) }
                .onFailure {
                    commandTimeout?.let(handler::removeCallbacks)
                    commandTimeout = null
                    broadcastState("error")
                    handler.postDelayed({ startWakeListening() }, 800)
                }
        }, 420)
    }

    private fun executeCandidates(candidates: List<String>) {
        invalidateRecognizer()
        val clean = candidates.map { stripWakePrefix(it).trim() }.filter { it.isNotBlank() }
        if (clean.isEmpty()) {
            handler.postDelayed({ startWakeListening() }, 350)
            return
        }

        val shortTarget = clean.first()
        val words = normalize(shortTarget).split(' ').filter { it.isNotBlank() }
        val openedAsApp = words.size <= 4 && !looksLikePhoneCommand(normalize(shortTarget)) &&
            AppLauncher(applicationContext).openByName(shortTarget)

        val reply = if (openedAsApp) {
            "J’ouvre $shortTarget."
        } else {
            when (val result = brain.replyCandidates(clean)) {
                is ZenoBrain.Result.Text -> result.text
                is ZenoBrain.Result.Action -> result.text
            }
        }

        broadcastState("speaking")
        Toast.makeText(this, "Zeno : $reply", Toast.LENGTH_LONG).show()
        val engine = tts
        if (engine == null) {
            handler.postDelayed({ startWakeListening() }, 700)
            return
        }
        runCatching { engine.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "zeno_reply") }
        val resumeDelay = (900L + reply.length * 28L).coerceIn(1300L, 4200L)
        handler.postDelayed({ startWakeListening() }, resumeDelay)
    }

    private fun looksLikePhoneCommand(text: String): Boolean = listOf(
        "allume", "eteins", "active", "desactive", "volume", "son", "wifi", "wi fi",
        "bluetooth", "luminosite", "rotation", "alarme", "reveil", "minuteur", "timer",
        "mode avion", "localisation", "gps", "silencieux", "vibreur", "ne pas deranger",
        "hotspot", "partage connexion", "nfc", "batterie", "parametres", "reglages"
    ).any { text.contains(it) }

    private fun stripWakePrefix(raw: String): String {
        val text = normalize(raw)
        val prefix = wakePhrases.firstOrNull { text.startsWith("$it ") }
        return if (prefix != null) text.removePrefix(prefix).trim() else raw
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean = candidates.any { normalize(it) in wakePhrases }

    private fun extractWakePayload(candidates: List<String>): String? {
        for (candidate in candidates) {
            val text = normalize(candidate)
            for (phrase in wakePhrases) {
                if (text == phrase) return ""
                if (text.startsWith("$phrase ")) return text.removePrefix(phrase).trim()
            }
        }
        return null
    }

    private fun normalize(value: String): String {
        val noAccents = Normalizer.normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents
            .replace('’', ' ')
            .replace('\'', ' ')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun showOverlayDot() {
        if (!Settings.canDrawOverlays(this) || overlayDot != null) return
        val wm = runCatching { getSystemService(WINDOW_SERVICE) as WindowManager }.getOrNull() ?: return
        val density = resources.displayMetrics.density
        val size = (11f * density).toInt().coerceAtLeast(9)
        val dot = View(this).apply {
            contentDescription = "Voyant Zeno"
            alpha = .86f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF168CFF.toInt())
                setStroke((1f * density).toInt().coerceAtLeast(1), 0x99FFFFFF.toInt())
            }
        }
        val params = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (7f * density).toInt()
        }
        runCatching { wm.addView(dot, params) }.onSuccess {
            overlayManager = wm
            overlayDot = dot
        }
    }

    private fun updateOverlayDot(state: String) {
        if (overlayDot == null && Settings.canDrawOverlays(this)) showOverlayDot()
        val dot = overlayDot ?: return
        val color = when (state) {
            "command" -> 0xFF24E6FF.toInt()
            "speaking" -> 0xFF9B5CFF.toInt()
            "permission", "error" -> 0xFFFF7043.toInt()
            "off" -> 0xFF607D8B.toInt()
            else -> 0xFF168CFF.toInt()
        }
        (dot.background as? GradientDrawable)?.setColor(color)
        dot.animate()
            .alpha(if (state == "command") 1f else .82f)
            .scaleX(if (state == "command") 1.45f else 1f)
            .scaleY(if (state == "command") 1.45f else 1f)
            .setDuration(150)
            .start()
    }

    private fun removeOverlayDot() {
        val dot = overlayDot
        val wm = overlayManager
        overlayDot = null
        overlayManager = null
        if (dot != null && wm != null) runCatching { wm.removeView(dot) }
    }

    private fun broadcastState(state: String) {
        updateOverlayDot(state)
        runCatching {
            sendBroadcast(Intent(ACTION_VOICE_STATE).setPackage(packageName).putExtra(EXTRA_STATE, state))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        invalidateRecognizer()
        removeOverlayDot()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        broadcastState("off")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DIRECT_COMMAND = "com.zeno.robot.action.DIRECT_COMMAND"
        const val ACTION_VOICE_STATE = "com.zeno.robot.action.VOICE_STATE"
        const val EXTRA_STATE = "voice_state"
        private const val CHANNEL_ID = "zeno_voice"
        private const val NOTIFICATION_ID = 4201
        private val wakePhrases = setOf(
            "zeno ouvre", "seno ouvre", "xeno ouvre", "zino ouvre", "zena ouvre"
        )
    }
}
'''
SERVICE.write_text(service, encoding='utf-8')

# --- ZenoBrain : fonctions téléphone supplémentaires accessibles à la voix.
brain = BRAIN.read_text(encoding='utf-8')

insert_point = '''        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager\n'''
if 'advancedPhoneCommand(text)?.let { return it }' not in brain:
    if insert_point not in brain:
        raise SystemExit('AudioManager introuvable dans ZenoBrain')
    brain = brain.replace(insert_point, '        advancedPhoneCommand(text)?.let { return it }\n\n' + insert_point, 1)

helper_anchor = '''    private fun containsToggleVerb(text: String): Boolean = listOf(\n'''
if 'private fun advancedPhoneCommand(text: String): Result?' not in brain:
    helpers = r'''    private fun advancedPhoneCommand(text: String): Result? {
        fun launch(action: String): Result {
            openSetting(action)
            return Result.Action("J’ouvre le réglage demandé.")
        }

        // Luminosité directe si l'utilisateur a accordé « Modifier les paramètres système ».
        val brightness = Regex("(?:luminosite|eclairage).*?(\\d{1,3})").find(text)
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (brightness != null) {
            if (!Settings.System.canWrite(context)) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                return Result.Action("Autorise Zeno à modifier les paramètres système, puis redemande la luminosité.")
            }
            val percent = brightness.coerceIn(1, 100)
            val value = ((percent / 100f) * 255f).toInt().coerceIn(1, 255)
            runCatching {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            }
            return Result.Action("Je règle la luminosité à $percent %.")
        }

        if ((text.contains("rotation") || text.contains("tourne l ecran") || text.contains("tourne ecran")) &&
            (text.contains("active") || text.contains("allume") || text.contains("desactive") || text.contains("coupe") || text.contains("eteins"))) {
            if (!Settings.System.canWrite(context)) {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return Result.Action("Autorise la modification des paramètres système pour commander la rotation.")
            }
            val enabled = !(text.contains("desactive") || text.contains("coupe") || text.contains("eteins"))
            runCatching { Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enabled) 1 else 0) }
            return Result.Action(if (enabled) "J’active la rotation automatique." else "Je désactive la rotation automatique.")
        }

        val timeoutMatch = Regex("(?:veille|ecran).*?(\\d{1,3})\\s*(seconde|secondes|minute|minutes)").find(text)
        if (timeoutMatch != null && (text.contains("veille") || text.contains("eteindre") || text.contains("extinction"))) {
            if (!Settings.System.canWrite(context)) {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return Result.Action("Autorise la modification des paramètres système pour régler la veille.")
            }
            val amount = timeoutMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: return null
            val millis = if (timeoutMatch.groupValues[2].startsWith("minute")) amount * 60_000 else amount * 1_000
            runCatching { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, millis) }
            return Result.Action("Je règle la mise en veille à ${timeoutMatch.groupValues[1]} ${timeoutMatch.groupValues[2]}.")
        }

        // Alarmes et minuteurs Android.
        val alarm = Regex("(?:alarme|reveil).*?(\\d{1,2})(?:\\s*h(?:eure)?s?\\s*|[:h])(\\d{1,2})?").find(text)
        if (alarm != null) {
            val hour = alarm.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
            val minute = alarm.groupValues.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
                .putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                .putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Zeno")
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(intent)
                Result.Action("Je programme l’alarme à %02d:%02d.".format(hour, minute))
            }.getOrElse { Result.Text("Je n’ai pas trouvé d’application d’alarme compatible.") }
        }

        val timer = Regex("(?:minuteur|timer).*?(\\d{1,3})\\s*(seconde|secondes|minute|minutes)").find(text)
        if (timer != null) {
            val amount = timer.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: return null
            val seconds = if (timer.groupValues[2].startsWith("minute")) amount * 60 else amount
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER)
                .putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Zeno")
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(intent)
                Result.Action("Je lance le minuteur de $amount ${timer.groupValues[2]}.")
            }.getOrElse { Result.Text("Je n’ai pas trouvé d’application de minuteur compatible.") }
        }

        if (text.contains("ouvre camera") || text.contains("ouvre la camera") || text.contains("lance camera") || text.contains("lance la camera")) {
            return runCatching {
                context.startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                Result.Action("J’ouvre la caméra.")
            }.getOrElse { Result.Text("Je n’arrive pas à ouvrir la caméra.") }
        }

        // Mode de sonnerie. Le mode silencieux complet peut nécessiter l'accès Ne pas déranger.
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (text.contains("mode vibreur") || text.contains("mets vibreur") || text.contains("mets en vibreur")) {
            return runCatching {
                audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                Result.Action("Je mets le téléphone en vibreur.")
            }.getOrElse {
                openSetting(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                Result.Action("Android demande l’accès Ne pas déranger pour modifier ce mode.")
            }
        }
        if (text.contains("mode normal") || text.contains("remets la sonnerie") || text.contains("active la sonnerie")) {
            return runCatching {
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Result.Action("Je remets la sonnerie normale.")
            }.getOrElse {
                openSetting(Settings.ACTION_SOUND_SETTINGS)
                Result.Action("J’ouvre les réglages de sonnerie.")
            }
        }

        if (text.contains("ne pas deranger")) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val enable = !(text.contains("desactive") || text.contains("coupe") || text.contains("eteins"))
            if (!nm.isNotificationPolicyAccessGranted) {
                openSetting(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                return Result.Action("Autorise l’accès Ne pas déranger à Zeno, puis redemande.")
            }
            runCatching {
                nm.setInterruptionFilter(
                    if (enable) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
            return Result.Action(if (enable) "J’active Ne pas déranger." else "Je désactive Ne pas déranger.")
        }

        // Raccourcis vers les réglages que les applications normales ne peuvent pas changer silencieusement.
        return when {
            (text.contains("hotspot") || text.contains("partage connexion") || text.contains("point d acces")) -> launch(Settings.ACTION_TETHER_SETTINGS)
            text.contains("nfc") -> launch(Settings.ACTION_NFC_SETTINGS)
            text.contains("notifications") && (text.contains("ouvre") || text.contains("reglage") || text.contains("parametre")) -> launch(Settings.ACTION_NOTIFICATION_SETTINGS)
            text.contains("batterie") && (text.contains("ouvre") || text.contains("reglage") || text.contains("economie")) -> launch(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            text.contains("accessibilite") -> launch(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            text.contains("sonnerie") && (text.contains("ouvre") || text.contains("reglage")) -> launch(Settings.ACTION_SOUND_SETTINGS)
            text.contains("clavier") && (text.contains("ouvre") || text.contains("reglage")) -> launch(Settings.ACTION_INPUT_METHOD_SETTINGS)
            text.contains("securite") && (text.contains("ouvre") || text.contains("reglage")) -> launch(Settings.ACTION_SECURITY_SETTINGS)
            text == "ouvre les parametres" || text == "ouvre parametres" || text == "ouvre les reglages" || text == "ouvre reglages" -> launch(Settings.ACTION_SETTINGS)
            else -> null
        }
    }

'''
    if helper_anchor not in brain:
        raise SystemExit('Point insertion helpers introuvable dans ZenoBrain')
    brain = brain.replace(helper_anchor, helpers + helper_anchor, 1)

# La nouvelle phrase est retirée avant l'analyse des commandes si elle arrive en une seule phrase.
brain = brain.replace(
    'val wakePrefixes = listOf("salut zeno ", "bonjour zeno ", "hey zeno ", "zeno ")',
    'val wakePrefixes = listOf("zeno ouvre ", "seno ouvre ", "xeno ouvre ", "salut zeno ", "bonjour zeno ", "hey zeno ", "zeno ")'
)
brain = brain.replace(
    '"salut zeno ", "salut zéno ", "bonjour zeno ", "bonjour zéno ",',
    '"zeno ouvre ", "zéno ouvre ", "seno ouvre ", "xeno ouvre ", "salut zeno ", "salut zéno ", "bonjour zeno ", "bonjour zéno ",'
)
brain = brain.replace(
    'Mode local actif. Essaie : ouvre YouTube et cherche musique, mets le volume à 50, ou monte le volume puis ouvre Spotify.',
    'Dis « Zeno ouvre », attends le voyant cyan, puis dis par exemple YouTube, allume la lampe, règle la luminosité à 40 ou mets un minuteur de 5 minutes.'
)
BRAIN.write_text(brain, encoding='utf-8')

# --- Accueil : phrase affichée + accès aux permissions système avancées.
main = MAIN.read_text(encoding='utf-8')
main = main.replace('Phrase fixe : Salut Zeno', 'Phrase fixe : Zeno ouvre')
main = main.replace('Salut Zeno', 'Zeno ouvre')
main = main.replace('Zeno Android v1.3.13', 'Zeno Android v1.3.14')
main = main.replace('Version 1.3.13', 'Version 1.3.14')

# Ajoute deux boutons au menu de permissions existant sans le réécrire complètement.
if 'Modifier les réglages système' not in main:
    marker = '''            Button(\n                onClick = {\n                    micGranted = hasMicrophone()\n'''
    block = r'''            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, tint = Color(0xFF59D8FF))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Modifier les réglages système", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Luminosité, rotation et veille", color = Color(0xFF9DB3DA), fontSize = 11.sp)
                }
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }) { Text("Autoriser") }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFF59D8FF))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Accès Ne pas déranger", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Pour silencieux / vibreur / Ne pas déranger", color = Color(0xFF9DB3DA), fontSize = 11.sp)
                }
                OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                }) { Text("Autoriser") }
            }

'''
    if marker not in main:
        raise SystemExit('Bouton Activer Zeno introuvable dans le menu permissions')
    main = main.replace(marker, block + marker, 1)

MAIN.write_text(main, encoding='utf-8')
print('Zeno 1.3.14 : phrase Zeno ouvre, écoute longue et commandes téléphone renforcées')
