package com.trustbank.loanapp.ui.screens.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.ui.common.documentTypeLabel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.common.statusMeta
import com.trustbank.loanapp.ui.common.toTone
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.components.StatusChip
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun ApplicationDetailScreen(
    applicationId: String,
    onBack: () -> Unit,
    viewModel: ApplicationDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val response = rememberFieldState {
        if (it.trim().length < 10) "Describe your response (at least 10 characters)" else null
    }

    LaunchedEffect(applicationId) { viewModel.load(applicationId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(title = "Application Details", onBack = onBack)

        val application = uiState.application
        if (uiState.isLoading || application == null) {
            LoadingIndicator(modifier = Modifier.height(240.dp))
            return@Column
        }

        val product = MockData.productById(application.productId)
        val meta = statusMeta(application.status)

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(product?.name ?: application.productId, style = MaterialTheme.typography.titleLarge, color = AppColors.Neutral900)
                Text(application.id, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
            }
            StatusChip(label = meta.label, tone = meta.tone)
        }

        SectionCard(title = "Loan Details") {
            DetailRow("Amount requested", Formatters.ugx(application.amountRequested))
            if (application.amountApproved != null) {
                DetailRow("Amount approved", Formatters.ugx(application.amountApproved))
            }
            DetailRow("Term", "${application.termMonths} months")
            DetailRow("Purpose", application.purpose)
            DetailRow("Submitted", Formatters.dateTime(application.submittedAt))
            DetailRow("Last updated", Formatters.dateTime(application.updatedAt))
        }

        if (uiState.documents.isNotEmpty()) {
            SectionCard(title = "Documents") {
                uiState.documents.forEach { document ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(documentTypeLabel(document.docType), style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral800)
                        val docTone = document.verificationStatus.toTone()
                        StatusChip(label = document.verificationStatus.name, tone = docTone)
                    }
                }
            }
        }

        if (application.decisionRemarks != null) {
            SectionCard(title = "Decision Notes") {
                Text(application.decisionRemarks, style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral700)
            }
        }

        if (application.status == LoanStatus.AWAITING_ADDITIONAL_INFO) {
            SectionCard(title = "Action Needed") {
                if (application.infoRequestMessage != null) {
                    Text(application.infoRequestMessage, style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral700)
                }
                if (uiState.responseSent) {
                    Text("Your response has been sent.", style = MaterialTheme.typography.bodySmall, color = AppColors.Success600)
                } else {
                    AppTextField(
                        label = "Your response",
                        value = response.value,
                        onValueChange = response::onValueChange,
                        onFocusLost = response::onFocusLost,
                        required = true,
                        error = response.error,
                        placeholder = "Describe what you're providing or ask a question",
                        singleLine = false,
                        minLines = 3,
                    )
                    PrimaryButton(
                        text = "Send response",
                        isLoading = uiState.isResponding,
                        onClick = {
                            if (response.validateForSubmit()) viewModel.respondToInfoRequest(applicationId)
                        },
                    )
                }
            }
        }
    }
}
