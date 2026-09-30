from pathlib import Path

P = Path('app/src/main/java/com/zeno/robot/data/ZenoBrain.kt')
s = P.read_text(encoding='utf-8')

needle = '''        if (candidates.isEmpty()) return Result.Text("Je n’ai rien entendu.")

        // Les commandes du téléphone passent avant l'ouverture d'applications.
'''
replacement = '''        if (candidates.isEmpty()) return Result.Text("Je n’ai rien entendu.")

        // Commandes naturelles spéciales, par exemple :
        // « ouvre YouTube et cherche musique années 80 ».
        if (candidates.size == 1) {
            handleMediaSearch(candidates.first())?.let { return it }

            // Autorise plusieurs actions dans une seule phrase :
            // « monte le volume puis ouvre Spotify ensuite allume la lampe ».
            val sequence = splitCommandSequence(candidates.first())
            if (sequence.size > 1) {
                val replies = mutableListOf<String>()
                var actionDone = false
                sequence.take(4).forEach { command ->
                    when (val result = replyCandidates(listOf(command))) {
                        is Result.Action -> {
                            actionDone = true
                            replies += result.text
                        }
                        is Result.Text -> replies += result.text
                    }
                }
                val text = replies.joinToString(" ").ifBlank { "C’est fait." }
                return if (actionDone) Result.Action(text) else Result.Text(text)
            }
        }

        // Les commandes du téléphone passent avant l'ouverture d'applications.
'''
if needle not in s:
    raise SystemExit('Point insertion commandes intelligentes introuvable')
s = s.replace(needle, replacement, 1)

needle = '''        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return when {
'''
replacement = '''        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // « mets le volume à 50 », « volume 30 pour cent », etc.
        val volumePercent = Regex("(?:volume|son).*?(\\\\d{1,3})(?:\\\\s*(?:pour\\\\s*cent))?")
            .find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (volumePercent != null && (text.contains("volume") || text.contains("son"))) {
            val percent = volumePercent.coerceIn(0, 100)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val level = ((percent / 100f) * max).toInt().coerceIn(0, max)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, AudioManager.FLAG_SHOW_UI)
            return Result.Action("Je règle le volume à $percent %.")
        }

        return when {
'''
if needle not in s:
    raise SystemExit('Point insertion volume introuvable')
s = s.replace(needle, replacement, 1)

needle = '''    private fun containsToggleVerb(text: String): Boolean = listOf(
'''
helpers = '''    private fun splitCommandSequence(raw: String): List<String> {
        return raw.split(
            Regex("\\\\s+(?:(?:et)\\\\s+)?(?:puis|ensuite|apres|après)\\\\s+", RegexOption.IGNORE_CASE)
        ).map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun handleMediaSearch(raw: String): Result? {
        var text = raw.trim()
        text = text.replace(
            Regex("^(?:salut\\\\s+|bonjour\\\\s+|hey\\\\s+)?z[eé]no\\\\s+", RegexOption.IGNORE_CASE),
            ""
        ).trim()

        val openYoutube = Regex(
            "^(?:ouvre|lance|demarre|démarre)(?:-moi|\\\\s+moi)?\\\\s+(?:l['’ ]?application\\\\s+)?(?:youtube|you tube)\\\\s+(?:(?:et|puis|et puis)\\\\s+)?(?:cherche|recherche)\\\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(text)
        val searchYoutube = Regex(
            "^(?:cherche|recherche)\\\\s+(.+?)\\\\s+sur\\\\s+(?:youtube|you tube)$",
            RegexOption.IGNORE_CASE
        ).find(text)

        val query = when {
            openYoutube != null -> openYoutube.groupValues[1].trim()
            searchYoutube != null -> searchYoutube.groupValues[1].trim()
            else -> return null
        }
        if (query.isBlank()) return null

        val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result.Action("J’ouvre YouTube et je cherche $query.")
        }.getOrElse {
            searchWeb(query)
            Result.Action("Je cherche $query sur le web.")
        }
    }

    private fun containsToggleVerb(text: String): Boolean = listOf(
'''
if needle not in s:
    raise SystemExit('Point insertion helpers introuvable')
s = s.replace(needle, helpers, 1)

# Rend le message d'aide plus représentatif des nouvelles commandes.
s = s.replace(
    'Mode local actif. Dis par exemple : ouvre YouTube, allume la lampe, monte le volume ou active le Wi‑Fi.',
    'Mode local actif. Essaie : ouvre YouTube et cherche musique, mets le volume à 50, ou monte le volume puis ouvre Spotify.'
)

P.write_text(s, encoding='utf-8')
print('Commandes naturelles, volume en pourcentage et commandes enchaînées ajoutées')
