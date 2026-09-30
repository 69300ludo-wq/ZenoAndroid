package com.zeno.robot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFF4D5A))) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF26070B), Color(0xFF090B16), Color(0xFF020617)))
                    )
                ) {
                    SosScreen(Color(0xFFFF4D5A))
                }
            }
        }
    }
}

@Composable
fun SosScreen(accent: Color) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zeno_sos", Context.MODE_PRIVATE) }
    var contact by remember { mutableStateOf(prefs.getString("contact", "") ?: "") }
    var status by remember { mutableStateOf("") }

    fun dial(number: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }.onFailure { status = "Impossible d’ouvrir l’application Téléphone." }
    }

    fun prepareSms() {
        val number = contact.trim()
        if (number.isBlank()) {
            status = "Enregistre d’abord le numéro de ton contact SOS."
            return
        }
        val message = "SOS - J’ai besoin d’aide. Merci de me contacter rapidement."
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
                    putExtra("sms_body", message)
                }
            )
        }.onFailure { status = "Impossible d’ouvrir l’application Messages." }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(Icons.Default.Warning, null, tint = accent, modifier = Modifier.size(68.dp))
            Text("MODE SOS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                "Accès rapide aux secours et à ton contact d’urgence",
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            Button(
                onClick = { dial("112") },
                modifier = Modifier.fillMaxWidth().height(62.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Call, null)
                Spacer(Modifier.width(10.dp))
                Text("APPELER LE 112", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("Urgences en France", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dial("15") }, modifier = Modifier.weight(1f)) { Text("15 SAMU") }
                OutlinedButton(onClick = { dial("17") }, modifier = Modifier.weight(1f)) { Text("17 Police") }
                OutlinedButton(onClick = { dial("18") }, modifier = Modifier.weight(1f)) { Text("18 Pompiers") }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xCC111827),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .45f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contact SOS personnel", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it.take(24) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Numéro de téléphone") }
                    )
                    Button(
                        onClick = {
                            val clean = contact.trim()
                            prefs.edit().putString("contact", clean).apply()
                            status = if (clean.isBlank()) "Contact SOS effacé." else "Contact SOS enregistré."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer le contact")
                    }
                    OutlinedButton(onClick = { if (contact.isBlank()) status = "Enregistre d’abord un contact SOS." else dial(contact.trim()) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Call, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Appeler mon contact SOS")
                    }
                    OutlinedButton(onClick = { prepareSms() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Message, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Préparer un SMS SOS")
                    }
                }
            }
        }

        if (status.isNotBlank()) {
            item { Text(status, color = accent, textAlign = TextAlign.Center) }
        }

        item {
            Text(
                "Zeno ouvre le composeur ou le message pour que tu confirmes toi-même l’appel ou l’envoi.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
