package com.basilea.proxy.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EventsApiService {
    @POST("api/checkin/validate-code")
    suspend fun validateAccountCode(@Body request: AccountCodeRequest): AccountCodeResponse

    @GET("api/tickets/my")
    suspend fun getMyTickets(): List<TicketSummary>

    @GET("api/tickets/{id}/qr")
    suspend fun getTicketQR(@Path("id") ticketId: String): TicketQRResponse
}

data class AccountCodeRequest(
    val code: String,
    val internalKey: String
)