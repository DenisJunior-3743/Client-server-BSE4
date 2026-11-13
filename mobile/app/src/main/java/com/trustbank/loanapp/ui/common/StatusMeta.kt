package com.trustbank.loanapp.ui.common

import androidx.compose.ui.graphics.Color
import com.trustbank.loanapp.data.model.DocumentVerificationStatus
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.ui.theme.AppColors

enum class StatusTone { NEUTRAL, PRIMARY, WARNING, SUCCESS, DANGER }

data class StatusMeta(val label: String, val tone: StatusTone)

// Mirrors web/src/lib/status.ts's STATUS_META table.
fun statusMeta(status: LoanStatus): StatusMeta = when (status) {
    LoanStatus.DRAFT -> StatusMeta("Draft", StatusTone.NEUTRAL)
    LoanStatus.SUBMITTED -> StatusMeta("Submitted", StatusTone.PRIMARY)
    LoanStatus.PENDING_DOCUMENTS -> StatusMeta("Pending Documents", StatusTone.WARNING)
    LoanStatus.DOCUMENTS_REJECTED -> StatusMeta("Documents Rejected", StatusTone.DANGER)
    LoanStatus.CREDIT_CHECK_PENDING -> StatusMeta("Credit Check Pending", StatusTone.PRIMARY)
    LoanStatus.CREDIT_CHECK_FAILED -> StatusMeta("Credit Check Failed", StatusTone.DANGER)
    LoanStatus.CREDIT_CHECK_PASSED -> StatusMeta("Credit Check Passed", StatusTone.SUCCESS)
    LoanStatus.UNDER_REVIEW -> StatusMeta("Under Review", StatusTone.PRIMARY)
    LoanStatus.AWAITING_ADDITIONAL_INFO -> StatusMeta("Awaiting Info", StatusTone.WARNING)
    LoanStatus.APPROVED -> StatusMeta("Approved", StatusTone.SUCCESS)
    LoanStatus.REJECTED -> StatusMeta("Rejected", StatusTone.DANGER)
    LoanStatus.PENDING_DISBURSEMENT -> StatusMeta("Pending Disbursement", StatusTone.PRIMARY)
    LoanStatus.DISBURSED -> StatusMeta("Disbursed", StatusTone.SUCCESS)
    LoanStatus.ACTIVE -> StatusMeta("Active", StatusTone.SUCCESS)
    LoanStatus.DELINQUENT -> StatusMeta("Delinquent", StatusTone.WARNING)
    LoanStatus.CLOSED -> StatusMeta("Closed", StatusTone.NEUTRAL)
    LoanStatus.DEFAULTED -> StatusMeta("Defaulted", StatusTone.DANGER)
}

fun StatusTone.toColor(): Color = when (this) {
    StatusTone.NEUTRAL -> AppColors.Neutral500
    StatusTone.PRIMARY -> AppColors.Primary600
    StatusTone.WARNING -> AppColors.Warning600
    StatusTone.SUCCESS -> AppColors.Success600
    StatusTone.DANGER -> AppColors.Danger600
}

fun DocumentVerificationStatus.toTone(): StatusTone = when (this) {
    DocumentVerificationStatus.VERIFIED -> StatusTone.SUCCESS
    DocumentVerificationStatus.REJECTED -> StatusTone.DANGER
    DocumentVerificationStatus.PENDING -> StatusTone.WARNING
}

fun StatusTone.toContainerColor(): Color = when (this) {
    StatusTone.NEUTRAL -> AppColors.Neutral100
    StatusTone.PRIMARY -> AppColors.Primary50
    StatusTone.WARNING -> AppColors.Warning50
    StatusTone.SUCCESS -> AppColors.Success50
    StatusTone.DANGER -> AppColors.Danger50
}
