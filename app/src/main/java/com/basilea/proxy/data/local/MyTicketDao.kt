package com.basilea.proxy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.basilea.proxy.data.model.MyLocalTicketEntity

@Dao
interface MyTicketDao {
    @Query("SELECT * FROM my_tickets ORDER BY savedAt DESC")
    suspend fun getAllTickets(): List<MyLocalTicketEntity>

    @Query("SELECT * FROM my_tickets WHERE ticketId = :ticketId")
    suspend fun getTicket(ticketId: String): MyLocalTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<MyLocalTicketEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: MyLocalTicketEntity)

    @Query("DELETE FROM my_tickets WHERE ticketId = :ticketId")
    suspend fun deleteTicket(ticketId: String)

    @Query("DELETE FROM my_tickets")
    suspend fun deleteAllTickets()
}