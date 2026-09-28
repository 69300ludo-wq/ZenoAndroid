package com.zeno.robot.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.zeno.robot.model.InstalledApp
import java.text.Normalizer
import java.util.Locale

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

    fun openByPackage(packageName: String): Boolean {
        return runCatching {
            val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return false
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }

    fun openByName(name: String): Boolean {
        val target = cleanTarget(name)
        if (target.isBlank()) return false

        knownPackages[target]?.let { packageName ->
            if (openByPackage(packageName)) return true
        }

        val apps = listLaunchableApps()
        val exact = apps.firstOrNull { normalize(it.label) == target }
        if (exact != null && openByPackage(exact.packageName)) return true

        val starts = apps.firstOrNull {
            val label = normalize(it.label)
            label.startsWith(target) || target.startsWith(label)
        }
        if (starts != null && openByPackage(starts.packageName)) return true

        val contains = apps.firstOrNull {
            val label = normalize(it.label)
            label.contains(target) || target.contains(label)
        }
        if (contains != null && openByPackage(contains.packageName)) return true

        val targetWords = target.split(' ').filter { it.length > 1 }.toSet()
        val fuzzy = apps
            .map { app ->
                val labelWords = normalize(app.label).split(' ').filter { it.length > 1 }.toSet()
                app to targetWords.intersect(labelWords).size
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first

        return fuzzy?.let { openByPackage(it.packageName) } ?: false
    }

    private fun cleanTarget(raw: String): String {
        var value = normalize(raw)
            .replace(Regex("[.!?,;:]+$"), "")
            .trim()

        val prefixes = listOf(
            "l application ", "lapp ", "application ", "appli ",
            "le ", "la ", "les "
        )
        prefixes.firstOrNull { value.startsWith(it) }?.let { value = value.removePrefix(it).trim() }

        value = value
            .removeSuffix(" s il te plait")
            .removeSuffix(" s'il te plait")
            .removeSuffix(" stp")
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
            "whats app" to "whatsapp",
            "snap" to "snapchat",
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
