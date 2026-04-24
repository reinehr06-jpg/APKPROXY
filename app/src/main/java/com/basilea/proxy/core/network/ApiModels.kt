package com.basilea.proxy.core.network

data class AccountCodeResponse(
    val eventId: String,
    val secureEventId: String,
    val eventName: String,
    val eventDate: String,
    val operatorName: String
)

data class TicketSummary(
    val id: String,
    val eventName: String,
    val eventDate: String,
    val status: String,
    val ticketType: String? = null,
    val holderName: String? = null
)

data class TicketQRResponse(
    val qrPayload: String,
    val holderName: String,
    val ticketType: String
)

data class CheckinValidationRequest(
    val qrPayload: String,
    val operatorId: String
)

data class CheckinValidationResponse(
    val success: Boolean,
    val message: String,
    val ticketId: String? = null,
    val holderName: String? = null,
    val ticketType: String? = null
)