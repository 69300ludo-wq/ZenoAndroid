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

    fun reply(message: String): Result {
        val clean = message.trim()
        val lower = clean.lowercase()
        val launcher = AppLauncher(context)

        extractOpenTarget(clean)?.let { target ->
            return if (launcher.openByName(target)) {
                Result.Action("J’ouvre $target pour toi.")
            } else {
                Result.Text("Je n’ai pas trouvé l’application « $target ». Ouvre l’onglet Mes applis pour vérifier son nom.")
            }
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
        val lower = text.lowercase()

        val wakePrefixes = listOf("salut zeno ", "salut zéno ", "bonjour zeno ", "bonjour zéno ", "zeno ", "zéno ")
        wakePrefixes.firstOrNull { lower.startsWith(it) }?.let {
            text = text.substring(it.length).trim()
        }

        val patterns = listOf(
            Regex("^(ouvre|lance|demarre|démarre)\\s+(moi\\s+)?(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(ouvre|lance|demarre|démarre)-moi\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^peux[- ]tu\\s+(ouvrir|lancer|demarrer|démarrer)\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^va\\s+sur\\s+(.+)$", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val groups = match.groupValues.drop(1).filter { it.isNotBlank() }
            val target = groups.lastOrNull()?.trim().orEmpty()
            if (target.isNotBlank()) return target
        }
        return null
    }

    fun searchWeb(query: String) {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun localReply(lower: String): String = when {
        "bonjour" in lower || "salut" in lower -> "Bonjour ! Je suis Zeno. Je peux lancer tes applications, écouter ta voix, traduire et faire des recherches sur le web."
        "qui es" in lower -> "Je suis Zeno, ton compagnon IA Android personnalisable."
        "heure" in lower -> "Je peux te donner l’heure via ton téléphone et t’aider à créer des rappels dans une prochaine version."
        "merci" in lower -> "Avec plaisir 💙"
        else -> "J’ai compris : « $lower ». Mon mode local est actif. Pour des réponses IA complètes, connecte-moi à ton backend Zeno dans la configuration du projet."
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
