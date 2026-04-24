package com.basilea.proxy.core.crypto

import com.basilea.proxy.BuildConfig
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

data class QRPayload(
    val t: String,
    val e: String,
    val w: Long,
    val h: String
)

object QRCryptoManager {
    private const val WINDOW_SIZE_MS = 30_000L
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun computeHmac(ticketId: String, eventId: String, window: Long): String {
        val key = SecretKeySpec(
            BuildConfig.QR_HMAC_SECRET.toByteArray(StandardCharsets.UTF_8),
            HMAC_ALGORITHM
        )
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        val data = "$ticketId:$eventId:$window"
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8)).toHexString()
    }

    fun generatePayload(ticketId: String, eventId: String): QRPayload {
        val window = currentWindow()
        val hmac = computeHmac(ticketId, eventId, window)
        return QRPayload(
            t = ticketId,
            e = eventId,
            w = window,
            h = hmac
        )
    }

    fun generatePayloadJson(ticketId: String, eventId: String): String {
        val payload = generatePayload(ticketId, eventId)
        val json = """{"t":"${payload.t}","e":"${payload.e}","w":${payload.w},"h":"${payload.h}"}"""
        return Base64.getEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }

    fun isValid(payload: QRPayload): Boolean {
        val now = currentWindow()
        val prev = now - 1
        val validHmacs = listOf(
            computeHmac(payload.t, payload.e, now),
            computeHmac(payload.t, payload.e, prev)
        )
        return payload.h in validHmacs
    }

    fun parsePayload(base64Encoded: String): QRPayload? {
        return try {
            val json = String(Base64.getDecoder().decode(base64Encoded), StandardCharsets.UTF_8)
            parseJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJson(json: String): QRPayload? {
        return try {
            val tRegex = """"t"\s*:\s*"([^"]+)"""".toRegex()
            val eRegex = """"e"\s*:\s*"([^"]+)"""".toRegex()
            val wRegex = """"w"\s*:\s*(\d+)""".toRegex()
            val hRegex = """"h"\s*:\s*"([^"]+)"""".toRegex()

            val tMatch = tRegex.find(json) ?: return null
            val eMatch = eRegex.find(json) ?: return null
            val wMatch = wRegex.find(json) ?: return null
            val hMatch = hRegex.find(json) ?: return null

            val t = tMatch.groupValues.getOrNull(1) ?: return null
            val e = eMatch.groupValues.getOrNull(1) ?: return null
            val w = wMatch.groupValues.getOrNull(1)?.toLongOrNull() ?: return null
            val h = hMatch.groupValues.getOrNull(1) ?: return null

            QRPayload(t, e, w, h)
        } catch (e: Exception) {
            null
        }
    }

    fun currentWindow(): Long {
        return floor(System.currentTimeMillis().toDouble() / WINDOW_SIZE_MS).toLong()
    }

    fun isWindowExpired(window: Long): Boolean {
        return currentWindow() - window > 1
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}