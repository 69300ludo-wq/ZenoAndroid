package com.zeno.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Thème Zeno (Couleurs sombres et cyan) ---
private val ZenoDarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF), // Cyan vif
    onPrimary = Color.Black,
    background = Color(0xFF0B0F19), // Fond très sombre
    surface = Color(0xFF1E293B), // Cartes sombres
    secondary = Color(0xFFBB86FC) // Violet
)

@Composable
fun ZenoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ZenoDarkColors, content = content)
}

// --- Données simples pour le chat ---
data class Message(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ZenoMainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenoMainScreen() {
    var userMessage by remember { mutableStateOf("") }
    // Conversation simulée de base
    val conversation = remember { mutableStateListOf(
        Message("Bonjour ! Je suis Zeno, ton assistant IA. Comment puis-je t'aider ?", false)
    )}

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // --- En-tête : Zeno Visuel ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(text = "ZENO AI", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            // Avatar du robot (emoji pour l'instant)
            Box(modifier = Modifier.size(90.dp).background(MaterialTheme.colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = "🤖", fontSize = 50.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Statut : En ligne", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }

        // --- Zone de Chat (Scrollable) ---
        LazyColumn(modifier = Modifier.weight(1f), reverseLayout = true) {
            items(conversation.reversed()) { message ->
                ChatBubble(message)
            }
        }

        // --- Barre de saisie ---
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Parle à Zeno...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        conversation.add(Message(userMessage, true))
                        // TODO: Ici vous connecterez votre API IA
                        conversation.add(Message("J'ai bien reçu : '$userMessage'. La connexion à l'IA sera ajoutée bientôt !", false))
                        userMessage = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Envoyer")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    Row(horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Surface(
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
        }
    }
}
