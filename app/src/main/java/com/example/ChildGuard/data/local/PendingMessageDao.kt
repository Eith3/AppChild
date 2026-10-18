package com.example.childguard.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.childguard.data.local.entity.PendingMessageEntity

@Dao
interface PendingMessageDao {

    @Insert
    suspend fun insert(msg: PendingMessageEntity): Long

    /** FIFO-порядок — шлём в том порядке, в котором события случились. */
    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    suspend fun all(): List<PendingMessageEntity>

    @Delete
    suspend fun delete(msg: PendingMessageEntity)

    @Query("UPDATE pending_messages SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("DELETE FROM pending_messages WHERE attempts >= 10")
    suspend fun dropPoisonPills()
}
