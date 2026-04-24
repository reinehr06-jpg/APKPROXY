package com.basilea.proxy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.basilea.proxy.data.local.CheckinSessionDao
import com.basilea.proxy.data.local.MyTicketDao
import com.basilea.proxy.data.local.OfflineTicketDao
import com.basilea.proxy.data.model.CheckinSessionEntity
import com.basilea.proxy.data.model.MyLocalTicketEntity
import com.basilea.proxy.data.model.OfflineTicketEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        CheckinSessionEntity::class,
        OfflineTicketEntity::class,
        MyLocalTicketEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkinSessionDao(): CheckinSessionDao
    abstract fun offlineTicketDao(): OfflineTicketDao
    abstract fun myTicketDao(): MyTicketDao

    companion object {
        private const val DATABASE_NAME = "basileia.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}