from pathlib import Path
import re

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')
LAUNCHER = Path('app/src/main/java/com/zeno/robot/data/AppLauncher.kt')

service = r'''package com.zeno.robot.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.zeno.robot.MainActivity
import com.zeno.robot.R
import com.zeno.robot.data.ZenoBrain
import java.text.Normalizer
import java.util.Locale

/**
 * Service vocal Zeno sans overlay.
 *
 * Important : le réveil et la commande n'utilisent jamais le même SpeechRecognizer
 * en même temps. Le moteur est détruit puis recréé entre les deux phases pour éviter
 * l'état BUSY rencontré sur certains téléphones.
 */
class FloatingZenoService : Service() {
    private enum class Mode { WAKE, COMMAND }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var sessionSerial = 0
    private var mode = Mode.WAKE
    private var commandRetries = 0
    private lateinit var brain: ZenoBrain

    override fun onCreate() {
        super.onCreate()
        if (!hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcastState("permission")
            stopSelf()
            return
        }

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

        tts = runCatching {
            TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching { tts?.language = Locale.FRENCH }
                }
            }
        }.getOrNull()

        broadcastState("idle")
        handler.postDelayed({ startWakeListening() }, 450)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasMic()) {
            broadcastState("permission")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_DIRECT_COMMAND) {
            handler.post { beginCommandListening(resetRetry = true) }
        } else {
            handler.postDelayed({ startWakeListening() }, 250)
        }
        return START_NOT_STICKY
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
            .setContentText("Dis « Salut Zeno » ou touche le micro dans l’accueil.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_description) }
        )
    }

    private fun recognitionIntent(partial: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
            if (!partial) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1300L)
            }
        }

    private fun invalidateRecognizer() {
        sessionSerial += 1
        val old = recognizer
        recognizer = null
        runCatching { old?.cancel() }
        runCatching { old?.destroy() }
    }

    private fun createRecognizer(listener: RecognitionListener): SpeechRecognizer? =
        runCatching {
            SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
            }
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
                    else -> 350L
                }
                handler.postDelayed({ startWakeListening() }, delay)
            }

            override fun onResults(results: Bundle?) {
                if (token != sessionSerial || mode != Mode.WAKE) return
                val candidates = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                if (containsWakePhrase(candidates)) {
                    beginCommandListening(resetRetry = true)
                } else {
                    handler.postDelayed({ startWakeListening() }, 250)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (token != sessionSerial || mode != Mode.WAKE) return
                val candidates = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                if (containsWakePhrase(candidates)) beginCommandListening(resetRetry = true)
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
        runCatching { current.startListening(recognitionIntent(partial = true)) }
            .onFailure {
                broadcastState("error")
                handler.postDelayed({ startWakeListening() }, 1000)
            }
    }

    private fun beginCommandListening(resetRetry: Boolean) {
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

        if (resetRetry) commandRetries = 0
        mode = Mode.COMMAND
        invalidateRecognizer()
        broadcastState("command")

        handler.postDelayed({
            if (mode != Mode.COMMAND) return@postDelayed
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
                    if (token != sessionSerial || mode != Mode.COMMAND) return
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        broadcastState("permission")
                        return
                    }
                    if (commandRetries < 1 && error != SpeechRecognizer.ERROR_CLIENT) {
                        commandRetries += 1
                        handler.postDelayed({ beginCommandListening(resetRetry = false) }, 450)
                    } else {
                        Toast.makeText(this@FloatingZenoService, "Je n’ai pas bien entendu. Réessaie.", Toast.LENGTH_SHORT).show()
                        broadcastState("idle")
                        handler.postDelayed({ startWakeListening() }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (token != sessionSerial || mode != Mode.COMMAND) return
                    val candidates = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    if (candidates.none { it.isNotBlank() }) {
                        if (commandRetries < 1) {
                            commandRetries += 1
                            handler.postDelayed({ beginCommandListening(resetRetry = false) }, 350)
                        } else {
                            broadcastState("idle")
                            handler.postDelayed({ startWakeListening() }, 450)
                        }
                        return
                    }
                    invalidateRecognizer()
                    handleCommand(candidates)
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
            runCatching { current.startListening(recognitionIntent(partial = false)) }
                .onFailure {
                    broadcastState("error")
                    handler.postDelayed({ startWakeListening() }, 800)
                }
        }, 380)
    }

    private fun handleCommand(candidates: List<String>) {
        val heard = candidates.firstOrNull()?.trim().orEmpty()
        if (heard.isNotBlank()) {
            Toast.makeText(this, "Vous : $heard", Toast.LENGTH_SHORT).show()
        }

        val reply = when (val result = brain.replyCandidates(candidates)) {
            is ZenoBrain.Result.Text -> result.text
            is ZenoBrain.Result.Action -> result.text
        }
        broadcastState("speaking")
        Toast.makeText(this, "Zeno : $reply", Toast.LENGTH_LONG).show()

        val engine = tts
        if (engine == null) {
            handler.postDelayed({ startWakeListening() }, 650)
            return
        }
        runCatching { engine.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "zeno_reply") }
        val resumeDelay = (1000L + reply.length * 32L).coerceIn(1400L, 4200L)
        handler.postDelayed({ startWakeListening() }, resumeDelay)
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean = candidates.any { raw ->
        val text = normalize(raw)
        text.contains("salut zeno") ||
            text.contains("salut seno") ||
            text.contains("salut xeno") ||
            text.contains("bonjour zeno") ||
            text.contains("hey zeno") ||
            text == "zeno"
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

    private fun broadcastState(state: String) {
        runCatching {
            sendBroadcast(
                Intent(ACTION_VOICE_STATE)
                    .setPackage(packageName)
                    .putExtra(EXTRA_STATE, state)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        invalidateRecognizer()
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
    }
}
'''
SERVICE.write_text(service, encoding='utf-8')

