from pathlib import Path
import re

BRAIN = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
MAIN = Path('app/src/main/java/com/zeno/robot/MainActivity.kt')

brain = BRAIN.read_text(encoding='utf-8')

# JSON Android natif pour lire les réponses météo / encyclopédie sans clé API.
if 'import org.json.JSONObject\n' not in brain:
    anchor = 'import java.net.URL\n'
    if anchor not in brain:
        raise SystemExit('Import URL introuvable dans ZenoBrain')
    brain = brain.replace(anchor, anchor + 'import org.json.JSONObject\n', 1)

# Les questions météo doivent être traitées avant l'ouverture des applications.
phone_anchor = '''        for (candidate in candidates) {
            handlePhoneCommand(candidate)?.let { return it }
        }

        val launcher = AppLauncher(context)
'''
weather_block = '''        for (candidate in candidates) {
            handlePhoneCommand(candidate)?.let { return it }
        }

        // Réponses vocales en ligne : la météo est lue directement par Zeno.
        for (candidate in candidates) {
            if (isWeatherQuestion(candidate)) {
                val weather = runCatching { weatherReply(candidate) }.getOrNull()
                return Result.Text(
                    weather ?: "Je n'arrive pas à récupérer la météo maintenant. Vérifie ta connexion internet et réessaie."
                )
            }
        }

        val launcher = AppLauncher(context)
'''
if phone_anchor not in brain:
    raise SystemExit('Point insertion météo introuvable')
brain = brain.replace(phone_anchor, weather_block, 1)

# Avant la réponse locale, répondre aussi à des questions simples de connaissance
# avec Wikipédia FR. Le backend Zeno, s'il est configuré, reste prioritaire.
local_anchor = '''        if (BuildConfig.ZENO_API_URL.isNotBlank()) {
            val remote = runCatching { callBackend(clean) }.getOrNull()
            if (!remote.isNullOrBlank()) return Result.Text(remote)
        }

        return Result.Text(localReply(lower))
'''
knowledge_block = '''        if (BuildConfig.ZENO_API_URL.isNotBlank()) {
            val remote = runCatching { callBackend(clean) }.getOrNull()
            if (!remote.isNullOrBlank()) return Result.Text(remote)
        }

        extractKnowledgeTopic(clean)?.let { topic ->
            val answer = runCatching { wikipediaReply(topic) }.getOrNull()
            if (!answer.isNullOrBlank()) return Result.Text(answer)
        }

        return Result.Text(localReply(lower))
'''
if local_anchor not in brain:
    raise SystemExit('Point insertion réponses web introuvable')
brain = brain.replace(local_anchor, knowledge_block, 1)

# Ajouter les fonctions réseau juste avant searchWeb().
function_anchor = '    fun searchWeb(query: String) {\n'
if function_anchor not in brain:
    raise SystemExit('searchWeb introuvable')

