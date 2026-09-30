package com.zeno.robot.data

import android.content.Context
import com.zeno.robot.model.ZenoTheme

/**
 * Gère uniquement le thème visuel de l'application Zeno.
 *
 * Les anciennes versions modifiaient aussi des alias d'icône/launcher Android.
 * Ces composants ont été supprimés quand Zeno est redevenu une application
 * classique. Essayer de les activer provoquait un crash au changement de couleur.
 */
class IconManager(private val context: Context) {
    fun apply(theme: ZenoTheme) {
        context.getSharedPreferences("zeno", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", theme.name)
            .apply()
    }

    fun current(): ZenoTheme {
        val value = context.getSharedPreferences("zeno", Context.MODE_PRIVATE)
            .getString("theme", ZenoTheme.CYAN.name)
        return runCatching {
            ZenoTheme.valueOf(value ?: ZenoTheme.CYAN.name)
        }.getOrDefault(ZenoTheme.CYAN)
    }
}
