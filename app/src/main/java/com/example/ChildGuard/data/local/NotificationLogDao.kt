package com.example.childguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.childguard.data.local.entity.NotificationEventEntity

@Dao
interface NotificationLogDao {

    @Insert
    suspend fun insert(event: NotificationEventEntity)

    /** Политика хранения: не более 7 дней истории. */
    @Query("DELETE FROM notification_log WHERE createdAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
