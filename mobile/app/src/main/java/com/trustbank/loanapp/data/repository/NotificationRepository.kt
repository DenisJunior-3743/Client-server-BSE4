package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.AppNotification
import kotlinx.coroutines.delay

interface NotificationRepository {
    suspend fun listNotifications(): List<AppNotification>
    suspend fun markRead(id: String)
}

class FakeNotificationRepository : NotificationRepository {
    override suspend fun listNotifications(): List<AppNotification> {
        delay(250)
        return MockData.notifications.sortedByDescending { it.sentAt }
    }

    override suspend fun markRead(id: String) {
        val index = MockData.notifications.indexOfFirst { it.id == id }
        if (index >= 0) MockData.notifications[index] = MockData.notifications[index].copy(read = true)
    }
}
