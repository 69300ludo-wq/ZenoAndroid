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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        continueWithOverlayPermission()
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            continueWithCameraPermission()
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
            continueWithCameraPermission()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun continueWithCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            continueWithOverlayPermission()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
        runCatching { overlayLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(this, "Impossible d'ouvrir l'autorisation d'affichage.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun finishSetup() {
        val ready = Settings.canDrawOverlays(this) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (!ready) {
            finish()
            return
        }

        // Certains téléphones refusent un service micro selon leur gestion de batterie.
        // Ce refus ne doit jamais faire planter l'application principale.
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, FloatingZenoService::class.java)
            )
        }.onSuccess {
            Toast.makeText(this, "Zeno vocal est activé.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(
                this,
                "Zeno reste utilisable. Le mode vocal permanent n'a pas pu démarrer sur ce téléphone.",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }
}
