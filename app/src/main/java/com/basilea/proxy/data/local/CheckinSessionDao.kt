package com.basilea.proxy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.basilea.proxy.data.model.CheckinSessionEntity

@Dao
interface CheckinSessionDao {
    @Query("SELECT * FROM checkin_sessions WHERE accountCode = :accountCode")
    suspend fun getSession(accountCode: String): CheckinSessionEntity?

    @Query("SELECT * FROM checkin_sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSession(): CheckinSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CheckinSessionEntity)

    @Query("DELETE FROM checkin_sessions WHERE accountCode = :accountCode")
    suspend fun deleteSession(accountCode: String)

    @Query("DELETE FROM checkin_sessions")
    suspend fun deleteAllSessions()
}