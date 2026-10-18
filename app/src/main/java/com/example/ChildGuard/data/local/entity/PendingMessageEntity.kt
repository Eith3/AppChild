package com.example.childguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Очередь исходящих сообщений — телефон ребёнка может быть офлайн. */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,              // "text" | "location"
    val chatId: Long,
    val text: String? = null,      // для type = "text"
    val lat: Double? = null,       // для type = "location"
    val lon: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)
