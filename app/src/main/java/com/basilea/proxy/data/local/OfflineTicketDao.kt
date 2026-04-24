package com.basilea.proxy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.basilea.proxy.data.model.OfflineTicketEntity

@Dao
interface OfflineTicketDao {
    @Query("SELECT * FROM offline_tickets WHERE secureEventId = :secureEventId")
    suspend fun getTicketsByEvent(secureEventId: String): List<OfflineTicketEntity>

    @Query("SELECT * FROM offline_tickets WHERE ticketId = :ticketId")
    suspend fun getTicket(ticketId: String): OfflineTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<OfflineTicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: OfflineTicketEntity)

    @Update
    suspend fun updateTicket(ticket: OfflineTicketEntity)

    @Query("DELETE FROM offline_tickets WHERE secureEventId = :secureEventId")
    suspend fun deleteTicketsByEvent(secureEventId: String)

    @Query("SELECT * FROM offline_tickets WHERE syncedAt > :since ORDER BY syncedAt ASC")
    suspend fun getUnsyncedTickets(since: Long): List<OfflineTicketEntity>
}