package com.basilea.proxy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_tickets")
data class OfflineTicketEntity(
    @PrimaryKey
    val ticketId: String,
    val secureEventId: String,
    val holderName: String,
    val ticketType: String,
    val status: String,
    val checkedInAt: Long?,
    val syncedAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)