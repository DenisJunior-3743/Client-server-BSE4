package com.trustbank.loanapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.common.ACTIVE_STATUSES
import com.trustbank.loanapp.ui.common.IN_PROGRESS_STATUSES
import com.trustbank.loanapp.ui.common.NEEDS_ACTION_STATUSES
import com.trustbank.loanapp.ui.common.StatusTone
import com.trustbank.loanapp.ui.components.ApplicationRow
import com.trustbank.loanapp.ui.components.EmptyState
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.StatTile
import com.trustbank.loanapp.ui.theme.AppColors

@Composable
fun HomeScreen(
    onApplyClick: () -> Unit,
    onApplicationClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by AppContainer.session.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    "Hello, ${user?.fullName?.substringBefore(' ') ?: "there"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.Neutral900,
                )
                Text("Here's where your loans stand today.", style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral500)
            }
            Box {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Filled.NotificationsNone, contentDescription = "Notifications", tint = AppColors.Neutral700)
                }
                if (uiState.unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.TopEnd)
                            .background(AppColors.Danger500, CircleShape),
                    )
                }
            }
        }

        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.height(160.dp))
        } else {
            val inProgress = uiState.applications.count { it.status in IN_PROGRESS_STATUSES }
            val needsAction = uiState.applications.count { it.status in NEEDS_ACTION_STATUSES }
            val active = uiState.applications.count { it.status in ACTIVE_STATUSES }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("In Progress", inProgress.toString(), Icons.Filled.HourglassTop, StatusTone.PRIMARY, modifier = Modifier.weight(1f))
                StatTile("Needs Action", needsAction.toString(), Icons.Filled.PriorityHigh, StatusTone.WARNING, modifier = Modifier.weight(1f))
                StatTile("Active Loans", active.toString(), Icons.Filled.CheckCircle, StatusTone.SUCCESS, modifier = Modifier.weight(1f))
            }

            PrimaryButton(text = "Apply for a new loan", onClick = onApplyClick)

            Text("Recent applications", style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)

            if (uiState.applications.isEmpty()) {
                EmptyState(title = "No applications yet", description = "Apply for your first loan to see it here.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.applications.take(3).forEach { application ->
                        ApplicationRow(application = application, onClick = { onApplicationClick(application.id) })
                    }
                }
            }
        }
    }
}
