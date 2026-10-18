package com.example.childguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.childguard.data.local.entity.NotificationEventEntity
import com.example.childguard.data.local.entity.PendingMessageEntity

@Database(
    entities = [PendingMessageEntity::class, NotificationEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingMessageDao(): PendingMessageDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "childguard.db"
                ).build().also { INSTANCE = it }
            }
    }
}
