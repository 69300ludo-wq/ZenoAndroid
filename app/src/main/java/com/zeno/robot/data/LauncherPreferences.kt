package com.zeno.robot.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HomeSettings(
    val showClock: Boolean = true,
    val showRobot: Boolean = true,
    val showSearch: Boolean = true,
    val showApps: Boolean = true,
    val showDock: Boolean = true,
    val robotSize: Int = 430,
    val appCount: Int = 8,
    val homePackages: List<String> = emptyList()
)

data class AppFolder(
    val id: String,
    val name: String,
    val packages: List<String>
)

class LauncherPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("zeno_launcher", Context.MODE_PRIVATE)

    fun loadHomeSettings(): HomeSettings = HomeSettings(
        showClock = prefs.getBoolean("show_clock", true),
        showRobot = prefs.getBoolean("show_robot", true),
        showSearch = prefs.getBoolean("show_search", true),
        showApps = prefs.getBoolean("show_apps", true),
        showDock = prefs.getBoolean("show_dock", true),
        robotSize = prefs.getInt("robot_size", 430).coerceIn(300, 520),
        appCount = prefs.getInt("app_count", 8).coerceIn(4, 12),
        homePackages = decodeStringList(prefs.getString("home_packages", "[]"))
    )

    fun saveHomeSettings(settings: HomeSettings) {
        prefs.edit()
            .putBoolean("show_clock", settings.showClock)
            .putBoolean("show_robot", settings.showRobot)
            .putBoolean("show_search", settings.showSearch)
            .putBoolean("show_apps", settings.showApps)
            .putBoolean("show_dock", settings.showDock)
            .putInt("robot_size", settings.robotSize.coerceIn(300, 520))
            .putInt("app_count", settings.appCount.coerceIn(4, 12))
            .putString("home_packages", encodeStringList(settings.homePackages))
            .apply()
    }

    fun loadFolders(): List<AppFolder> {
        val raw = prefs.getString("folders", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val packages = mutableListOf<String>()
                    val packageArray = item.optJSONArray("packages") ?: JSONArray()
                    for (j in 0 until packageArray.length()) packages += packageArray.optString(j)
                    if (id.isNotBlank() && name.isNotBlank()) add(AppFolder(id, name, packages.filter { it.isNotBlank() }))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveFolders(folders: List<AppFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            val item = JSONObject()
            item.put("id", folder.id)
            item.put("name", folder.name)
            item.put("packages", JSONArray(folder.packages))
            array.put(item)
        }
        prefs.edit().putString("folders", array.toString()).apply()
    }

    private fun encodeStringList(values: List<String>): String = JSONArray(values).toString()

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}
