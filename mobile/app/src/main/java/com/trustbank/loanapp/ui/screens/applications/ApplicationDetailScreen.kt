package com.trustbank.loanapp.ui.screens.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.ui.common.documentTypeLabel
import com.trustbank.loanapp.ui.common.paymentTermsLabel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.common.statusMeta
import com.trustbank.loanapp.ui.common.toTone
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DigitsField
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.components.StatusChip
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun ApplicationDetailScreen(
    applicationId: String,
    onBack: () -> Unit,
    viewModel: ApplicationDetailViewModel = viewModel(),
) {
    SetStatusBarAppearance(darkIcons = true)

    val uiState by viewModel.uiState.collectAsState()
    val amountsVisible by AppContainer.visibility.amountsVisible.collectAsState()
    val response = rememberFieldState {
        if (it.trim().length < 10) "Describe your response (at least 10 characters)" else null
    }

    LaunchedEffect(applicationId) { viewModel.load(applicationId) }

    // Clock for the offer countdown, refreshed every second while this screen is open.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }
    var paymentAmount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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

        val product = MockData.productById(application.productId) ?: AppContainer.productCache[application.productId]
        val meta = statusMeta(application.status)

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(product?.name ?: "Loan", style = MaterialTheme.typography.titleLarge, color = AppColors.Neutral900)
                Text(application.reference ?: application.id, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
            }
            StatusChip(label = meta.label, tone = meta.tone)
        }

        SectionCard(title = "Loan Details") {
            DetailRow("Amount requested", Formatters.maskedUgx(application.amountRequested, amountsVisible))
            if (application.amountApproved != null) {
                DetailRow("Amount approved", Formatters.maskedUgx(application.amountApproved, amountsVisible))
            }
            DetailRow("Term", "${application.termMonths} months")
            if (application.paymentTerms != null) {
                DetailRow("Terms of payment", paymentTermsLabel(application.paymentTerms))
            }
            DetailRow("Purpose", application.purpose)
            DetailRow("Submitted", Formatters.dateTime(application.submittedAt))
            DetailRow("Last updated", Formatters.dateTime(application.updatedAt))
        }

        val actionMessage = uiState.actionMessage
        if (actionMessage != null) {
            Text(actionMessage, style = MaterialTheme.typography.bodySmall, color = AppColors.Success600)
        }
        val actionError = uiState.actionError
        if (actionError != null) {
            Text(actionError, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger600)
        }

        // Approved offer with a FIRM deadline. The countdown and the disabled button mirror the
        // rule for the borrower; the server is what enforces it (410 after the expiry).
        if (application.status == LoanStatus.APPROVED) {
            SectionCard(title = "Loan Offer") {
                val offerExpiresAt = application.offerExpiresAt
                val secondsLeft = if (offerExpiresAt == null) Long.MAX_VALUE else Duration.between(now, offerExpiresAt).seconds
                val offerExpired = secondsLeft <= 0L
                Text(
                    "You are approved for ${Formatters.maskedUgx(application.amountApproved ?: application.amountRequested, amountsVisible)}. " +
                        "Accept the offer to have the funds released.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Neutral700,
                )
                if (offerExpiresAt != null) {
                    Text(
                        if (offerExpired) {
                            "This offer has expired."
                        } else {
                            "Time left to accept: ${remainingLabel(secondsLeft)} (until ${Formatters.dateTime(offerExpiresAt)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (offerExpired) AppColors.Danger600 else AppColors.Warning600,
                    )
                }
                PrimaryButton(
                    text = "Accept offer",
                    enabled = !offerExpired,
                    isLoading = uiState.isActing,
                    onClick = { viewModel.acceptOffer(applicationId) },
                )
            }
        }

        if (uiState.schedule.isNotEmpty()) {
            SectionCard(title = "Repayment Schedule") {
                uiState.schedule.forEach { installment ->
                    DetailRow(
                        "${installment.installmentNo}. ${Formatters.date(installment.dueDate)}",
                        "${Formatters.maskedUgx(installment.amountDue, amountsVisible)} · ${installment.status}",
                    )
                }
                val outstanding = uiState.schedule.sumOf { it.amountDue - it.amountPaid }
                DetailRow("Outstanding balance", Formatters.maskedUgx(outstanding, amountsVisible))

                if (application.status == LoanStatus.ACTIVE || application.status == LoanStatus.DELINQUENT) {
                    DigitsField(
                        label = "Amount to pay (UGX)",
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        maxLength = 10,
                        placeholder = "e.g. 460783",
                    )
                    val amount = paymentAmount.toDoubleOrNull()
                    PrimaryButton(
                        text = "Pay with mobile money",
                        enabled = amount != null && amount > 0.0 && amount <= outstanding,
                        isLoading = uiState.isActing,
                        onClick = { if (amount != null) viewModel.repay(applicationId, amount) },
                    )
                }
            }
        }

        if (application.businessName != null) {
            SectionCard(title = "Business & Collateral") {
                DetailRow("Applicant", application.applicantFullName ?: "—")
                DetailRow("NIN", application.applicantNin ?: "—")
                DetailRow("Physical location", application.physicalLocation ?: "—")
                DetailRow("Business name", application.businessName)
                DetailRow("Business location", application.businessLocation ?: "—")
                DetailRow("Collateral security", application.collateralSecurity ?: "—")
            }
        }

        if (application.witnesses.isNotEmpty()) {
            SectionCard(title = "Witnesses") {
                application.witnesses.forEachIndexed { index, witness ->
                    DetailRow("Witness ${index + 1}", "${witness.name} · ${witness.contact}")
                }
            }
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

/** "2d 4h", "3h 12m" or "4m 09s", whichever fits how much time is left. */
private fun remainingLabel(totalSeconds: Long): String {
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
