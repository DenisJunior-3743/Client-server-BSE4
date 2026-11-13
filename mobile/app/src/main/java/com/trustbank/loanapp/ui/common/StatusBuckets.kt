package com.trustbank.loanapp.ui.common

import com.trustbank.loanapp.data.model.LoanStatus

// Coarse groupings used by Home/Applications screens for quick counts —
// separate from statusMeta()'s per-status label/tone.
val IN_PROGRESS_STATUSES = setOf(
    LoanStatus.DRAFT,
    LoanStatus.SUBMITTED,
    LoanStatus.PENDING_DOCUMENTS,
    LoanStatus.CREDIT_CHECK_PENDING,
    LoanStatus.CREDIT_CHECK_PASSED,
    LoanStatus.UNDER_REVIEW,
)

val NEEDS_ACTION_STATUSES = setOf(
    LoanStatus.AWAITING_ADDITIONAL_INFO,
    LoanStatus.DOCUMENTS_REJECTED,
)

val ACTIVE_STATUSES = setOf(
    LoanStatus.APPROVED,
    LoanStatus.PENDING_DISBURSEMENT,
    LoanStatus.DISBURSED,
    LoanStatus.ACTIVE,
)

val CLOSED_STATUSES = setOf(
    LoanStatus.REJECTED,
    LoanStatus.CREDIT_CHECK_FAILED,
    LoanStatus.CLOSED,
    LoanStatus.DEFAULTED,
    LoanStatus.DELINQUENT,
)
