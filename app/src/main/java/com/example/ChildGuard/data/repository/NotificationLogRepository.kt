package com.example.childguard.data.repository

import com.example.childguard.data.local.NotificationLogDao
import com.example.childguard.data.local.entity.NotificationEventEntity
import com.example.childguard.service.NotificationEvent

class NotificationLogRepository(private val dao: NotificationLogDao) {

    companion object {
        private const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000 // 7 дней
    }

    suspend fun insert(event: NotificationEvent, isThreat: Boolean, category: String?) {
        dao.insert(
            NotificationEventEntity(
                packageName = event.packageName,
                title = event.title.take(200),
                text = event.text.take(500),
                postTime = event.postTime,
                isThreat = isThreat,
                category = category
            )
        )
        dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
    }
}
