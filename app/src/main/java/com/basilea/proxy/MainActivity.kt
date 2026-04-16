package com.basilea.proxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basilea.proxy.core.AuthManager
import com.basilea.proxy.core.UpdateManager
import com.basilea.proxy.core.UpdateInfo
import com.basilea.proxy.service.ProxyService
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// Theme colors
private val CyberCyan = Color(0xFF06B6D4)
private val NeonPurple = Color(0xFFA855F7)
private val DeepMidnight = Color(0xFF020617)
private val GlassBackground = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)

class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        updateManager = UpdateManager(this)

        setContent {
            BasileaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DeepMidnight) {
                    TechBackground()

                    var showUpdateDialog by remember { mutableStateOf(false) }
                    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                    
                    LaunchedEffect(Unit) {
                        try {
                            val info = updateManager.checkForUpdates(1) // Fixado em 1 para este teste visual
                            if (info.hasUpdate) {
                                updateInfo = info
                                showUpdateDialog = true
                            }
                        } catch(e:Exception){}
                    }

                    if (showUpdateDialog && updateInfo != null) {
                        UpdateDialog(updateInfo!!) {
                            showUpdateDialog = false
                            updateManager.downloadAndInstallUpdate(updateInfo!!.downloadUrl)
                        }
                    }

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
    fun TechBackground() {
        val infiniteTransition = rememberInfiniteTransition(label = "tech_bg")
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bg_offset"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Deep background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepMidnight, Color(0xFF0F0728))
                )
            )

            // Tech Grid
            val gridSize = 100f
            val gridColor = CyberCyan.copy(alpha = 0.05f)

            var x = offset % gridSize
            while (x < canvasWidth) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
                x += gridSize
            }

            var y = offset % gridSize
            while (y < canvasHeight) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }
        }
    }

    @Composable
    fun UpdateDialog(updateInfo: UpdateInfo, onUpdateClick: () -> Unit) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMidnight.copy(alpha=0.95f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha=0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(">>> SYSTEM_UPDATE_FOUND", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Uma nova versão de segurança obrigatória está disponível.", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(updateInfo.releaseNotes, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onUpdateClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("INICIAR DOWNLOAD", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.dp)
                    }
                }
            }
        }
    }

    @Composable
    fun AnimatedBasileaLogo(modifier: Modifier = Modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
        
        val rotation1 by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring1"
        )
        
        val rotation2 by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring2"
        )

        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(contentAlignment = Alignment.Center, modifier = modifier.size(160.dp)) {
            // Glowing Backdrop
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
            )
            
            // Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                
                // Inner Ring (Fast, Clockwise)
                rotate(rotation1, center) {
                    drawArc(
                        color = CyberCyan,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(20.dp.toPx(), 20.dp.toPx()),
                        size = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Outer Ring (Slow, Counter-Clockwise)
                rotate(rotation2, center) {
                    drawArc(
                        color = NeonPurple,
                        startAngle = 90f,
                        sweepAngle = 200f,
                        useCenter = false,
                        topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                        size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Central "B" Card
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0764).copy(alpha = 0.9f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "B", 
                        fontSize = 42.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White,
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = NeonPurple,
                                offset = Offset(0f, 0f),
                                blurRadius = 25f
                            )
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun LoginScreen() {
        var whatsapp by remember { mutableStateOf("") }
        var document by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedBasileaLogo()

            Spacer(modifier = Modifier.height(56.dp))
            
            Text("SYS.BASILEA_PROXY", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
            Text("SECURE CONNECTION PROTOCOL", color = CyberCyan, fontSize = 11.sp, letterSpacing = 2.sp)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Glassmorphism Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "TARGET_WHATSAPP", 
                        fontSize = 11.sp, 
                        color = CyberCyan, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = Brush.verticalGradient(listOf(CyberCyan, NeonPurple)),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Neon Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Brush.horizontalGradient(listOf(CyberCyan, NeonPurple)))
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "DOCUMENT (CPF/CNPJ)", 
                        fontSize = 11.sp, 
                        color = CyberCyan, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = document,
                        onValueChange = { document = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = Brush.verticalGradient(listOf(CyberCyan, NeonPurple)),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Neon Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Brush.horizontalGradient(listOf(CyberCyan, NeonPurple)))
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acesso restrito. Tráfego criptografado E2E.", fontSize = 11.sp, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tech Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val res = authManager.authenticate(whatsapp, document)
                        isLoading = false
                        if (res.success) {
                            startProxyService()
                            recreate()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha=0.8f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("INICIAR CONEXÃO_SEC", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.5.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(">>> STATUS: AWAITING_INPUT", color = Color.Gray.copy(alpha=0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }

    @Composable
    fun ReactorCore() {
        val infiniteTransition = rememberInfiniteTransition(label = "reactor")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "reactor_ring"
        )
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            // Glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            )
            
            // Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                
                // Track
                drawCircle(
                    color = Color.White.copy(alpha=0.03f),
                    radius = size.width / 2 - 20f,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Spinning segment
                rotate(rotation, center) {
                    drawArc(
                        color = CyberCyan,
                        startAngle = 0f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            // Core Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "VPN_ACTIVE", 
                    fontSize = 12.sp, 
                    color = CyberCyan, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "100%", 
                    fontSize = 52.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = CyberCyan,
                            offset = Offset(0f,0f),
                            blurRadius = 40f
                        )
                    )
                )
            }
        }
    }

    @Composable
    fun DashboardScreen(churchName: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BASILEA_NODE", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.height(56.dp))
            
            ReactorCore()
            
            Spacer(modifier = Modifier.height(56.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("NODE_IDENTITY:", color = CyberCyan, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(churchName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("ROUTING_STATUS:", color = CyberCyan, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("OPTIMIZED / SECURE", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.dp)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
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
            primary = NeonPurple,
            secondary = CyberCyan,
            background = DeepMidnight,
            surface = DeepMidnight
        ),
        content = content
    )
}

@Preview(showSystemUi = true)
@Composable
fun PreviewLoginScreen() {
    BasileaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = DeepMidnight) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Instanciando classe mockada só para visualização do design
                val mockActivity = MainActivity()
                mockActivity.TechBackground()
                mockActivity.LoginScreen()
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewDashboardScreen() {
    BasileaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = DeepMidnight) {
            Box(modifier = Modifier.fillMaxSize()) {
                val mockActivity = MainActivity()
                mockActivity.TechBackground()
                mockActivity.DashboardScreen(churchName = "Igreja Central (Mock)")
            }
        }
    }
}
