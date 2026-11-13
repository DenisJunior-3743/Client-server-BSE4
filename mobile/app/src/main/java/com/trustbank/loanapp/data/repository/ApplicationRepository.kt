package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.DocumentUpload
import com.trustbank.loanapp.data.model.DocumentVerificationStatus
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.data.model.PaymentTerms
import com.trustbank.loanapp.data.model.RepaymentInstallment
import com.trustbank.loanapp.data.model.RepaymentReceipt
import com.trustbank.loanapp.data.model.Witness
import java.time.LocalDate
import java.time.LocalDateTime

data class NewApplicationInput(
    val productId: String,
    val amountRequested: Double,
    val termMonths: Int,
    val purpose: String,
    val applicantFullName: String,
    val applicantDob: LocalDate,
    val applicantNin: String,
    val physicalLocation: String,
    val businessName: String,
    val businessLocation: String,
    val collateralSecurity: String,
    val witnesses: List<Witness>,
    val paymentTerms: PaymentTerms,
    /**
     * Identifies this submission to the server. Created once per filled-in form and sent again
     * unchanged on every retry, so a retried request can never create a second loan.
     */
    val idempotencyKey: String = "",
)

interface ApplicationRepository {
    suspend fun listApplications(): List<LoanApplication>
    suspend fun getApplication(id: String): LoanApplication?
    suspend fun getDocuments(applicationId: String): List<LoanDocument>
    suspend fun uploadDocument(applicationId: String, type: DocumentType, upload: DocumentUpload): LoanDocument
    suspend fun submitApplication(input: NewApplicationInput): LoanApplication
    suspend fun respondToInfoRequest(applicationId: String): LoanApplication

    // The two calls below move money, so each takes an idempotency key: the caller creates it
    // once per user action and passes the same key again when retrying that action.
    suspend fun acceptOffer(applicationId: String, idempotencyKey: String): LoanApplication
    suspend fun getSchedule(applicationId: String): List<RepaymentInstallment>
    suspend fun repay(applicationId: String, amount: Double, idempotencyKey: String): RepaymentReceipt
}
