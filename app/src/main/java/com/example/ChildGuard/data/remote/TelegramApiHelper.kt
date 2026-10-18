package com.example.childguard.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramApiHelper {

    sealed interface Result {
        data class Success(val messageId: Long) : Result
        /** retryable=false — 4xx (плохой токен/чат), повторять бессмысленно. */
        data class Error(val description: String, val retryable: Boolean) : Result
    }

    private companion object {
        const val TAG = "TelegramApi"
        const val BASE_URL = "https://api.telegram.org/bot"
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun payload(vararg pairs: Pair<String, Any?>): String =
            JSONObject().apply {
                pairs.forEach { (k, v) ->
                    when (v) {
                        null -> Unit
                        is Long -> put(k, v)
                        is Double -> put(k, v)
                        is Boolean -> put(k, v)
                        else -> put(k, v.toString())
                    }
                }
            }.toString()
    }

    suspend fun sendTextMessage(botToken: String, chatId: Long, text: String): Result =
        execute(botToken, "sendMessage", payload(
            "chat_id" to chatId,
            "text" to text.take(4000), // лимит Telegram 4096
            "disable_web_page_preview" to true
        ))

    suspend fun sendLocationMessage(botToken: String, chatId: Long, lat: Double, lon: Double): Result =
        execute(botToken, "sendLocation", payload(
            "chat_id" to chatId,
            "latitude" to lat,
            "longitude" to lon
        ))

    /** Тест соединения из экрана настройки. */
    suspend fun testConnection(botToken: String, chatId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL$botToken/sendMessage")
                    .post(payload(
                        "chat_id" to chatId,
                        "text" to "✅ ChildGuard: подключение установлено."
                    ).toRequestBody(JSON_MEDIA))
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: IOException) {
                Log.w(TAG, "Connection test failed", e)
                false
            }
        }

    private suspend fun execute(token: String, method: String, payload: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL$token/$method")
                    .post(payload.toRequestBody(JSON_MEDIA))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    val json = body?.let { runCatching { JSONObject(it) }.getOrNull() }
                    when {
                        json?.optBoolean("ok") == true ->
                            Result.Success(json.optJSONObject("result")?.optLong("message_id") ?: -1)
                        json != null ->
                            Result.Error(json.optString("description", "HTTP ${response.code}"),
                                retryable = response.code !in 400..499)
                        else -> Result.Error("HTTP ${response.code}", retryable = true)
                    }
                }
            } catch (e: IOException) {
                Result.Error(e.message ?: "network", retryable = true)
            }
        }
}
