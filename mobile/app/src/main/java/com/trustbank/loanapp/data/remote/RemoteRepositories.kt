package com.trustbank.loanapp.data.remote

import android.util.Log
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.DocumentUpload
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.PendingVerification
import com.trustbank.loanapp.data.model.AppNotification
import com.trustbank.loanapp.data.model.ApplicantProfile
import com.trustbank.loanapp.data.model.EmploymentType
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.RepaymentInstallment
import com.trustbank.loanapp.data.model.RepaymentReceipt
import com.trustbank.loanapp.data.model.User
import com.trustbank.loanapp.data.repository.ApplicationRepository
import com.trustbank.loanapp.data.repository.AuthRepository
import com.trustbank.loanapp.data.repository.NewApplicationInput
import com.trustbank.loanapp.data.repository.NotificationRepository
import com.trustbank.loanapp.data.repository.ProductRepository
import com.trustbank.loanapp.data.repository.ProfileRepository
import java.time.LocalDate
import java.time.LocalDateTime

// All application repositories use the authenticated Spring Boot API.
//
// Reads are fail-soft (log, then return empty/null) because several screens load data
// without a try/catch; a dropped connection must show an empty list, not crash the app.
// Writes throw ApiException so the caller can show the server's message.

private const val TAG = "LoanApi"

private suspend fun <T> readOr(fallback: T, block: suspend LoanApi.() -> T): T =
    try {
        ApiClient.callWithRetry(block = block) // GETs are idempotent: safe to retransmit
    } catch (e: ApiException) {
        Log.w(TAG, "Read failed: ${e.code} ${e.message}")
        fallback
    }

class RemoteAuthRepository : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> = runApi {
        val reply = ApiClient.call { login(LoginRequestDto(email.trim(), password)) }
        if (reply.user.role != "BORROWER") {
            throw ApiException(403, "STAFF_ACCOUNT", "This app is for borrowers. Staff use the web dashboard.")
        }
        TokenStore.accessToken = reply.accessToken
        reply.user.toModel()
    }

    override suspend fun updateAccount(fullName: String, email: String, phone: String): Result<User> = runApi {
        ApiClient.call { updateMe(UpdateMeRequestDto(fullName.trim(), email.trim(), phone)).toModel() }
    }

    override suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User> = runApi {
        val created = ApiClient.call { register(RegisterRequestDto(fullName.trim(), email.trim(), phone, password)) }
        // No token yet: the account cannot sign in until the one-time code is confirmed
        // (VerifyPhoneScreen). Remember what that screen needs, in memory only.
        AppContainer.pendingVerification = PendingVerification(
            email = email.trim(),
            password = password,
            expiresAt = parseInstantOrNull(created.otpExpiresAt) ?: LocalDateTime.now().plusMinutes(5),
            devCode = created.devOtp,
        )
        created.user.toModel()
    }

    override val requiresPhoneVerification: Boolean get() = true

    /**
     * Firm deadline: the server rejects a code presented after its expiry (HTTP 410), however
     * correct it is. The screen's countdown only mirrors that; the server decides.
     */
    override suspend fun verifyPhone(email: String, code: String): Result<Unit> = runApi {
        val reply = ApiClient.call { verifyOtp(OtpVerifyRequestDto(email, code)) }
        if (!reply.isSuccessful) {
            val message = when (reply.code()) {
                410 -> "This code has expired. Request a new one."
                429 -> "Too many attempts. Request a new code."
                else -> "That code is not correct."
            }
            throw ApiException(reply.code(), if (reply.code() == 410) "DEADLINE_EXPIRED" else "INVALID_CODE", message)
        }
        Unit
    }

    override suspend fun resendCode(email: String): Result<Unit> = runApi {
        val issued = ApiClient.call { resendOtp(OtpResendRequestDto(email)) }
        val pending = AppContainer.pendingVerification
        if (pending != null) {
            AppContainer.pendingVerification = pending.copy(
                expiresAt = parseInstantOrNull(issued.otpExpiresAt) ?: LocalDateTime.now().plusMinutes(5),
                devCode = issued.devOtp,
            )
        }
        Unit
    }

    private inline fun <T> runApi(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: ApiException) {
            Result.failure(e)
        }
}

class RemoteProductRepository : ProductRepository {
    override suspend fun listProducts(): List<LoanProduct> {
        val products = readOr(emptyList()) { listProducts().map { it.toModel() } }
        products.forEach { AppContainer.productCache[it.id] = it }
        return products
    }

    override suspend fun getProduct(id: String): LoanProduct? = listProducts().find { it.id == id }
}

class RemoteProfileRepository : ProfileRepository {

