package com.basilea.proxy.core

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class TunnelStream(
    private val streamId: String,
    private val host: String,
    private val port: Int,
    private val onDataReceived: (String, ByteArray) -> Unit,
    private val onStreamClosed: (String, String) -> Unit
) {
    private val socket = Socket()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val writeChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val closed = AtomicBoolean(false)
    private val lastActivityAt = AtomicLong(System.currentTimeMillis())

    companion object {
        private const val TAG = "TunnelStream"
        private const val IDLE_TIMEOUT_MS = 60_000L
    }

    suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "[$streamId] Connecting to $host:$port")
            socket.connect(InetSocketAddress(host, port), 10000)
            startLoops()
            true
        } catch (e: Exception) {
            Log.e(TAG, "[$streamId] Failed to connect: ${e.message}")
            false
        }
    }

    private fun startLoops() {
        scope.launch { readLoop() }
        scope.launch { writeLoop() }
        scope.launch { monitorLoop() }
    }

    fun enqueueData(data: ByteArray) {
        if (!closed.get()) {
            markActivity()
            writeChannel.trySend(data)
        }
    }

    private suspend fun readLoop() {
        val buffer = ByteArray(16384)
        try {
            val input = socket.getInputStream()
            while (isActive && !closed.get()) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                
                markActivity()
                val data = buffer.copyOfRange(0, bytesRead)
                onDataReceived(streamId, data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$streamId] Read error: ${e.message}")
        } finally {
            closeInternal("read_error_or_eof")
        }
    }

    private suspend fun writeLoop() {
        try {
            val output = socket.getOutputStream()
            for (data in writeChannel) {
                output.write(data)
                output.flush()
                markActivity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$streamId] Write error: ${e.message}")
        } finally {
            closeInternal("write_error")
        }
    }

    private suspend fun monitorLoop() {
        while (isActive && !closed.get()) {
            val idleTime = System.currentTimeMillis() - lastActivityAt.get()
            if (idleTime > IDLE_TIMEOUT_MS) {
                Log.w(TAG, "[$streamId] Idle for ${idleTime}ms, closing")
                closeInternal("idle_timeout")
                return
            }
            delay(15000)
        }
    }

    fun closeFromServer(reason: String) {
        closeInternal(reason, notifyServer = false)
    }

    private fun closeInternal(reason: String, notifyServer: Boolean = true) {
        if (closed.compareAndSet(false, true)) {
            Log.d(TAG, "[$streamId] Closing: $reason")
            writeChannel.close()
            try { socket.close() } catch (ignored: Exception) {}
            job.cancel()
            
            if (notifyServer) {
                onStreamClosed(streamId, reason)
            }
        }
    }

    private fun markActivity() {
        lastActivityAt.set(System.currentTimeMillis())
    }
}
