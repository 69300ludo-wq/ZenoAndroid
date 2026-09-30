package com.zeno.robot.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.zeno.robot.BuildConfig
import com.zeno.robot.SetupActivity
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

class ZenoBrain(private val context: Context) {
    sealed interface Result {
        data class Text(val text: String) : Result
        data class Action(val text: String) : Result
    }

    fun reply(message: String): Result = replyCandidates(listOf(message))

    fun replyCandidates(messages: List<String>): Result {
        val candidates = messages.map { it.trim() }.filter { it.isNotBlank() }
        if (candidates.isEmpty()) return Result.Text("Je n’ai rien entendu.")

        // Les commandes du téléphone passent avant l'ouverture d'applications.
        for (candidate in candidates) {
            handlePhoneCommand(candidate)?.let { return it }
        }

        val launcher = AppLauncher(context)

        // On teste toutes les propositions du moteur vocal, pas seulement la première.
        for (candidate in candidates) {
            val target = extractOpenTarget(candidate)
            if (!target.isNullOrBlank() && launcher.openByName(target)) {
                return Result.Action("J’ouvre $target pour toi.")
            }
        }

        // Si l'utilisateur prononce seulement le nom de l'appli : « YouTube », « WhatsApp », etc.
        for (candidate in candidates) {
            if (candidate.split(Regex("\\s+")).size <= 4 && launcher.openByName(candidate)) {
                return Result.Action("J’ouvre ${candidate.trim()} pour toi.")
            }
        }

        val clean = candidates.first()
        val lower = clean.lowercase()

        extractOpenTarget(clean)?.let { target ->
            return Result.Text("Je n’ai pas trouvé l’application « $target ». Vérifie son nom dans Mes applis.")
        }

        if (lower.startsWith("cherche ") || lower.startsWith("recherche ")) {
            val query = clean.substringAfter(" ").removeSuffix(" sur internet").trim()
            searchWeb(query)
            return Result.Action("Je lance une recherche sur le web pour « $query ».")
        }

        if (BuildConfig.ZENO_API_URL.isNotBlank()) {
            val remote = runCatching { callBackend(clean) }.getOrNull()
            if (!remote.isNullOrBlank()) return Result.Text(remote)
        }

        return Result.Text(localReply(lower))
    }

