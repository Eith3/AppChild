package com.example.childguard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.childguard.di.AppLocator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SetupScreen()
                }
            }
        }
    }
}

@Composable
fun SetupScreen() {
    // VM создаём вручную (без Hilt) — для концепта допустимо.
    val vm = remember { SetupViewModel(AppLocator.settings, AppLocator.telegram) }
    val state by vm.ui.collectAsState()
    val context = LocalContext.current

    // Runtime-permission для Android 13+ (только для своих уведомлений).
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат не критичен для работы слушателя */ }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройка защиты", style = MaterialTheme.typography.headlineSmall)
        Text(
            "1) @BotFather → создайте бота, скопируйте токен.\n" +
            "2) @userinfobot → узнайте свой Chat ID.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = state.botToken,
            onValueChange = vm::onTokenChange,
            label = { Text("Токен бота") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.chatId,
            onValueChange = vm::onChatIdChange,
            label = { Text("Chat ID родителя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = vm::checkConnection,
            enabled = !state.isChecking,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.isChecking) "Проверка…" else "Проверить подключение") }

        state.checkResult?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        if (state.isSaved) {
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("2. Включить доступ к уведомлениям") }

            Text(
                "3. Разрешите геолокацию, когда система спросит.\n" +
                "Мониторинг и трекинг запустятся автоматически.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
