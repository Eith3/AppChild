package com.example.childguard.data.repository

import android.util.Log
import com.example.childguard.data.local.AppDatabase
import com.example.childguard.data.local.entity.PendingMessageEntity
import com.example.childguard.data.preferences.SettingsRepository
import com.example.childguard.data.remote.TelegramApiHelper
import com.example.childguard.domain.ThreatAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Единая точка отправки алертов.
 * Паттерн: сначала пишем в Room-очередь, потом пытаемся отправить всё накопленное.
 * Офлайн не теряет события — при появлении сети следующий вызов/воркер всё дольёт.
 */
class AlertDispatcher(
    private val database: AppDatabase,
    private val telegram: TelegramApiHelper,
    private val settings: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao get() = database.pendingMessageDao()

    /** Антифлуд: не чаще 1 алерта на категорию в 5 минут. */
    private val lastAlertAt = mutableMapOf<ThreatAnalyzer.ThreatCategory, Long>()
    private companion object {
        const val TAG = "AlertDispatcher"
        const val ALERT_COOLDOWN_MS = 5L * 60 * 1000
    }

    fun dispatchAlert(category: ThreatAnalyzer.ThreatCategory, source: String, preview: String) {
        scope.launch {
            val chatId = settings.parentChatId ?: return@launch
            val now = System.currentTimeMillis()
            if (now - (lastAlertAt[category] ?: 0) < ALERT_COOLDOWN_MS) {
                Log.d(TAG, "Alert for $category suppressed by cooldown")
                return@launch
            }
            lastAlertAt[category] = now

            dao.insert(
                PendingMessageEntity(
                    type = "text",
                    chatId = chatId,
                    text = preview
                )
            )
            flushQueue()
        }
    }

    fun dispatchLocation(chatId: Long, lat: Double, lon: Double, mapsLink: String) {
        scope.launch {
            dao.insert(
                PendingMessageEntity(
                    type = "location",
                    chatId = chatId,
                    lat = lat,
                    lon = lon
                )
            )
            dao.insert(
                PendingMessageEntity(
                    type = "text",
                    chatId = chatId,
                    text = "📍 Геолокация: $mapsLink"
                )
            )
            flushQueue()
        }
    }

    /** Отправляем очередь, пока не упрёмся в ошибку. */
    suspend fun flushQueue() {
        val token = settings.botToken
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No bot token, keeping ${dao.all().size} messages queued")
            return
        }
        for (msg in dao.all()) {
            val result = when (msg.type) {
                "location" -> telegram.sendLocationMessage(token, msg.chatId, msg.lat!!, msg.lon!!)
                else -> telegram.sendTextMessage(token, msg.chatId, msg.text.orEmpty())
            }
            when (result) {
                is TelegramApiHelper.Result.Success -> dao.delete(msg)
                is TelegramApiHelper.Result.Error -> {
                    dao.incrementAttempts(msg.id)
                    if (!result.retryable) {
                        Log.e(TAG, "Non-retryable: ${result.description}, dropping message")
                        dao.delete(msg)
                    }
                    dao.dropPoisonPills()
                    break // сеть упала — остальное дошлём позже
                }
            }
        }
    }
}
