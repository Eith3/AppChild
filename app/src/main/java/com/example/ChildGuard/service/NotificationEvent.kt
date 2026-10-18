package com.example.childguard.service

/** Нейтральная модель — android-классы не уходят в domain-слой. */
data class NotificationEvent(
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isOngoing: Boolean,
    val isClearable: Boolean
)
