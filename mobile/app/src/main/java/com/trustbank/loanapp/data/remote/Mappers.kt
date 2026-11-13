package com.trustbank.loanapp.data.remote

import com.trustbank.loanapp.data.model.AppNotification
import com.trustbank.loanapp.data.model.ApplicantProfile
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.DocumentVerificationStatus
import com.trustbank.loanapp.data.model.EmploymentType
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.data.model.ProductType
import com.trustbank.loanapp.data.model.RepaymentInstallment
import com.trustbank.loanapp.data.model.RepaymentReceipt
import com.trustbank.loanapp.data.model.User
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// Wire DTO -> the app's own models, so no screen has to know the server's JSON.

private fun parseInstant(value: String?): LocalDateTime =
    try {
        LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault())
    } catch (_: Exception) {
        LocalDateTime.now()
    }

fun parseInstantOrNull(value: String?): LocalDateTime? =
    try {
        if (value == null) null else LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault())
    } catch (_: Exception) {
        null
    }

private inline fun <reified E : Enum<E>> enumOr(value: String?, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == value } ?: fallback

fun UserDto.toModel() = User(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    avatarColorHex = avatarColor ?: "#1E3A8A",
)

fun ProductDto.toModel() = LoanProduct(
    id = id,
    name = name,
    type = enumOr(type, ProductType.PERSONAL),
    interestRateAnnual = interestRateAnnual,
    minAmount = minAmount,
    maxAmount = maxAmount,
    minTermMonths = minTermMonths,
    maxTermMonths = maxTermMonths,
)

fun ApplicationDto.toModel() = LoanApplication(
    id = id,
    productId = productId,
    amountRequested = amountRequested,
    amountApproved = amountApproved,
    termMonths = termMonths,
    purpose = purpose,
    status = enumOr(status, LoanStatus.SUBMITTED),
    submittedAt = parseInstant(submittedAt),
    updatedAt = parseInstant(updatedAt),
    decisionRemarks = decisionRemarks,
    infoRequestMessage = infoRequestMessage,
    reference = reference,
    offerExpiresAt = parseInstantOrNull(offerExpiresAt),
)

fun ScheduleItemDto.toModel() = RepaymentInstallment(
    installmentNo = installmentNo,
    dueDate = try {
        LocalDate.parse(dueDate)
    } catch (_: Exception) {
        LocalDate.now()
    },
    amountDue = amountDue,
    amountPaid = amountPaid,
    status = status,
)

fun RepaymentReceiptDto.toModel() = RepaymentReceipt(
    paymentReference = paymentReference,
    amountPaid = amountPaid,
    outstandingBalance = outstandingBalance,
)

fun DocumentDto.toModel() = LoanDocument(
    id = id,
    loanApplicationId = loanApplicationId,
    docType = enumOr(docType, DocumentType.NATIONAL_ID),
    fileName = fileName,
    verificationStatus = enumOr(verificationStatus, DocumentVerificationStatus.PENDING),
    uploadedAt = parseInstant(uploadedAt),
)

fun NotificationDto.toModel() = AppNotification(
    id = id,
    loanApplicationId = loanApplicationId,
    message = message,
    sentAt = parseInstant(sentAt),
    read = readAt != null,
)

fun ProfileDto.toModel() = ApplicantProfile(
    userId = userId,
    nationalId = nationalId,
    dateOfBirth = try {
        LocalDate.parse(dateOfBirth)
    } catch (_: Exception) {
        LocalDate.of(2000, 1, 1)
    },
    employer = employmentInfo?.get("employer").orEmpty(),
    jobTitle = employmentInfo?.get("jobTitle").orEmpty(),
    employmentType = enumOr(employmentInfo?.get("employmentType"), EmploymentType.FORMAL),
    incomeMonthly = incomeMonthly,
    district = address?.get("district").orEmpty(),
    address = address?.get("street").orEmpty(),
)

fun ApplicantProfile.toRequest() = ProfileRequestDto(
    nationalId = nationalId,
    dateOfBirth = dateOfBirth.toString(),
    employmentInfo = EmploymentInfoDto(employer = employer, jobTitle = jobTitle, employmentType = employmentType.name),
    incomeMonthly = incomeMonthly,
    // The mobile form has one district field and one address line; the server stores district/city/street.
    address = AddressDto(district = district, city = district, street = address),
)
