package com.basilea.proxy.ui.tickets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basilea.proxy.core.crypto.QRCryptoManager
import com.basilea.proxy.core.qr.QRCodeGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val CyberCyan = Color(0xFF06B6D4)
private val NeonPurple = Color(0xFFA855F7)
private val DeepMidnight = Color(0xFF020617)
private val GlassBackground = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)
private val SuccessGreen = Color(0xFF10B981)
private val WarningYellow = Color(0xFFFBBF24)

class TicketQRViewModel : ViewModel() {
    var qrBitmap by mutableStateOf<Bitmap?>(null)
    var countdown by mutableStateOf(30)
    var holderName by mutableStateOf("")
    var ticketType by mutableStateOf("")
    var eventName by mutableStateOf("")
    var eventDate by mutableStateOf("")
    var eventLocation by mutableStateOf("")
    var isOffline by mutableStateOf(false)
    var fraudAlert by mutableStateOf<String?>(null)
    
    fun loadTicketData(ticketId: String, isOfflineMode: Boolean = false) {
        isOffline = isOfflineMode
        
        holderName = "João Silva"
        ticketType = "Pista"
        eventName = "Show ABC"
        eventDate = "25/04/2026 às 20:00"
        eventLocation = "Arena XYZ, São Paulo"
        
        generateQR(ticketId)
        startCountdown(ticketId)
    }
    
    private fun generateQR(ticketId: String) {
        viewModelScope.launch {
            try {
                val qrPayload = QRCryptoManager.generatePayloadJson(
                    ticketId = ticketId,
                    eventId = "sec-fake-001"
                )
                qrBitmap = QRCodeGenerator.generateQRCode(qrPayload, 260, 260)
            } catch (e: Exception) {
                qrBitmap = null
            }
        }
    }
    
    private fun startCountdown(ticketId: String) {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                countdown--
                if (countdown <= 0) {
                    countdown = 30
                    generateQR(ticketId)
                }
            }
        }
    }
    
    fun onFraudAttemptDetected() {
        fraudAlert = "Tentativa de fraude detectada! QR bloqueado."
        
        viewModelScope.launch {
            delay(5000)
            fraudAlert = null
        }
    }
    
    fun clearFraudAlert() {
        fraudAlert = null
    }
}

@Composable
fun TicketQRScreen(
    viewModel: TicketQRViewModel,
    ticketId: String,
    onBack: () -> Unit
) {
    LaunchedEffect(ticketId) {
        viewModel.loadTicketData(ticketId)
    }
    
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepMidnight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text("← Voltar", color = CyberCyan)
                }
                
                if (viewModel.isOffline) {
                    Text(
                        "📴 Offline",
                        color = WarningYellow,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                viewModel.eventName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                viewModel.eventDate,
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Text(
                viewModel.eventLocation,
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, CyberCyan, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.qrBitmap != null) {
                        Image(
                            bitmap = viewModel.qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(260.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "⏱️ Renovando em ${viewModel.countdown}s",
                color = if (viewModel.countdown <= 10) WarningYellow else CyberCyan,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        viewModel.holderName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        viewModel.ticketType,
                        color = CyberCyan,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "📍 Ver no Maps",
                color = CyberCyan,
                fontSize = 12.sp
            )
        }
        
        if (viewModel.fraudAlert != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearFraudAlert() },
                title = { Text("⚠️ Alerta de Segurança", color = ErrorRed) },
                text = { Text(viewModel.fraudAlert!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearFraudAlert() }) {
                        Text("OK", color = CyberCyan)
                    }
                }
            )
        }
    }
}

private val ErrorRed = Color(0xFFEF4444)