package com.basilea.proxy.ui.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.basilea.proxy.core.network.ApiFactory
import com.basilea.proxy.core.network.TicketSummary
import kotlinx.coroutines.launch

private val CyberCyan = Color(0xFF06B6D4)
private val NeonPurple = Color(0xFFA855F7)
private val DeepMidnight = Color(0xFF020617)
private val GlassBackground = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)
private val SuccessGreen = Color(0xFF10B981)
private val WarningYellow = Color(0xFFFBBF24)
private val ErrorRed = Color(0xFFEF4444)

data class TicketUiModel(
    val id: String,
    val eventName: String,
    val eventDate: String,
    val status: String,
    val ticketType: String?,
    val holderName: String?,
    val isOfflineSaved: Boolean = false
)

class MyTicketsViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var tickets by mutableStateOf<List<TicketUiModel>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var isOffline by mutableStateOf(false)
    
    private val eventsApi = ApiFactory.createEventsApi()
    private val savedOfflineIds = mutableSetOf<String>()
    
    fun loadTickets(context: android.content.Context) {
        isLoading = true
        error = null
        
        viewModelScope.launch {
            try {
                val apiTickets = eventsApi.getMyTickets()
                
                val uiModels = apiTickets.map { apiTicket ->
                    TicketUiModel(
                        id = apiTicket.id,
                        eventName = apiTicket.eventName,
                        eventDate = apiTicket.eventDate,
                        status = apiTicket.status,
                        ticketType = apiTicket.ticketType,
                        holderName = apiTicket.holderName,
                        isOfflineSaved = apiTicket.id in savedOfflineIds
                    )
                }
                
                tickets = uiModels
            } catch (e: Exception) {
                isOffline = true
                error = "Exibindo ingressos salvos offline"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun saveOffline(context: android.content.Context, ticketId: String) {
        savedOfflineIds.add(ticketId)
        
        tickets = tickets.map {
            if (it.id == ticketId) it.copy(isOfflineSaved = true) else it
        }
    }
    
    fun getOfflineStatus(ticketId: String): Boolean {
        return tickets.find { it.id == ticketId }?.isOfflineSaved == true
    }
}

@Composable
fun MyTicketsScreen(
    viewModel: MyTicketsViewModel,
    onTicketClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadTickets(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MEUS INGRESSOS",
                color = CyberCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (viewModel.isOffline) {
                Text(
                    "Offline",
                    color = WarningYellow,
                    fontSize = 11.sp
                )
            }
        }
        
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.tickets) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        onClick = { onTicketClick(ticket.id) },
                        onSaveOffline = {
                            viewModel.saveOffline(context, ticket.id)
                        }
                    )
                }
            }
        }
        
        if (viewModel.error != null && viewModel.tickets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhum ingresso encontrado",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: TicketUiModel,
    onClick: () -> Unit,
    onSaveOffline: () -> Unit
) {
    val statusColor = when (ticket.status) {
        "active" -> SuccessGreen
        "used" -> Color.Gray
        "transferred" -> WarningYellow
        "blocked" -> ErrorRed
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    ticket.eventName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        ticket.status.uppercase(),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                ticket.eventDate,
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            if (ticket.ticketType != null) {
                Text(
                    ticket.ticketType,
                    color = CyberCyan,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (ticket.isOfflineSaved) {
                    Text(
                        "✅ Disponível offline",
                        color = SuccessGreen,
                        fontSize = 11.sp
                    )
                } else {
                    TextButton(
                        onClick = onSaveOffline,
                        modifier = Modifier
                    ) {
                        Text(
                            "Salvar offline",
                            color = NeonPurple,
                            fontSize = 12.sp
                        )
                    }
                }
                
                if (ticket.status == "active") {
                    Text(
                        "Ver QR Code →",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}