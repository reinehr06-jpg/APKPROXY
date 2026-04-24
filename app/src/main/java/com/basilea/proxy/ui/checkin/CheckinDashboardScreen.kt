package com.basilea.proxy.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basilea.proxy.core.auth.CheckinSessionManager
import kotlinx.coroutines.launch

private val CyberCyan = Color(0xFF06B6D4)
private val DeepMidnight = Color(0xFF020617)
private val GlassBackground = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)
private val SuccessGreen = Color(0xFF10B981)

data class CheckinEntry(
    val id: String,
    val holderName: String,
    val ticketType: String,
    val checkedInAt: Long
)

class CheckinDashboardViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var totalValidated by mutableStateOf(0)
    var entries by mutableStateOf<List<CheckinEntry>>(emptyList())
    var isOnline by mutableStateOf(true)
    
    fun loadHistory() {
        viewModelScope.launch {
            isLoading = true
            
            val mockEntries = listOf(
                CheckinEntry("1", "Maria Santos", "Pista", System.currentTimeMillis() - 3600000),
                CheckinEntry("2", "Pedro Oliveira", "Camarote", System.currentTimeMillis() - 1800000),
                CheckinEntry("3", "Ana Costa", "VIP", System.currentTimeMillis() - 600000)
            )
            
            totalValidated = mockEntries.size
            entries = mockEntries
            isLoading = false
        }
    }
    
    fun endSession(context: android.content.Context) {
        val sessionManager = CheckinSessionManager(context)
        sessionManager.clearSession()
    }
}

@Composable
fun CheckinDashboardScreen(
    viewModel: CheckinDashboardViewModel,
    context: android.content.Context,
    eventName: String,
    onSessionEnded: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HISTÓRICO CHECK-IN",
                color = CyberCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (viewModel.isOnline) "Online" else "Offline",
                color = if (viewModel.isOnline) SuccessGreen else Color(0xFFFBBF24),
                fontSize = 11.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TOTAL VALIDADO", color = CyberCyan, fontSize = 11.sp)
                    Text(
                        viewModel.totalValidated.toString(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(eventName, color = Color.White, fontSize = 14.sp)
                    Text("Hoje", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("ENTRADAS", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.entries) { entry ->
                EntryCard(entry = entry)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                viewModel.endSession(context)
                onSessionEnded()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
        ) {
            Text("ENCERRAR SESSÃO", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntryCard(entry: CheckinEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SuccessGreen, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(entry.holderName, color = Color.White, fontSize = 14.sp)
                    Text(entry.ticketType, color = Color.Gray, fontSize = 11.sp)
                }
            }
            Text(formatTime(entry.checkedInAt), color = Color.Gray, fontSize = 11.sp)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return if (minutes < 60) "${minutes}min atrás" else "${minutes / 60}h atrás"
}