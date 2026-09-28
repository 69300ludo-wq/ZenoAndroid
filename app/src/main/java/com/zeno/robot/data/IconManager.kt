package com.zeno.robot.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.zeno.robot.model.ZenoTheme

class IconManager(private val context: Context) {
    fun apply(theme: ZenoTheme) {
        ZenoTheme.entries.forEach { item ->
            val component = ComponentName(context.packageName, "${context.packageName}.${item.alias}")
            val state = if (item == theme) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        context.getSharedPreferences("zeno", Context.MODE_PRIVATE)
            .edit().putString("theme", theme.name).apply()
    }

    fun current(): ZenoTheme {
        val value = context.getSharedPreferences("zeno", Context.MODE_PRIVATE)
            .getString("theme", ZenoTheme.CYAN.name)
        return runCatching { ZenoTheme.valueOf(value ?: ZenoTheme.CYAN.name) }.getOrDefault(ZenoTheme.CYAN)
    }
}
