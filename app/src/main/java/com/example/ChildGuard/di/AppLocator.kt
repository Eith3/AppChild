package com.example.childguard.di

import android.content.Context
import com.example.childguard.data.local.AppDatabase
import com.example.childguard.data.preferences.SettingsRepository
import com.example.childguard.data.remote.TelegramApiHelper
import com.example.childguard.data.repository.AlertDispatcher
import com.example.childguard.data.repository.NotificationLogRepository
import com.example.childguard.domain.ThreatAnalyzer

/**
 * Простой service locator — для концепта достаточно.
 * Для production заменить на Hilt.
 */
object AppLocator {

    lateinit var settings: SettingsRepository
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var telegram: TelegramApiHelper
        private set
    lateinit var alertDispatcher: AlertDispatcher
        private set
    lateinit var logRepository: NotificationLogRepository
        private set
    lateinit var threatAnalyzer: ThreatAnalyzer
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext
        settings = SettingsRepository(appContext)
        database = AppDatabase.get(appContext)
        telegram = TelegramApiHelper()
        logRepository = NotificationLogRepository(database.notificationLogDao())
        threatAnalyzer = ThreatAnalyzer()
        alertDispatcher = AlertDispatcher(
            database = database,
            telegram = telegram,
            settings = settings
        )
    }
}
