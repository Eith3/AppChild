package com.example.childguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_log")
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isThreat: Boolean,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
