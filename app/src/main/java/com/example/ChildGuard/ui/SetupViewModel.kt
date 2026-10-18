package com.example.childguard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childguard.data.preferences.SettingsRepository
import com.example.childguard.data.remote.TelegramApiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val botToken: String = "",
    val chatId: String = "",
    val isChecking: Boolean = false,
    val checkResult: String? = null,
    val isSaved: Boolean = false
)

class SetupViewModel(
    private val settings: SettingsRepository,
    private val telegram: TelegramApiHelper
) : ViewModel() {

    private val _ui = MutableStateFlow(SetupUiState())
    val ui = _ui.asStateFlow()

    fun onTokenChange(v: String) { _ui.value = _ui.value.copy(botToken = v.trim()) }
    fun onChatIdChange(v: String) { _ui.value = _ui.value.copy(chatId = v.trim()) }

    fun checkConnection() {
        val token = _ui.value.botToken
        val chat = _ui.value.chatId.toLongOrNull()
        _ui.value = _ui.value.copy(isChecking = true, checkResult = null)

        viewModelScope.launch {
            val result = when {
                token.isBlank() -> "Введите токен бота"
                chat == null -> "Chat ID — число (узнайте через @userinfobot)"
                else -> {
                    val ok = telegram.testConnection(token, chat)
                    if (ok) {
                        // Сохраняем только после успешной проверки.
                        settings.saveBotToken(token)
                        settings.saveParentChatId(chat)
                        settings.markSetupComplete()
                        _ui.value = _ui.value.copy(isSaved = true)
                        "Подключено ✔ Проверьте Telegram."
                    } else "Ошибка. Проверьте токен, Chat ID и интернет."
                }
            }
            _ui.value = _ui.value.copy(isChecking = false, checkResult = result)
        }
    }
}