launcher = r'''package com.zeno.robot.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.zeno.robot.model.InstalledApp
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

class AppLauncher(private val context: Context) {
    private val packageManager get() = context.packageManager

    fun listLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                InstalledApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { normalize(it.label) }
    }

    fun openByPackage(packageName: String): Boolean = runCatching {
        packageManager.getLaunchIntentForPackage(packageName)?.let { direct ->
            direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(direct)
            return true
        }

        val probe = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        val resolved = packageManager.queryIntentActivities(probe, PackageManager.MATCH_ALL).firstOrNull()
            ?: return false
        val explicit = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(explicit)
        true
    }.getOrDefault(false)

    /** Ouvre n'importe quelle application possédant une icône dans le lanceur Android. */
    fun openByName(name: String): Boolean {
        val target = cleanTarget(name)
        if (target.isBlank()) return false

        knownPackages[target]?.let { if (openByPackage(it)) return true }

        val apps = listLaunchableApps()
        if (apps.isEmpty()) return false

        val best = apps
            .map { app -> app to score(target, normalize(app.label)) }
            .maxByOrNull { it.second }
            ?: return false

        // Le seuil reste prudent pour ne pas ouvrir une mauvaise application,
        // mais accepte les petites erreurs de transcription du moteur vocal.
        if (best.second < 58) return false
        return openByPackage(best.first.packageName)
    }

    private fun score(target: String, label: String): Int {
        if (target == label) return 100
        val compactTarget = target.replace(" ", "")
        val compactLabel = label.replace(" ", "")
        if (compactTarget == compactLabel) return 99
        if (label.startsWith(target) || target.startsWith(label)) return 94
        if (label.contains(target) || target.contains(label)) return 88
        if (compactLabel.contains(compactTarget) || compactTarget.contains(compactLabel)) return 88

        val targetWords = target.split(' ').filter { it.length > 1 }.toSet()
        val labelWords = label.split(' ').filter { it.length > 1 }.toSet()
        val common = targetWords.intersect(labelWords).size
        if (common > 0) {
            val coverage = (common * 30) / max(targetWords.size, labelWords.size).coerceAtLeast(1)
            return 64 + coverage
        }

        val distance = levenshtein(compactTarget, compactLabel)
        val longest = max(compactTarget.length, compactLabel.length).coerceAtLeast(1)
        val similarity = 100 - (distance * 100 / longest)
        return if (similarity >= 68) similarity else 0
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }
            previous = current
        }
        return previous[b.length]
    }

    private fun cleanTarget(raw: String): String {
        var value = normalize(raw)
            .replace(Regex("[.!?,;:]+$"), "")
            .trim()

        val prefixes = listOf(
            "salut zeno ", "bonjour zeno ", "hey zeno ", "zeno ",
            "ouvre moi l application ", "ouvre l application ",
            "ouvre moi l appli ", "ouvre l appli ",
            "ouvre moi ", "ouvre ", "lance moi ", "lance ",
            "demarre moi ", "demarre ", "va sur ",
            "application ", "appli ", "le ", "la ", "les "
        )
        var changed = true
        while (changed) {
            changed = false
            prefixes.firstOrNull { value.startsWith(it) }?.let {
                value = value.removePrefix(it).trim()
                changed = true
            }
        }

        value = value
            .removeSuffix(" s il te plait")
            .removeSuffix(" stp")
            .removeSuffix(" merci")
            .trim()

        return aliases[value] ?: value
    }

    private fun normalize(value: String): String {
        val noAccents = Normalizer.normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccents
            .replace('’', ' ')
            .replace('\'', ' ')
            .replace('-', ' ')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private val aliases = mapOf(
            "you tube" to "youtube",
            "youtube musique" to "youtube music",
            "whats app" to "whatsapp",
            "ouatsap" to "whatsapp",
            "snap" to "snapchat",
            "snap chat" to "snapchat",
            "insta" to "instagram",
            "tik tok" to "tiktok",
            "spot ify" to "spotify",
            "google chrome" to "chrome",
            "google map" to "maps",
            "google maps" to "maps",
            "messenger facebook" to "messenger"
        )

        private val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "whatsapp" to "com.whatsapp",
            "snapchat" to "com.snapchat.android",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "messenger" to "com.facebook.orca",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "tiktok" to "com.zhiliaoapp.musically"
        )
    }
}
'''
LAUNCHER.write_text(launcher, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')

# Le voyant reçoit maintenant l'état réel du service vocal, même si celui-ci tourne
# dans le processus :voice. Il reste visible sur l'accueil et les autres écrans.
pattern = re.compile(
    r'@Composable\nprivate fun BoxScope\.VoiceStatusDot\(\) \{.*?\n\}\n\n',
    re.S,
)
indicator = r'''@Composable
private fun BoxScope.VoiceStatusDot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_indicator", Context.MODE_PRIVATE) }
    var revision by remember { mutableIntStateOf(0) }
    val micGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var voiceState by remember { mutableStateOf(if (micGranted) "idle" else "permission") }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == FloatingZenoService.ACTION_VOICE_STATE) {
                    voiceState = intent.getStringExtra(FloatingZenoService.EXTRA_STATE) ?: "idle"
                }
            }
        }
        val filter = android.content.IntentFilter(FloatingZenoService.ACTION_VOICE_STATE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision += 1 }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    if (revision < 0) return

    val size = prefs.getInt("indicator_size", 9).coerceIn(6, 18)
    val opacity = prefs.getFloat("indicator_opacity", .92f).coerceIn(.35f, 1f)
    val selectedColor = Color(prefs.getLong("indicator_color", 0xFF168CFFL))
    val position = prefs.getString("indicator_position", "center") ?: "center"
    val userPulse = prefs.getBoolean("indicator_pulse", true)

    val active = voiceState == "wake" || voiceState == "command" || voiceState == "speaking"
    val baseColor = when (voiceState) {
        "command" -> Color(0xFF24E6FF)
        "error", "permission" -> Color(0xFFFF7043)
        "off" -> Color(0xFF607D8B)
        else -> selectedColor
    }
    val targetOpacity = when (voiceState) {
        "command" -> 1f
        "wake" -> opacity
        "speaking" -> .78f
        "error", "permission" -> .88f
        "off" -> .38f
        else -> .58f
    }
    val pulse = userPulse && active
    val alpha = if (pulse) {
        val transition = rememberInfiniteTransition(label = "zenoVoiceDot")
        val animated by transition.animateFloat(
            initialValue = (targetOpacity * .55f).coerceAtLeast(.28f),
            targetValue = targetOpacity,
            animationSpec = infiniteRepeatable(tween(if (voiceState == "command") 420 else 760), RepeatMode.Reverse),
            label = "zenoVoiceDotAlpha"
        )
        animated
    } else targetOpacity

    val align = when (position) {
        "left" -> Alignment.TopStart
        "right" -> Alignment.TopEnd
        else -> Alignment.TopCenter
    }

    Box(
        Modifier
            .align(align)
            .padding(
                top = 7.dp,
                start = if (position == "left") 72.dp else 0.dp,
                end = if (position == "right") 72.dp else 0.dp
            )
            .size((size + if (voiceState == "command") 3 else 0).dp)
            .zIndex(100f)
            .background(baseColor.copy(alpha = alpha), CircleShape)
            .clickable { startVoiceCommand(context) }
    )
}

'''
main, count = pattern.subn(indicator, main, count=1)
if count != 1:
    raise SystemExit('VoiceStatusDot introuvable')

# Le bouton de l'accueil et le voyant utilisent l'action publique du service final.
main = main.replace(
    '.setAction("com.zeno.robot.action.DIRECT_COMMAND")',
    '.setAction(FloatingZenoService.ACTION_DIRECT_COMMAND)'
)
main = main.replace('Zeno Android v1.3.11', 'Zeno Android v1.3.12')
main = main.replace('Version 1.3.11', 'Version 1.3.12')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.12 : vocal accueil renforcé, voyant dynamique et ouverture de toutes les applis')
