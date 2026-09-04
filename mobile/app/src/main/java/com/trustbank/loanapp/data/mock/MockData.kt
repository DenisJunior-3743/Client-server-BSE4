package com.trustbank.loanapp.data.mock

import com.trustbank.loanapp.data.model.ApplicantProfile
import com.trustbank.loanapp.data.model.AppNotification
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.DocumentVerificationStatus
import com.trustbank.loanapp.data.model.EmploymentType
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.data.model.ProductType
import com.trustbank.loanapp.data.model.User
import java.time.LocalDate
import java.time.LocalDateTime

// In-memory data standing in for the backend during Task 1. Field shapes
// mirror diagrams/erd.puml so Phase 2 can swap this module for a
// Retrofit-backed repository without touching the UI layer.
object MockData {

    const val DEMO_PASSWORD = "Passw0rd1"

    val currentUser = User(
        id = "borrower-01",
        fullName = "Allan Tumusiime",
        email = "allan.tumusiime@mail.com",
        phone = "0781122334",
        avatarColorHex = "#2563EB",
    )

    var applicantProfile = ApplicantProfile(
        userId = currentUser.id,
        nationalId = "CM88213445B77",
        dateOfBirth = LocalDate.of(1994, 5, 12),
        employer = "Mbarara University",
        jobTitle = "Lecturer",
        employmentType = EmploymentType.FORMAL,
        incomeMonthly = 1_800_000.0,
        district = "Mbarara",
        address = "Plot 45, Kakoba",
    )

    val products = listOf(
        LoanProduct("prod-personal", "Personal Loan", ProductType.PERSONAL, 19.0, 500_000.0, 15_000_000.0, 3, 36),
        LoanProduct("prod-business", "Business Growth Loan", ProductType.BUSINESS, 21.0, 2_000_000.0, 80_000_000.0, 6, 60),
        LoanProduct("prod-agri", "Agriculture Loan", ProductType.AGRICULTURE, 16.0, 500_000.0, 25_000_000.0, 3, 24),
        LoanProduct("prod-asset", "Asset Finance", ProductType.ASSET_FINANCE, 18.0, 3_000_000.0, 60_000_000.0, 12, 48),
        LoanProduct("prod-emergency", "Emergency Loan", ProductType.EMERGENCY, 24.0, 200_000.0, 5_000_000.0, 1, 12),
    )

    fun productById(id: String): LoanProduct? = products.find { it.id == id }

    val applications = mutableListOf(
        LoanApplication(
            id = "LN-1001",
            productId = "prod-business",
            amountRequested = 12_000_000.0,
            termMonths = 24,
            purpose = "Expand retail shop stock",
            status = LoanStatus.UNDER_REVIEW,
            submittedAt = LocalDateTime.now().minusDays(6),
            updatedAt = LocalDateTime.now().minusDays(1),
        ),
        LoanApplication(
            id = "LN-1002",
            productId = "prod-emergency",
            amountRequested = 800_000.0,
            amountApproved = 800_000.0,
            termMonths = 6,
            purpose = "Medical expenses",
            status = LoanStatus.ACTIVE,
            submittedAt = LocalDateTime.now().minusDays(70),
            updatedAt = LocalDateTime.now().minusDays(50),
            decisionRemarks = "Meets lending criteria; approved as requested.",
        ),
        LoanApplication(
            id = "LN-1003",
            productId = "prod-personal",
            amountRequested = 3_500_000.0,
            termMonths = 12,
            purpose = "School fees",
            status = LoanStatus.AWAITING_ADDITIONAL_INFO,
            submittedAt = LocalDateTime.now().minusDays(12),
            updatedAt = LocalDateTime.now().minusDays(2),
            infoRequestMessage = "Please upload your latest 3-month bank statement.",
        ),
        LoanApplication(
            id = "LN-1004",
            productId = "prod-agri",
            amountRequested = 6_000_000.0,
            termMonths = 18,
            purpose = "Purchase of farm inputs",
            status = LoanStatus.CREDIT_CHECK_PENDING,
            submittedAt = LocalDateTime.now().minusDays(3),
            updatedAt = LocalDateTime.now().minusDays(1),
        ),
        LoanApplication(
            id = "LN-1005",
            productId = "prod-personal",
            amountRequested = 2_000_000.0,
            termMonths = 9,
            purpose = "Home renovation",
            status = LoanStatus.REJECTED,
            submittedAt = LocalDateTime.now().minusDays(40),
            updatedAt = LocalDateTime.now().minusDays(33),
            decisionRemarks = "Debt-to-income ratio exceeds policy threshold.",
        ),
        LoanApplication(
            id = "LN-1006",
            productId = "prod-asset",
            amountRequested = 18_000_000.0,
            termMonths = 36,
            purpose = "Vehicle purchase",
            status = LoanStatus.PENDING_DOCUMENTS,
            submittedAt = LocalDateTime.now().minusDays(2),
            updatedAt = LocalDateTime.now().minusHours(20),
        ),
    )

    val documents = mutableListOf(
        LoanDocument("DOC-2001", "LN-1006", DocumentType.NATIONAL_ID, "national_id_LN-1006.pdf", DocumentVerificationStatus.VERIFIED, LocalDateTime.now().minusDays(2)),
        LoanDocument("DOC-2002", "LN-1006", DocumentType.PAYSLIP, "payslip_LN-1006.pdf", DocumentVerificationStatus.REJECTED, LocalDateTime.now().minusDays(2)),
        LoanDocument("DOC-2003", "LN-1001", DocumentType.NATIONAL_ID, "national_id_LN-1001.pdf", DocumentVerificationStatus.VERIFIED, LocalDateTime.now().minusDays(6)),
        LoanDocument("DOC-2004", "LN-1001", DocumentType.BANK_STATEMENT, "bank_statement_LN-1001.pdf", DocumentVerificationStatus.VERIFIED, LocalDateTime.now().minusDays(6)),
    )

    val notifications = mutableListOf(
        AppNotification("NOTE-01", "LN-1003", "Loan Officer requested more information on application LN-1003.", LocalDateTime.now().minusDays(2), read = false),
        AppNotification("NOTE-02", "LN-1005", "Your application LN-1005 was rejected.", LocalDateTime.now().minusDays(33), read = true),
        AppNotification("NOTE-03", "LN-1002", "Application LN-1002 has been disbursed.", LocalDateTime.now().minusDays(50), read = true),
        AppNotification("NOTE-04", "LN-1004", "Application LN-1004 is undergoing a credit check.", LocalDateTime.now().minusDays(1), read = false),
        AppNotification("NOTE-05", null, "Welcome to TrustBank Loans — complete your profile to speed up future applications.", LocalDateTime.now().minusDays(90), read = true),
    )
}
