package com.example.childguard.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.childguard.di.AppLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val analyzer get() = AppLocator.threatAnalyzer
    private val alertDispatcher get() = AppLocator.alertDispatcher
    private val logRepository get() = AppLocator.logRepository

    companion object {
        private const val TAG = "NotifListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
            ?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?.toString().orEmpty()
        val fullText = "$title\n$text".trim()
        if (fullText.isBlank()) return

        // Пропускаем свои уведомления — иначе петля:
        // "бот прислал алерт" -> алерт содержит стоп-слово -> новый алерт.
        if (sbn.packageName == packageName) return

        val event = NotificationEvent(
            packageName = sbn.packageName.orEmpty(),
            title = title,
            text = text,
            postTime = sbn.postTime,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable
        )

        serviceScope.launch {
            val verdict = analyzer.analyze(fullText)

            logRepository.insert(event, verdict.isThreat, verdict.category.displayName)

            if (verdict.isThreat) {
                alertDispatcher.dispatchAlert(
                    category = verdict.category,
                    source = event.packageName,
                    preview = buildString {
                        append("⚠️ Подозрительное уведомление\n")
                        append("Категория: ${verdict.category.displayName}\n")
                        append("Приложение: ${event.packageName}\n")
                        if (title.isNotBlank()) append("Заголовок: ${title.take(100)}\n")
                        append("Фрагмент: ${text.take(120)}")
                    }
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Removed: ${sbn.packageName}")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Listener disconnected — проверьте доступ в настройках")
        super.onListenerDisconnected()
    }
}