    private fun normalizeCommand(value: String): String {
        val noAccent = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return noAccent
            .replace('’', ' ')
            .replace('\'', ' ')
            .replace('-', ' ')
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun handlePhoneCommand(raw: String): Result? {
        var text = normalizeCommand(raw)
        val wakePrefixes = listOf("salut zeno ", "bonjour zeno ", "hey zeno ", "zeno ")
        wakePrefixes.firstOrNull { text.startsWith(it) }?.let { text = text.removePrefix(it).trim() }

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return when {
            text.contains("allume la lampe") || text.contains("allume lampe") ||
                text.contains("active la lampe") || text.contains("active lampe") ||
                text.contains("allume le flash") || text.contains("active le flash") -> setTorch(true)

            text.contains("eteins la lampe") || text.contains("eteint la lampe") ||
                text.contains("desactive la lampe") || text.contains("coupe la lampe") ||
                text.contains("eteins le flash") || text.contains("desactive le flash") -> setTorch(false)

            text.contains("monte le volume") || text.contains("augmente le volume") ||
                text.contains("plus fort") -> {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                Result.Action("J’augmente le volume.")
            }

            text.contains("baisse le volume") || text.contains("diminue le volume") ||
                text.contains("moins fort") -> {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                Result.Action("Je baisse le volume.")
            }

            text.contains("coupe le son") || text.contains("mets en silencieux") ||
                text == "silencieux" -> {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                Result.Action("J’ai coupé le son multimédia.")
            }

            text.contains("remets le son") || text.contains("reactive le son") ||
                text.contains("active le son") -> {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                Result.Action("J’ai remis le son multimédia.")
            }

            hasToggle(text, "wifi") || hasToggle(text, "wi fi") -> {
                openSetting(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)
                Result.Action("J’ouvre le contrôle Wi‑Fi. Android te demande de confirmer le changement.")
            }

            hasToggle(text, "bluetooth") -> {
                openSetting(Settings.ACTION_BLUETOOTH_SETTINGS)
                Result.Action("J’ouvre le contrôle Bluetooth. Android te demande de confirmer le changement.")
            }

            (text.contains("donnees mobiles") || text.contains("data mobile") || text.contains("internet mobile")) &&
                containsToggleVerb(text) -> {
                openSetting(Settings.ACTION_WIRELESS_SETTINGS)
                Result.Action("J’ouvre les réglages réseau pour les données mobiles. Android te demande de confirmer.")
            }

            (text.contains("mode avion") || text.contains("avion")) && containsToggleVerb(text) -> {
                openSetting(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                Result.Action("J’ouvre le réglage du mode avion. Android te demande de confirmer.")
            }

            (text.contains("localisation") || text.contains("gps")) && containsToggleVerb(text) -> {
                openSetting(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                Result.Action("J’ouvre le réglage de localisation. Android te demande de confirmer.")
            }

            text.contains("luminosite") && (text.contains("regle") || text.contains("change") || containsToggleVerb(text)) -> {
                openSetting(Settings.ACTION_DISPLAY_SETTINGS)
                Result.Action("J’ouvre le réglage de luminosité.")
            }

            else -> null
        }
    }

    private fun containsToggleVerb(text: String): Boolean = listOf(
        "active", "allume", "mets", "ouvre", "desactive", "coupe", "eteins", "eteint"
    ).any { text.contains(it) }

    private fun hasToggle(text: String, feature: String): Boolean =
        text.contains(feature) && containsToggleVerb(text)

    private fun openSetting(action: String) {
        runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun setTorch(enabled: Boolean): Result {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            context.startActivity(Intent(context, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return Result.Action("Autorise la caméra une fois pour que je puisse commander la lampe.")
        }

        return runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.Text("Je n’ai pas trouvé de lampe sur ce téléphone.")
            manager.setTorchMode(cameraId, enabled)
            Result.Action(if (enabled) "J’allume la lampe." else "J’éteins la lampe.")
        }.getOrElse {
            Result.Text("Je n’arrive pas à commander la lampe pour le moment.")
        }
    }

    private fun extractOpenTarget(message: String): String? {
        var text = message.trim()
        var lower = text.lowercase()

        val wakePrefixes = listOf(
            "salut zeno ", "salut zéno ", "bonjour zeno ", "bonjour zéno ",
            "hey zeno ", "hey zéno ", "zeno ", "zéno "
        )
        wakePrefixes.firstOrNull { lower.startsWith(it) }?.let {
            text = text.substring(it.length).trim()
            lower = text.lowercase()
        }

        val patterns = listOf(
            Regex("^(ouvre|lance|demarre|démarre)\\s+(moi\\s+)?(l application\\s+|l'appli\\s+|l appli\\s+|application\\s+|appli\\s+)?(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(ouvre|lance|demarre|démarre)-moi\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^peux[- ]tu\\s+(ouvrir|lancer|demarrer|démarrer)\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(va|vas)\\s+sur\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^mets[- ]moi\\s+(.+)$", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val target = match.groupValues.drop(1).lastOrNull { it.isNotBlank() }?.trim().orEmpty()
            if (target.isNotBlank()) return target
        }

        // Formulation naturelle : « tu peux m'ouvrir YouTube »
        val natural = Regex(".*\\b(ouvrir|lancer)\\b\\s+(?:moi\\s+)?(?:l application\\s+|l'appli\\s+|l appli\\s+|application\\s+|appli\\s+)?(.+)$", RegexOption.IGNORE_CASE)
            .find(text)
        if (natural != null) return natural.groupValues.last().trim()

        return null
    }

    fun searchWeb(query: String) {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun localReply(lower: String): String = when {
        "bonjour" in lower || "salut" in lower -> "Bonjour ! Je suis Zeno. Mon mode local est actif."
        "qui es" in lower -> "Je suis Zeno, ton compagnon IA Android personnalisable."
        "heure" in lower -> "Je peux lancer tes applications, traduire et faire des recherches sur le web."
        "merci" in lower -> "Avec plaisir 💙"
        else -> "Mode local actif. Dis par exemple : ouvre YouTube, allume la lampe, monte le volume ou active le Wi‑Fi."
    }

    private fun callBackend(message: String): String? {
        val endpoint = BuildConfig.ZENO_API_URL.trimEnd('/') + "/chat"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val escaped = message.replace("\\", "\\\\").replace("\"", "\\\"")
        connection.outputStream.bufferedWriter().use { it.write("{\"message\":\"$escaped\"}") }
        if (connection.responseCode !in 200..299) return null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return Regex("\\\"reply\\\"\\s*:\\s*\\\"(.*?)\\\"")
            .find(body)?.groupValues?.getOrNull(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
    }
}
