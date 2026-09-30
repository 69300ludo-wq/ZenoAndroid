from pathlib import Path

SERVICE = Path('app/src/main/java/com/zeno/robot/service/FloatingZenoService.kt')
s = SERVICE.read_text(encoding='utf-8')

start = s.find('    private fun containsWakePhrase(candidates: List<String>): Boolean')
end = s.find('    private fun triggerWakePhrase()', start)
if start < 0 or end < 0:
    raise SystemExit('Bloc de détection de la phrase de réveil introuvable')

replacement = '''    private fun closeWakeWord(heard: String, expected: String): Boolean {
        if (heard == expected) return true
        if (heard.length < 3 || expected.length < 3) return false
        if (kotlin.math.abs(heard.length - expected.length) > 1) return false

        var i = 0
        var j = 0
        var edits = 0
        while (i < heard.length && j < expected.length) {
            if (heard[i] == expected[j]) {
                i++
                j++
            } else {
                edits++
                if (edits > 1) return false
                when {
                    heard.length > expected.length -> i++
                    expected.length > heard.length -> j++
                    else -> { i++; j++ }
                }
            }
        }
        if (i < heard.length || j < expected.length) edits++
        return edits <= 1
    }

    private fun containsWakePhrase(candidates: List<String>): Boolean {
        val wake = normalizeWake(currentWakePhrase())
        if (wake.isBlank()) return false
        val words = wake.split(" ").filter { it.isNotBlank() }
        val keyword = words.lastOrNull { it.length >= 4 } ?: words.lastOrNull().orEmpty()

        return candidates.any { raw ->
            val heard = normalizeWake(raw)
            if (heard.isBlank()) return@any false

            // Détection normale : la phrase complète a été comprise.
            if (heard.contains(wake)) return@any true

            // Mode voix basse : le moteur Android peut n'entendre que le mot distinctif
            // de la phrase (par ex. « Zeno » dans « Salut Zeno »). On l'accepte aussi,
            // avec une petite tolérance d'une lettre pour les chuchotements.
            if (keyword.length >= 4) {
                val heardWords = heard.split(" ").filter { it.isNotBlank() }
                if (heardWords.any { closeWakeWord(it, keyword) }) return@any true
            }
            false
        }
    }

'''
s = s[:start] + replacement + s[end:]

# Plus de propositions augmente les chances de récupérer correctement une voix faible.
s = s.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 12)')
s = s.replace('putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)', 'putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 12)')

SERVICE.write_text(s, encoding='utf-8')
print('Mode voix basse activé : détection plus tolérante de la phrase de réveil')
