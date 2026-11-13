package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.model.AppNotification

interface NotificationRepository {
    suspend fun listNotifications(): List<AppNotification>
    suspend fun markRead(id: String)
}
