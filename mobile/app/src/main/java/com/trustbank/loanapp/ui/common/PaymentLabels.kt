package com.trustbank.loanapp.ui.common

import com.trustbank.loanapp.data.model.PaymentTerms

fun paymentTermsLabel(terms: PaymentTerms): String = when (terms) {
    PaymentTerms.MONTHLY -> "Monthly"
    PaymentTerms.WEEKLY -> "Weekly"
    PaymentTerms.QUARTERLY -> "Quarterly"
    PaymentTerms.LUMP_SUM -> "Lump Sum (End of Term)"
}
