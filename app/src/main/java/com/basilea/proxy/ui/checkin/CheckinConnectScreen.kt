package com.basilea.proxy.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basilea.proxy.core.auth.CheckinSessionManager
import com.basilea.proxy.core.network.AccountCodeRequest
import com.basilea.proxy.core.network.ApiFactory
import com.basilea.proxy.data.model.CheckinSessionEntity
import kotlinx.coroutines.launch

private val CyberCyan = Color(0xFF06B6D4)
private val NeonPurple = Color(0xFFA855F7)
private val GlassBackground = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)

data class CheckinConnectUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val eventName: String = ""
)

class CheckinConnectViewModel : ViewModel() {
    private val eventsApi = ApiFactory.createEventsApi()
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var isConnected by mutableStateOf(false)
    var eventName by mutableStateOf("")
    
    fun connect(context: android.content.Context, accountCode: String) {
        if (accountCode.isBlank()) {
            error = "Digite o código do evento"
            return
        }
        
        isLoading = true
        error = null
        
        viewModelScope.launch {
            try {
                val request = AccountCodeRequest(
                    code = accountCode,
                    internalKey = com.basilea.proxy.BuildConfig.EVENTS_INTERNAL_KEY
                )
                val response = eventsApi.validateAccountCode(request)
                
                val sessionManager = CheckinSessionManager(context)
                val session = CheckinSessionEntity(
                    accountCode = accountCode,
                    eventId = response.eventId,
                    secureEventId = response.secureEventId,
                    eventName = response.eventName,
                    operatorName = response.operatorName,
                    expiresAt = System.currentTimeMillis() + CheckinSessionManager.SESSION_DURATION_MS
                )
                sessionManager.saveSession(session)
                
                isConnected = true
                eventName = response.eventName
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("400") == true -> "Código inválido. Verifique e tente novamente."
                    e.message?.contains("410") == true -> "Código expirado. Solicite um novo ao organizador."
                    else -> e.message ?: "Erro ao conectar"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearError() {
        error = null
    }
}

@Composable
fun CheckinConnectScreen(
    viewModel: CheckinConnectViewModel,
    onConnected: () -> Unit
) {
    var accountCode by remember { mutableStateOf("") }
    
    LaunchedEffect(viewModel.isConnected) {
        if (viewModel.isConnected) {
            onConnected()
        }
    }
    
    val localContext = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CHECK-IN",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 2.sp
        )
        Text(
            "CONECTAR OPERADOR",
            color = CyberCyan,
            fontSize = 12.sp,
            letterSpacing = 3.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GlassBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "CÓDIGO DO EVENTO",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = accountCode,
                    onValueChange = {
                        accountCode = it.uppercase()
                        viewModel.clearError()
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(CyberCyan, NeonPurple)))
                )
                
                if (viewModel.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        viewModel.error!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                viewModel.connect(localContext, accountCode)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.8f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "CONECTAR",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Ex: EVT-2026-XK72TZ",
            color = Color.Gray.copy(alpha = 0.5f),
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}