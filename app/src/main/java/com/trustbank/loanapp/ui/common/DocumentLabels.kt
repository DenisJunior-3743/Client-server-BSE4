package com.trustbank.loanapp.ui.common

import com.trustbank.loanapp.data.model.DocumentType

fun documentTypeLabel(type: DocumentType): String = when (type) {
    DocumentType.NATIONAL_ID -> "National ID"
    DocumentType.PAYSLIP -> "Payslip / Income Proof"
    DocumentType.BANK_STATEMENT -> "Bank Statement (3 months)"
    DocumentType.COLLATERAL_PROOF -> "Collateral Proof"
    DocumentType.PASSPORT_PHOTO -> "Passport Photo"
}
