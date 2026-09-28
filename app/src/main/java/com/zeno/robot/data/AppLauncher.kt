package com.zeno.robot.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.zeno.robot.model.InstalledApp

class AppLauncher(private val context: Context) {
    fun listLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                InstalledApp(
                    label = it.loadLabel(context.packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun openByPackage(packageName: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return true
    }

    fun openByName(name: String): Boolean {
        val normalized = name.trim().lowercase()
        val app = listLaunchableApps().firstOrNull {
            it.label.lowercase() == normalized || it.label.lowercase().contains(normalized)
        } ?: return false
        return openByPackage(app.packageName)
    }
}
