package com.basilea.proxy.ui.checkin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basilea.proxy.core.crypto.QRCryptoManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private val CyberCyan = Color(0xFF06B6D4)
private val DeepMidnight = Color(0xFF020617)
private val SuccessGreen = Color(0xFF10B981)
private val WarningYellow = Color(0xFFFBBF24)
private val ErrorRed = Color(0xFFEF4444)
private val AlertPurple = Color(0xFFA855F7)

enum class ValidationResult {
    NONE, VALID, USED, INVALID, ALERT
}

class QRScannerViewModel : ViewModel() {
    var hasPermission by mutableStateOf(false)
    var isValidating by mutableStateOf(false)
    var validationResult by mutableStateOf(ValidationResult.NONE)
    var resultMessage by mutableStateOf("")
    var holderName by mutableStateOf("")
    var isOnline by mutableStateOf(true)
    
    fun updatePermissionStatus(status: Boolean) {
        hasPermission = status
    }
    
    fun onBarcodeDetected(barcode: String) {
        if (isValidating || validationResult != ValidationResult.NONE) return
        
        viewModelScope.launch {
            isValidating = true
            
            try {
                val payload = QRCryptoManager.parsePayload(barcode)
                if (payload == null) {
                    showResult(ValidationResult.INVALID, "QR inválido", "")
                    return@launch
                }
                
                if (!QRCryptoManager.isValid(payload)) {
                    showResult(ValidationResult.INVALID, "QR expirado ou inválido", "")
                    return@launch
                }
                
                showResult(ValidationResult.VALID, "Entrada liberada", "João Silva")
                
            } catch (e: Exception) {
                isOnline = false
            } finally {
                isValidating = false
            }
        }
    }
    
    private suspend fun showResult(result: ValidationResult, message: String, holder: String) {
        validationResult = result
        resultMessage = message
        holderName = holder
        
        delay(4000)
        validationResult = ValidationResult.NONE
        resultMessage = ""
        holderName = ""
    }
}

@Composable
fun QRScannerScreen(
    viewModel: QRScannerViewModel,
    eventName: String,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
    }
    
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.updatePermissionStatus(true)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.hasPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        val scanner = BarcodeScanning.getClient()
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    barcode.rawValue?.let { value ->
                                                        if (value.startsWith("{")) {
                                                            viewModel.onBarcodeDetected(value)
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepMidnight.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(eventName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (viewModel.isOnline) "Online" else "Offline",
                        color = if (viewModel.isOnline) SuccessGreen else WarningYellow,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onNavigateToDashboard,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                ) {
                    Text("Histórico", color = CyberCyan, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(250.dp)
                    .border(3.dp, CyberCyan, RoundedCornerShape(16.dp))
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "Aponte a câmera para o QR Code do ingresso",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 14.sp
            )
        }
        
        if (viewModel.validationResult != ValidationResult.NONE) {
            ValidationResultCard(
                result = viewModel.validationResult,
                message = viewModel.resultMessage,
                holderName = viewModel.holderName,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ValidationResultCard(
    result: ValidationResult,
    message: String,
    holderName: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, borderColor) = when (result) {
        ValidationResult.VALID -> SuccessGreen.copy(alpha = 0.2f) to SuccessGreen
        ValidationResult.USED -> WarningYellow.copy(alpha = 0.2f) to WarningYellow
        ValidationResult.INVALID -> ErrorRed.copy(alpha = 0.2f) to ErrorRed
        ValidationResult.ALERT -> AlertPurple.copy(alpha = 0.2f) to AlertPurple
        ValidationResult.NONE -> Color.Transparent to Color.Transparent
    }
    
    val icon = when (result) {
        ValidationResult.VALID -> "✓"
        ValidationResult.USED -> "⚠"
        ValidationResult.INVALID -> "✗"
        ValidationResult.ALERT -> "!"
        ValidationResult.NONE -> ""
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 48.sp, color = borderColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = borderColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (holderName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(holderName, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}