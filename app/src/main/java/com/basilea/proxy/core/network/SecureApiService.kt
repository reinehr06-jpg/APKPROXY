package com.basilea.proxy.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SecureApiService {
    @POST("auth/login")
    suspend fun login(@Body request: Map<String, String>): AuthResponse

    @POST("proxy/register")
    suspend fun registerProxy(@Body request: Map<String, String>, @Header("Authorization") token: String): ProxyRegisterResponse

    @POST("proxy/heartbeat")
    suspend fun heartbeat(@Body request: Map<String, String>, @Header("Authorization") token: String): ProxyHeartbeatResponse

    @POST("checkin/validate")
    suspend fun validateTicket(@Body request: CheckinValidationRequest, @Header("X-Totem-Key") totemKey: String): CheckinValidationResponse

    @GET("checkin/offline/{id}")
    suspend fun getOfflineTickets(@Path("id") secureEventId: String, @Header("X-Totem-Key") totemKey: String): List<OfflineTicketResponse>

    @POST("checkin/sync")
    suspend fun syncCheckins(@Body request: List<CheckinSyncRequest>, @Header("X-Totem-Key") totemKey: String): CheckinSyncResponse

    @POST("checkin/fraud-alert")
    suspend fun reportFraudAttempt(@Body request: FraudAlertRequest, @Header("X-Totem-Key") totemKey: String): FraudAlertResponse
}

data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val churchName: String? = null
)

data class ProxyRegisterResponse(
    val success: Boolean,
    val clientId: String? = null,
    val message: String? = null
)

data class ProxyHeartbeatResponse(
    val success: Boolean,
    val message: String? = null
)

data class OfflineTicketResponse(
    val ticketId: String,
    val holderName: String,
    val ticketType: String,
    val status: String,
    val checkedInAt: Long? = null
)

data class CheckinSyncRequest(
    val ticketId: String,
    val checkedInAt: Long
)

data class CheckinSyncResponse(
    val success: Boolean,
    val syncedCount: Int
)

data class FraudAlertRequest(
    val ticketId: String,
    val eventId: String,
    val attemptType: String
)

data class FraudAlertResponse(
    val success: Boolean,
    val message: String
)