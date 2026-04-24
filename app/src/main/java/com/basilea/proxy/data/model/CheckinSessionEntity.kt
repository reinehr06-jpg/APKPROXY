package com.basilea.proxy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkin_sessions")
data class CheckinSessionEntity(
    @PrimaryKey
    val accountCode: String,
    val eventId: String,
    val secureEventId: String,
    val eventName: String,
    val operatorName: String,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)