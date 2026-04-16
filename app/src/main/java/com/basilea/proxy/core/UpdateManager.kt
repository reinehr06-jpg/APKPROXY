package com.basilea.proxy.core

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {

    suspend fun checkForUpdates(currentVersionCode: Int): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            // FAKE API RESPONSE (Simulando uma chamada de rede real)
            delay(1500) // 1.5s delay for realistic "checking for updates" feel
            
            // Simula que o servidor respondeu com a versão 2, e nós estamos na versão 1.
            return@withContext UpdateInfo(
                hasUpdate = true,
                latestVersionCode = 2,
                downloadUrl = "https://speed.hetzner.de/100MB.bin", // Link de teste grande e válido genérico só para ver o DownloadManager rodar. Substituir pelo seu .apk depois.
                releaseNotes = ">>> SECURITY_PATCH_V2\n- Otimizações no Proxy de Rede.\n- Nova Interface Glassmorphism.\n- Criptografia atualizada."
            )
        } catch (e: Exception) {
            return@withContext UpdateInfo(false, currentVersionCode, "", "")
        }
    }

    fun downloadAndInstallUpdate(apkUrl: String) {
        val fileName = "basilea_update.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("SYS.BASILEA_UPDATE")
            .setDescription("Baixando pacote criptografado...")
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return

        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
