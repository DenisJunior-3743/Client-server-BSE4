package com.trustbank.loanapp.data.remote

// Wire format of the Spring Boot API (backend: web/dto/AuthDtos.java, LoanDtos.java).
//
// These classes are the mobile side of the "RPC stubs": Gson marshals them to
// machine-independent JSON and back, so Kotlin on Android and Java on the server
// never need to agree on byte order or object layout — only on these field names.
// Dates travel as ISO-8601 strings and are converted in Mappers.kt.

data class LoginRequestDto(val email: String, val password: String)

data class RegisterRequestDto(val fullName: String, val email: String, val phone: String, val password: String)

data class OtpVerifyRequestDto(val email: String, val code: String)

data class OtpResendRequestDto(val email: String)

data class OtpIssuedDto(val otpExpiresAt: String?, val devOtp: String?)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val avatarColor: String?,
    val phoneVerified: Boolean,
)

data class LoginResponseDto(val accessToken: String, val tokenType: String, val expiresIn: Long, val user: UserDto)

/** [devOtp] is only present when the server runs its `dev` profile (no SMS gateway). */
data class RegisterResponseDto(val user: UserDto, val otpExpiresAt: String?, val devOtp: String?)

data class ProductDto(
    val id: String,
    val name: String,
    val type: String,
    val interestRateAnnual: Double,
    val minAmount: Double,
    val maxAmount: Double,
    val minTermMonths: Int,
    val maxTermMonths: Int,
    val active: Boolean,
)

data class EmploymentInfoDto(val employer: String, val jobTitle: String, val employmentType: String)

data class AddressDto(val district: String, val city: String, val street: String)

data class ProfileRequestDto(
    val nationalId: String,
    val dateOfBirth: String,
    val employmentInfo: EmploymentInfoDto,
    val incomeMonthly: Double,
    val address: AddressDto,
)

data class ProfileDto(
    val id: String,
    val userId: String,
    val nationalId: String,
    val dateOfBirth: String,
    val employmentInfo: Map<String, String>?,
    val incomeMonthly: Double,
    val address: Map<String, String>?,
)

data class SubmitApplicationRequestDto(
    val productId: String,
    val amountRequested: Double,
    val termMonths: Int,
    val purpose: String,
    /** The extra sections of the mobile form; the server stores them as JSONB. */
    val details: Map<String, Any>,
)

data class ApplicationDto(
    val id: String,
    val reference: String?,
    val productId: String,
    val productType: String?,
    val amountRequested: Double,
    val amountApproved: Double?,
    val interestRateAnnual: Double?,
    val termMonths: Int,
    val purpose: String,
    val status: String,
    val decisionRemarks: String?,
    val infoRequestMessage: String?,
    val offerExpiresAt: String?,
    val submittedAt: String,
    val updatedAt: String,
    val version: Long,
)

data class DocumentDto(
    val id: String,
    val loanApplicationId: String,
    val docType: String,
    val fileName: String,
    val verificationStatus: String,
    val uploadedAt: String,
    val contentType: String? = null,
)

data class ApplicationDetailDto(val application: ApplicationDto, val documents: List<DocumentDto>?)

data class InfoResponseRequestDto(val message: String)

data class RepaymentRequestDto(val amount: Double, val method: String)

data class RepaymentReceiptDto(
    val paymentReference: String,
    val amountPaid: Double,
    val outstandingBalance: Double,
    val loanStatus: String,
    val paidAt: String,
)

data class ScheduleItemDto(
    val installmentNo: Int,
    val dueDate: String,
    val principalDue: Double,
    val interestDue: Double,
    val amountDue: Double,
    val amountPaid: Double,
    val status: String,
)

data class NotificationDto(
    val id: String,
    val loanApplicationId: String?,
    val message: String,
    val sentAt: String,
    val readAt: String?,
)

/** The one error shape the server ever returns. */
data class ApiErrorDto(val status: Int?, val code: String?, val message: String?, val fieldErrors: Map<String, String>?)

data class UpdateMeRequestDto(val fullName: String, val email: String, val phone: String)
