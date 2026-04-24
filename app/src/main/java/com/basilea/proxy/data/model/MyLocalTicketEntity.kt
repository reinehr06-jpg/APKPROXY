package com.basilea.proxy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_tickets")
data class MyLocalTicketEntity(
    @PrimaryKey
    val ticketId: String,
    val eventId: String,
    val eventName: String,
    val eventDate: String,
    val eventLocation: String,
    val holderName: String,
    val ticketType: String,
    val status: String,
    val savedAt: Long = System.currentTimeMillis()
)