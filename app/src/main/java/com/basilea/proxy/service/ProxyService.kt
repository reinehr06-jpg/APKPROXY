package com.basilea.proxy.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.basilea.proxy.core.AuthManager
import com.basilea.proxy.core.SilverTunnelClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ProxyService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var client: SilverTunnelClient? = null
    
    companion object {
        const val CHANNEL_ID = "BasileaProxyChannel"
        const val NOTIFICATION_ID = 1337
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification("Iniciando Proxy...")
        startForeground(NOTIFICATION_ID, notification)

        val authManager = AuthManager(this)
        val relayUrl = authManager.getRelayUrl()
        val token = authManager.getSessionToken()

        if (relayUrl != null && token != null) {
            client = SilverTunnelClient(
                relayUrl = relayUrl,
                token = token,
                onStatusChanged = { isConnected ->
                    updateNotification(if (isConnected) "Proxy Ativo e Seguro" else "Conectando...")
                },
                onLog = { msg -> println("ProxyLog: $msg") }
            )
            client?.start()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Basilea Proxy")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Proxy Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        client?.stop()
        job.cancel()
        super.onDestroy()
    }
}
