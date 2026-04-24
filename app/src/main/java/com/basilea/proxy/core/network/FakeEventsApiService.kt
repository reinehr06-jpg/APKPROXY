package com.basilea.proxy.core.network

import com.basilea.proxy.core.crypto.QRCryptoManager
import com.basilea.proxy.BuildConfig
import kotlinx.coroutines.delay

class FakeEventsApiService : EventsApiService {
    
    override suspend fun validateAccountCode(request: AccountCodeRequest): AccountCodeResponse {
        delay(500)
        return AccountCodeResponse(
            eventId = "evt-fake-001",
            secureEventId = "sec-fake-001",
            eventName = "Evento de Teste Basileia",
            eventDate = "25/04/2026 às 20:00",
            operatorName = "Operador Teste"
        )
    }

    override suspend fun getMyTickets(): List<TicketSummary> {
        delay(300)
        return listOf(
            TicketSummary(
                id = "tkt-001",
                eventName = "Show ABC",
                eventDate = "25/04/2026",
                status = "active",
                ticketType = "Pista",
                holderName = "João Silva"
            ),
            TicketSummary(
                id = "tkt-002",
                eventName = "Show XYZ",
                eventDate = "10/05/2026",
                status = "used",
                ticketType = "Camarote",
                holderName = "João Silva"
            ),
            TicketSummary(
                id = "tkt-003",
                eventName = "Show Nacional",
                eventDate = "15/06/2026",
                status = "active",
                ticketType = "VIP",
                holderName = "João Silva"
            )
        )
    }

    override suspend fun getTicketQR(ticketId: String): TicketQRResponse {
        delay(200)
        val qrPayload = com.basilea.proxy.core.crypto.QRCryptoManager.generatePayloadJson(
            ticketId = ticketId,
            eventId = "sec-fake-001"
        )
        return TicketQRResponse(
            qrPayload = qrPayload,
            holderName = "João Silva",
            ticketType = "Pista"
        )
    }
}

object ApiFactory {
    fun createEventsApi(useMock: Boolean = com.basilea.proxy.BuildConfig.USE_MOCK_API): EventsApiService {
        return if (useMock) FakeEventsApiService() else createRealEventsApi()
    }

    private fun createRealEventsApi(): EventsApiService {
        val baseUrl = com.basilea.proxy.BuildConfig.EVENTS_API_URL
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okhttp3.OkHttpClient.Builder().build())
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        return retrofit.create(EventsApiService::class.java)
    }
}