helpers = r'''    private fun isWeatherQuestion(raw: String): Boolean {
        val text = normalizeCommand(raw)
        return text.contains("meteo") ||
            text.contains("quel temps") ||
            text.contains("quelle temperature") ||
            text.contains("temperature a") ||
            text.contains("temperature pour") ||
            text.contains("temps fait il") ||
            text.contains("il fait quel temps")
    }

    private fun extractWeatherCity(raw: String): String? {
        var text = raw.trim()
        val normalizedPrefix = listOf(
            "salut zeno ", "bonjour zeno ", "hey zeno ", "zeno ", "zéno "
        )
        val lower = text.lowercase()
        normalizedPrefix.firstOrNull { lower.startsWith(it) }?.let {
            text = text.substring(it.length).trim()
        }

        val patterns = listOf(
            Regex("(?:météo|meteo)(?:\\s+(?:à|a|sur|pour))?\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("(?:quel temps|quelle météo|quelle meteo|quelle température|quelle temperature)(?:\\s+(?:fait-il|fait il))?(?:\\s+(?:à|a|sur|pour))?\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("il fait quel temps(?:\\s+(?:à|a|sur|pour))?\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("température(?:\\s+(?:à|a|sur|pour))\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("temperature(?:\\s+(?:à|a|sur|pour))\\s+(.+)$", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val city = match.groupValues.lastOrNull()?.trim().orEmpty()
                .removeSuffix(" aujourd'hui")
                .removeSuffix(" demain")
                .trim()
            if (city.isNotBlank() && normalizeCommand(city) !in setOf("meteo", "temps", "aujourd hui")) return city
        }
        return null
    }

    private fun weatherReply(raw: String): String? {
        val city = extractWeatherCity(raw)
        val location = city?.let { Uri.encode(it) }.orEmpty()
        val endpoint = if (location.isBlank()) {
            "https://wttr.in/?format=j1&lang=fr"
        } else {
            "https://wttr.in/$location?format=j1&lang=fr"
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", "ZenoAndroid/1.3")
            setRequestProperty("Accept", "application/json")
        }
        if (connection.responseCode !in 200..299) return null
        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val current = root.optJSONArray("current_condition")?.optJSONObject(0) ?: return null
        val nearest = root.optJSONArray("nearest_area")?.optJSONObject(0)
        val detectedPlace = nearest
            ?.optJSONArray("areaName")
            ?.optJSONObject(0)
            ?.optString("value")
            .orEmpty()
        val place = city ?: detectedPlace.ifBlank { "ta zone" }
        val temp = current.optString("temp_C").ifBlank { "?" }
        val feels = current.optString("FeelsLikeC").ifBlank { temp }
        val humidity = current.optString("humidity").ifBlank { "?" }
        val wind = current.optString("windspeedKmph").ifBlank { "?" }
        val description = current.optJSONArray("lang_fr")
            ?.optJSONObject(0)
            ?.optString("value")
            ?.takeIf { it.isNotBlank() }
            ?: current.optJSONArray("weatherDesc")
                ?.optJSONObject(0)
                ?.optString("value")
                .orEmpty()

        return buildString {
            append("À $place, il fait $temp degrés")
            if (description.isNotBlank()) append(", $description")
            append(". Ressenti $feels degrés, humidité $humidity pour cent")
            if (wind != "?") append(", vent $wind kilomètres heure")
            append(".")
        }
    }

    private fun extractKnowledgeTopic(raw: String): String? {
        val text = raw.trim()
        val normalized = normalizeCommand(text)
        val starters = listOf(
            "qui est ", "qui etait ", "c est qui ", "c est quoi ",
            "qu est ce que ", "explique moi ", "parle moi de "
        )
        val starter = starters.firstOrNull { normalized.startsWith(it) } ?: return null
        val topic = normalized.removePrefix(starter).trim()
        return topic.takeIf { it.length >= 2 }
    }

    private fun wikipediaReply(topic: String): String? {
        val endpoint = "https://fr.wikipedia.org/w/api.php" +
            "?action=query&generator=search&gsrsearch=${Uri.encode(topic)}" +
            "&gsrlimit=1&prop=extracts&exintro=1&explaintext=1&format=json"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("User-Agent", "ZenoAndroid/1.3")
            setRequestProperty("Accept", "application/json")
        }
        if (connection.responseCode !in 200..299) return null
        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val keys = pages.keys()
        if (!keys.hasNext()) return null
        val page = pages.optJSONObject(keys.next()) ?: return null
        val title = page.optString("title").ifBlank { topic }
        val extract = page.optString("extract").replace(Regex("\\s+"), " ").trim()
        if (extract.isBlank()) return null
        val short = if (extract.length <= 620) extract else extract.take(620).substringBeforeLast(' ').trimEnd() + "…"
        return "$title. $short"
    }

'''
brain = brain.replace(function_anchor, helpers + function_anchor, 1)
BRAIN.write_text(brain, encoding='utf-8')

main = MAIN.read_text(encoding='utf-8')
main = main.replace('Zeno Android v1.3.25', 'Zeno Android v1.3.26')
main = main.replace('Version 1.3.25', 'Version 1.3.26')
MAIN.write_text(main, encoding='utf-8')

print('Zeno 1.3.26 : météo parlée et réponses vocales de connaissance en ligne')
