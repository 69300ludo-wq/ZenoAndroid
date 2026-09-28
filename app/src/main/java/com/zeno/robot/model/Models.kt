package com.zeno.robot.model

data class ChatMessage(val fromZeno: Boolean, val text: String)

data class InstalledApp(val label: String, val packageName: String)

enum class ZenoTheme(val label: String, val accent: Long, val alias: String) {
    CYAN("Classique Cyan", 0xFF00E5FF, "LauncherCyan"),
    VIOLET("Galaxie Violet", 0xFFB55CFF, "LauncherViolet"),
    GOLD("Premium Or", 0xFFFFC942, "LauncherGold"),
    GREEN("Nature Vert", 0xFF3BE49A, "LauncherGreen")
}
