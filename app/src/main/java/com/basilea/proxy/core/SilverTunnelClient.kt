package com.basilea.proxy.core

import android.util.Log
import okio.ByteString
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.Base64

class SilverTunnelClient(
    private val relayUrl: String,
    private val token: String,
    private val onStatusChanged: (Boolean) -> Unit,
    private val onLog: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val tunnelStreams = ConcurrentHashMap<String, TunnelStream>()
    
    companion object {
        private const val TAG = "SilverTunnelClient"
    }

    fun start() {
        val request = Request.Builder()
            .url(relayUrl)
            .addHeader("X-Proxy-Token", token)
            .build()
        
        webSocket = client.newWebSocket(request, SocketListener())
        onLog("Conectando ao relay: $relayUrl")
    }

    fun stop() {
        webSocket?.close(1000, "App closed")
        tunnelStreams.values.forEach { it.closeFromServer("client_stopped") }
        tunnelStreams.clear()
        onStatusChanged(false)
    }

    private inner class SocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onStatusChanged(true)
            onLog("Conectado com sucesso!")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            onStatusChanged(false)
            onLog("Conexão fechada: $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onStatusChanged(false)
            onLog("Falha na conexão: ${t.message}")
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.getString("type")
            val payload = json.getJSONObject("payload")

            when (type) {
                "tunnel_open" -> {
                    val streamId = payload.getString("id")
                    val host = payload.getString("host")
                    val port = payload.getInt("port")
                    
                    val stream = TunnelStream(
                        streamId = streamId,
                        host = host,
                        port = port,
                        onDataReceived = { id, data -> sendTunnelData(id, data) },
                        onStreamClosed = { id, reason -> sendTunnelClosed(id, reason) }
                    )
                    
                    kotlinx.coroutines.GlobalScope.launch {
                        if (stream.open()) {
                            tunnelStreams[streamId] = stream
                        } else {
                            sendTunnelClosed(streamId, "connect_failed")
                        }
                    }
                }
                "tunnel_data" -> {
                    val streamId = payload.getString("id")
                    val dataBase64 = payload.getString("data")
                    val data = Base64.getDecoder().decode(dataBase64)
                    tunnelStreams[streamId]?.enqueueData(data)
                }
                "tunnel_close" -> {
                    val streamId = payload.getString("id")
                    tunnelStreams.remove(streamId)?.closeFromServer("server_requested")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    private fun sendTunnelData(streamId: String, data: ByteArray) {
        val message = JSONObject().apply {
            put("type", "tunnel_data")
            put("payload", JSONObject().apply {
                put("id", streamId)
                put("data", Base64.getEncoder().encodeToString(data))
            })
        }
        webSocket?.send(message.toString())
    }

    private fun sendTunnelClosed(streamId: String, reason: String) {
        val message = JSONObject().apply {
            put("type", "tunnel_closed")
            put("payload", JSONObject().apply {
                put("id", streamId)
                put("reason", reason)
            })
        }
        webSocket?.send(message.toString())
    }
}
