package com.basilea.proxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basilea.proxy.core.AuthManager
import com.basilea.proxy.service.ProxyService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        setContent {
            BasileaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A0533)) { // Midnight Purple
                    val churchName = authManager.getChurchName()
                    if (churchName != null) {
                        DashboardScreen(churchName)
                    } else {
                        LoginScreen()
                    }
                }
            }
        }
    }

    @Composable
    fun LoginScreen() {
        var whatsapp by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing Logo
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                // Glow effect
                Box(modifier = Modifier.size(100.dp).background(Color(0xFF7C3AED).copy(alpha = 0.3f), RoundedCornerShape(20.dp)))
                
                // Main Symbol
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4C1D95)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Box(fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("B", fontSize = 60.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Text("Basiléia Proxy", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-1).sp)
            Text("Privacidade e Velocidade para sua Igreja", color = Color(0xFFD8B4FE), fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("SEU WHATSAPP", fontSize = 12.sp, color = Color(0xFFD8B4FE), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = Color(0xFF7C3AED), thickness = 2.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Acesso restrito a clientes cadastrados.", fontSize = 10.sp, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val res = authManager.authenticate(whatsapp)
                        isLoading = false
                        if (res.success) {
                            startProxyService()
                            recreate()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("ACESSAR SISTEMA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Precisa de ajuda? Fale conosco", color = Color(0xFF7C3AED), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    fun DashboardScreen(churchName: String) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Basilea Proxy", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(40.dp))
            
            // Status Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp).background(Color(0xFF4C1D95).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("100%", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) // Success Green
                    Text("Ativo", color = Color(0xFF10B981))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Igreja Autenticada:", color = Color.Gray, fontSize = 12.sp)
                    Text(churchName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = { 
                authManager.logout()
                stopProxyService()
                recreate()
            }) {
                Text("Desconectar", color = Color.Red)
            }
        }
    }

    private fun startProxyService() {
        startService(Intent(this, ProxyService::class.java))
    }

    private fun stopProxyService() {
        stopService(Intent(this, ProxyService::class.java))
    }
}

@Composable
fun BasileaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFFA855F7),
            background = Color(0xFF1A0533)
        ),
        content = content
    )
}
