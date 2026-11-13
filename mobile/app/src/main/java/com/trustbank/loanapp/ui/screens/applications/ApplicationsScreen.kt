package com.trustbank.loanapp.ui.screens.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.ui.components.ApplicationRow
import com.trustbank.loanapp.ui.components.EmptyState
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.theme.AppColors

@Composable
fun ApplicationsScreen(
    onApplicationClick: (String) -> Unit,
    viewModel: ApplicationsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("My Applications", style = MaterialTheme.typography.headlineSmall, color = AppColors.Neutral900)

        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.height(200.dp))
            uiState.applications.isEmpty() -> EmptyState(
                title = "No applications yet",
                description = "Applications you submit will appear here.",
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.applications, key = { it.id }) { application ->
                    ApplicationRow(application = application, onClick = { onApplicationClick(application.id) })
                }
            }
        }
    }
}
