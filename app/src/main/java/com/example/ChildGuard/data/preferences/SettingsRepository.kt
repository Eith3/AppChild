package com.example.childguard.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Токен бота — секрет. Храним в EncryptedSharedPreferences (AES-256),
 * вводится пользователем при онбординге, в код не зашит.
 */
class SettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var botToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var parentChatId: Long?
        get() = if (prefs.contains(KEY_CHAT_ID)) prefs.getLong(KEY_CHAT_ID, 0) else null
        private set(value) = prefs.edit().putLong(KEY_CHAT_ID, value ?: 0).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        private set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    fun saveBotToken(token: String) { botToken = token }
    fun saveParentChatId(chatId: Long) { parentChatId = chatId }
    fun markSetupComplete() { isSetupComplete = true }

    private companion object {
        const val KEY_TOKEN = "bot_token"
        const val KEY_CHAT_ID = "parent_chat_id"
        const val KEY_SETUP_DONE = "setup_done"
    }
}
