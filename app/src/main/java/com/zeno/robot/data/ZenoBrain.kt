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

        val openPrefixes = listOf("ouvre ", "lance ", "démarre ", "demarre ")
        val prefix = openPrefixes.firstOrNull { lower.startsWith(it) }
        if (prefix != null) {
            val target = clean.substring(prefix.length).trim()
            return if (launcher.openByName(target)) {
                Result.Action("J’ouvre $target pour toi.")
            } else {
                Result.Text("Je n’ai pas trouvé l’application « $target ». Tu peux la choisir dans l’onglet Applications.")
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
