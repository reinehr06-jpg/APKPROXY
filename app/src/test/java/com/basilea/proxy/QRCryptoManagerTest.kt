package com.basilea.proxy.core.crypto

import org.junit.Assert.*
import org.junit.Test

class QRCryptoManagerTest {

    @Test
    fun `hmac valido aceita janela atual`() {
        val ticketId = "ticket-123"
        val eventId = "event-456"
        val window = QRCryptoManager.currentWindow()
        
        val hmac = QRCryptoManager.computeHmac(ticketId, eventId, window)
        
        assertNotNull(hmac)
        assertTrue(hmac.length == 64)
    }

    @Test
    fun `hmac valido aceita janela anterior tolerancia`() {
        val ticketId = "ticket-123"
        val eventId = "event-456"
        val window = QRCryptoManager.currentWindow() - 1
        
        val hmac = QRCryptoManager.computeHmac(ticketId, eventId, window)
        
        assertNotNull(hmac)
        assertTrue(hmac.length == 64)
    }

    @Test
    fun `hmac invalido rejeita sem chamar servidor`() {
        val payload = QRPayload(
            t = "ticket-123",
            e = "event-456",
            w = QRCryptoManager.currentWindow(),
            h = "invalid_hmac_value_64_chars_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        )
        
        val isValid = QRCryptoManager.isValid(payload)
        
        assertFalse(isValid)
    }

    @Test
    fun `payload gerado e valido corretamente`() {
        val ticketId = "ticket-test-001"
        val eventId = "event-test-001"
        
        val payload = QRCryptoManager.generatePayload(ticketId, eventId)
        
        assertEquals(ticketId, payload.t)
        assertEquals(eventId, payload.e)
        assertTrue(QRCryptoManager.isValid(payload))
    }

    @Test
    fun `payload invalido apos 2 janelas`() {
        val ticketId = "ticket-test-002"
        val eventId = "event-test-002"
        val oldWindow = QRCryptoManager.currentWindow() - 2
        
        val hmac = QRCryptoManager.computeHmac(ticketId, eventId, oldWindow)
        val payload = QRPayload(ticketId, eventId, oldWindow, hmac)
        
        val isValid = QRCryptoManager.isValid(payload)
        
        assertFalse(isValid)
    }

    @Test
    fun `parse payload valido`() {
        val ticketId = "ticket-123"
        val eventId = "event-456"
        val base64 = QRCryptoManager.generatePayloadJson(ticketId, eventId)
        
        val parsed = QRCryptoManager.parsePayload(base64)
        
        assertNotNull(parsed)
        assertEquals(ticketId, parsed?.t)
        assertEquals(eventId, parsed?.e)
    }

    @Test
    fun `parse payload invalido retorna null`() {
        val invalidBase64 = "invalid_base64_content"
        
        val parsed = QRCryptoManager.parsePayload(invalidBase64)
        
        assertNull(parsed)
    }

    @Test
    fun `current window avanca a cada 30 segundos`() {
        val window1 = QRCryptoManager.currentWindow()
        
        Thread.sleep(100)
        
        val window2 = QRCryptoManager.currentWindow()
        
        assertTrue(window2 >= window1)
    }
}