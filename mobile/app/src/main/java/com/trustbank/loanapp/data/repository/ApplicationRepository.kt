package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
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
import kotlinx.coroutines.delay
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

class FakeApplicationRepository : ApplicationRepository {
    override suspend fun listApplications(): List<LoanApplication> {
        delay(300)
        return MockData.applications.sortedByDescending { it.submittedAt }
    }

    override suspend fun getApplication(id: String): LoanApplication? {
        delay(200)
        return MockData.applications.find { it.id == id }
    }

    override suspend fun getDocuments(applicationId: String): List<LoanDocument> {
        delay(200)
        return MockData.documents.filter { it.loanApplicationId == applicationId }
    }

    override suspend fun uploadDocument(applicationId: String, type: DocumentType, upload: DocumentUpload): LoanDocument {
        delay(200)
        val document = LoanDocument(java.util.UUID.randomUUID().toString(), applicationId, type,
            upload.fileName, DocumentVerificationStatus.PENDING, LocalDateTime.now())
        MockData.documents.add(document)
        return document
    }

    override suspend fun submitApplication(input: NewApplicationInput): LoanApplication {
        delay(500)
        val application = LoanApplication(
            id = "LN-${1000 + MockData.applications.size + 1}",
            productId = input.productId,
            amountRequested = input.amountRequested,
            termMonths = input.termMonths,
            purpose = input.purpose,
            status = LoanStatus.SUBMITTED,
            submittedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            applicantFullName = input.applicantFullName,
            applicantDob = input.applicantDob,
            applicantNin = input.applicantNin,
            physicalLocation = input.physicalLocation,
            businessName = input.businessName,
            businessLocation = input.businessLocation,
            collateralSecurity = input.collateralSecurity,
            witnesses = input.witnesses,
            paymentTerms = input.paymentTerms,
        )
        MockData.applications.add(0, application)
        return application
    }

    override suspend fun respondToInfoRequest(applicationId: String): LoanApplication {
        delay(400)
        val index = MockData.applications.indexOfFirst { it.id == applicationId }
        require(index >= 0) { "Application not found" }
        val updated = MockData.applications[index].copy(
            status = LoanStatus.SUBMITTED,
            infoRequestMessage = null,
            updatedAt = LocalDateTime.now(),
        )
        MockData.applications[index] = updated
        return updated
    }

    override suspend fun acceptOffer(applicationId: String, idempotencyKey: String): LoanApplication {
        delay(400)
        val index = MockData.applications.indexOfFirst { it.id == applicationId }
        require(index >= 0) { "Application not found" }
        val updated = MockData.applications[index].copy(
            status = LoanStatus.PENDING_DISBURSEMENT,
            updatedAt = LocalDateTime.now(),
        )
        MockData.applications[index] = updated
        return updated
    }

    // Demo data has no repayment schedules; the real backend generates one at disbursement.
    override suspend fun getSchedule(applicationId: String): List<RepaymentInstallment> {
        delay(200)
        return emptyList()
    }

    override suspend fun repay(applicationId: String, amount: Double, idempotencyKey: String): RepaymentReceipt {
        delay(400)
        return RepaymentReceipt(paymentReference = "PAY-DEMO", amountPaid = amount, outstandingBalance = 0.0)
    }
}
