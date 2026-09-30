package com.quman.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InAppNotificationManager {
    private val _currentNotification = MutableStateFlow<ParsedSmsNotification?>(null)
    val currentNotification: StateFlow<ParsedSmsNotification?> = _currentNotification.asStateFlow()

    fun show(notification: ParsedSmsNotification) {
        _currentNotification.value = notification
    }

    fun dismiss() {
        _currentNotification.value = null
    }
}
