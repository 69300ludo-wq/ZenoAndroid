package com.zeno.robot.data

import android.content.ComponentName
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
            val direct = packageManager.getLaunchIntentForPackage(packageName)
            if (direct != null) {
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
    }

    fun openByName(name: String): Boolean {
        val target = cleanTarget(name)
        if (target.isBlank()) return false

        knownPackages[target]?.let { packageName ->
            if (openByPackage(packageName)) return true
        }

        val apps = listLaunchableApps()
        if (apps.isEmpty()) return false

        val scored = apps.map { app ->
            app to score(target, normalize(app.label))
        }.sortedByDescending { it.second }

        val best = scored.firstOrNull { it.second >= 55 }?.first ?: return false
        return openByPackage(best.packageName)
    }

    private fun score(target: String, label: String): Int {
        if (target == label) return 100
        if (label.startsWith(target) || target.startsWith(label)) return 90
        if (label.contains(target) || target.contains(label)) return 80

        val compactTarget = target.replace(" ", "")
        val compactLabel = label.replace(" ", "")
        if (compactTarget == compactLabel) return 98
        if (compactLabel.contains(compactTarget) || compactTarget.contains(compactLabel)) return 85

        val targetWords = target.split(' ').filter { it.length > 1 }.toSet()
        val labelWords = label.split(' ').filter { it.length > 1 }.toSet()
        val common = targetWords.intersect(labelWords).size
        if (common > 0) return 60 + (common * 5)

        return 0
    }

    private fun cleanTarget(raw: String): String {
        var value = normalize(raw)
            .replace(Regex("[.!?,;:]+$"), "")
            .trim()

        val prefixes = listOf(
            "l application ", "lapp ", "application ", "appli ",
            "ouvre moi ", "ouvre ", "lance moi ", "lance ",
            "demarre ", "demarre moi ", "va sur ",
            "le ", "la ", "les "
        )
        prefixes.firstOrNull { value.startsWith(it) }?.let { value = value.removePrefix(it).trim() }

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