    override suspend fun getProfile(): ApplicantProfile =
        // 404 simply means "not filled in yet": hand the form an empty profile to complete.
        readOr(blankProfile()) { getProfile().toModel() }

    override suspend fun updateProfile(profile: ApplicantProfile): ApplicantProfile =
        // PUT is idempotent (saving the same profile twice leaves the same profile), so it may be retried.
        ApiClient.callWithRetry { saveProfile(profile.toRequest()).toModel() }

    private fun blankProfile() = ApplicantProfile(
        userId = AppContainer.session.currentUser.value?.id.orEmpty(),
        nationalId = "",
        dateOfBirth = LocalDate.of(2000, 1, 1),
        employer = "",
        jobTitle = "",
        employmentType = EmploymentType.FORMAL,
        incomeMonthly = 0.0,
        district = "",
        address = "",
    )
}

class RemoteApplicationRepository : ApplicationRepository {

    override suspend fun listApplications(): List<LoanApplication> {
        // Rows show the product name, so make sure the product cache is warm first.
        if (AppContainer.productCache.isEmpty()) AppContainer.productRepository.listProducts()
        return readOr(emptyList()) { listApplications().map { it.toModel() } }
    }

    override suspend fun getApplication(id: String): LoanApplication? =
        readOr(null) { getApplication(id).application.toModel() }

    override suspend fun getDocuments(applicationId: String): List<LoanDocument> =
        readOr(emptyList()) { getApplication(applicationId).documents.orEmpty().map { it.toModel() } }

    override suspend fun uploadDocument(applicationId: String, type: DocumentType, upload: DocumentUpload): LoanDocument =
        ApiClient.call {
            uploadDocument(applicationId, type.name.toRequestBody("text/plain".toMediaType()),
                MultipartBody.Part.createFormData("file", upload.fileName,
                    upload.bytes.toRequestBody(upload.contentType.toMediaType()))).toModel()
        }

    /**
     * At-most-once submission. [NewApplicationInput.idempotencyKey] was created once for this
     * form by the view model, and EVERY attempt below (and any later tap on "Submit" for the
     * same unchanged form) sends that same key. If the first request reached the server but
     * its reply was lost, the retry gets the original loan back instead of creating another.
     */
    override suspend fun submitApplication(input: NewApplicationInput): LoanApplication {
        val details = mapOf<String, Any>(
            "applicantFullName" to input.applicantFullName,
            "applicantDob" to input.applicantDob.toString(),
            "applicantNin" to input.applicantNin,
            "physicalLocation" to input.physicalLocation,
            "businessName" to input.businessName,
            "businessLocation" to input.businessLocation,
            "collateralSecurity" to input.collateralSecurity,
            "witnesses" to input.witnesses.map { mapOf("name" to it.name, "contact" to it.contact) },
            "paymentTerms" to input.paymentTerms.name,
        )
        val body = SubmitApplicationRequestDto(
            productId = input.productId,
            amountRequested = input.amountRequested,
            termMonths = input.termMonths,
            purpose = input.purpose,
            details = details,
        )
        val created = ApiClient.callWithRetry { submitApplication(input.idempotencyKey, body) }
        // Keep what the borrower typed on the returned model (the list endpoint does not echo it back).
        return created.toModel().copy(
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
    }

    override suspend fun respondToInfoRequest(applicationId: String): LoanApplication =
        ApiClient.call {
            respondToInfoRequest(applicationId, InfoResponseRequestDto("Requested information provided via the mobile app."))
                .toModel()
        }

    // `this.` = the Retrofit API (the lambda's receiver), not this repository's method of the same name.

    override suspend fun acceptOffer(applicationId: String, idempotencyKey: String): LoanApplication =
        ApiClient.callWithRetry { this.acceptOffer(applicationId, idempotencyKey).toModel() }

    override suspend fun getSchedule(applicationId: String): List<RepaymentInstallment> =
        readOr(emptyList()) { this.getSchedule(applicationId).map { it.toModel() } }

    override suspend fun repay(applicationId: String, amount: Double, idempotencyKey: String): RepaymentReceipt =
        ApiClient.callWithRetry {
            this.repay(applicationId, idempotencyKey, RepaymentRequestDto(amount, "MOBILE_MONEY")).toModel()
        }
}

class RemoteNotificationRepository : NotificationRepository {
    override suspend fun listNotifications(): List<AppNotification> =
        readOr(emptyList()) { listNotifications().map { it.toModel() } }

    override suspend fun markRead(id: String) {
        try {
            ApiClient.call { markNotificationRead(id) }
        } catch (e: ApiException) {
            Log.w(TAG, "Could not mark notification read: ${e.message}")
        }
    }
}
