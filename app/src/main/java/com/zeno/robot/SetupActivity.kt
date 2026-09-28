package com.zeno.robot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zeno.robot.service.FloatingZenoService

class SetupActivity : ComponentActivity() {

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        finishSetup()
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            continueWithOverlayPermission()
        } else {
            Toast.makeText(
                this,
                "Le microphone est nécessaire pour parler à Zeno.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            continueWithOverlayPermission()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun continueWithOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            finishSetup()
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayLauncher.launch(intent)
    }

    private fun finishSetup() {
        if (Settings.canDrawOverlays(this) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, FloatingZenoService::class.java)
            )
            Toast.makeText(this, "Zeno est maintenant actif sur ton écran.", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
