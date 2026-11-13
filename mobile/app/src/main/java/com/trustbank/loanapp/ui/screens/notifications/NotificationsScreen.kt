package com.trustbank.loanapp.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.model.AppNotification
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.EmptyState
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    SetStatusBarAppearance(darkIcons = true)

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackRow(title = "Notifications", onBack = onBack)

        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
            uiState.notifications.isEmpty() -> EmptyState(
                icon = Icons.Filled.NotificationsNone,
                title = "You're all caught up",
                description = "New updates about your applications will show up here.",
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationRow(notification = notification, onClick = { viewModel.markRead(notification.id) })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.Neutral200, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        if (!notification.read) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AppColors.Primary600, CircleShape),
            )
            Box(modifier = Modifier.padding(end = 10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.message, style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral800)
            Text(Formatters.dateTime(notification.sentAt), style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
        }
    }
}
