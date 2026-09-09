package com.trustbank.loanapp.data.model

import java.time.LocalDate
import java.time.LocalDateTime

// Mirrors diagrams/erd.puml and web/src/types/index.ts — the applicant-side
// subset the mobile app needs. Field names match so the eventual Retrofit
// DTOs in Phase 2 can reuse these shapes with minimal changes.

enum class LoanStatus {
    DRAFT, SUBMITTED, PENDING_DOCUMENTS, DOCUMENTS_REJECTED,
    CREDIT_CHECK_PENDING, CREDIT_CHECK_FAILED, CREDIT_CHECK_PASSED,
    UNDER_REVIEW, AWAITING_ADDITIONAL_INFO,
    APPROVED, REJECTED,
    PENDING_DISBURSEMENT, DISBURSED, ACTIVE, DELINQUENT, CLOSED, DEFAULTED,
}

enum class EmploymentType { FORMAL, SELF_EMPLOYED, INFORMAL }

enum class ProductType { PERSONAL, BUSINESS, AGRICULTURE, ASSET_FINANCE, EMERGENCY }

enum class DocumentType { NATIONAL_ID, PAYSLIP, BANK_STATEMENT, COLLATERAL_PROOF, PASSPORT_PHOTO }

enum class DocumentVerificationStatus { PENDING, VERIFIED, REJECTED }

enum class PaymentTerms { MONTHLY, WEEKLY, QUARTERLY, LUMP_SUM }

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val avatarColorHex: String,
)

data class ApplicantProfile(
    val userId: String,
    val nationalId: String,
    val dateOfBirth: LocalDate,
    val employer: String,
    val jobTitle: String,
    val employmentType: EmploymentType,
    val incomeMonthly: Double,
    val district: String,
    val address: String,
)

data class LoanProduct(
    val id: String,
    val name: String,
    val type: ProductType,
    val interestRateAnnual: Double,
    val minAmount: Double,
    val maxAmount: Double,
    val minTermMonths: Int,
    val maxTermMonths: Int,
)

data class Witness(
    val name: String,
    val contact: String,
)

data class LoanDocument(
    val id: String,
    val loanApplicationId: String,
    val docType: DocumentType,
    val fileName: String,
    val verificationStatus: DocumentVerificationStatus,
    val uploadedAt: LocalDateTime,
)

data class LoanApplication(
    val id: String,
    val productId: String,
    val amountRequested: Double,
    val amountApproved: Double? = null,
    val termMonths: Int,
    val purpose: String,
    val status: LoanStatus,
    val submittedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val decisionRemarks: String? = null,
    val infoRequestMessage: String? = null,
    // Captured on the multi-phase application form (§ physical/business/collateral details).
    val applicantFullName: String? = null,
    val applicantDob: LocalDate? = null,
    val applicantNin: String? = null,
    val physicalLocation: String? = null,
    val businessName: String? = null,
    val businessLocation: String? = null,
    val collateralSecurity: String? = null,
    val witnesses: List<Witness> = emptyList(),
    val paymentTerms: PaymentTerms? = null,
)

data class AppNotification(
    val id: String,
    val loanApplicationId: String?,
    val message: String,
    val sentAt: LocalDateTime,
    val read: Boolean,
)
