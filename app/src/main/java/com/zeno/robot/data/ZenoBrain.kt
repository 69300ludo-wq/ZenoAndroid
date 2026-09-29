package com.zeno.robot.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.zeno.robot.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

class ZenoBrain(private val context: Context) {
    sealed interface Result {
        data class Text(val text: String) : Result
        data class Action(val text: String) : Result
    }

    fun reply(message: String): Result = replyCandidates(listOf(message))

    fun replyCandidates(messages: List<String>): Result {
        val candidates = messages.map { it.trim() }.filter { it.isNotBlank() }
        if (candidates.isEmpty()) return Result.Text("Je n’ai rien entendu.")

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
        else -> "Mode local actif. Dis par exemple : ouvre YouTube, ouvre WhatsApp ou cherche quelque chose sur le web."
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
