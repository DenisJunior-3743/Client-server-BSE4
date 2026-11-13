package com.trustbank.loanapp.data.remote

import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * The borrower-facing REST API as a Retrofit interface. Retrofit generates the client stub:
 * each call below looks like a local suspend function, and marshalling, the HTTP request
 * over TCP, and unmarshalling happen behind it (the RPC model from the course notes).
 *
 * Which calls are safe to send twice:
 *  - GET and PUT are idempotent by nature, so they can simply be retried.
 *  - The three POSTs that create something or move money take an `Idempotency-Key`.
 *    The server runs each key AT MOST ONCE and replays the first reply to any retry, so a
 *    retry on a flaky network can never create a second loan or take a payment twice.
 */
interface LoanApi {

    // ---- auth (no token needed) ----

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): RegisterResponseDto

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyRequestDto): Response<Unit>

    @POST("api/v1/auth/otp/resend")
    suspend fun resendOtp(@Body body: OtpResendRequestDto): OtpIssuedDto

    // ---- products / profile ----

    @GET("api/v1/products")
    suspend fun listProducts(): List<ProductDto>

    @GET("api/v1/me/profile")
    suspend fun getProfile(): ProfileDto

    @PUT("api/v1/me/profile")
    suspend fun saveProfile(@Body body: ProfileRequestDto): ProfileDto

    // ---- applications ----

    @POST("api/v1/applications")
    suspend fun submitApplication(
        @Header(IDEMPOTENCY_KEY) idempotencyKey: String,
        @Body body: SubmitApplicationRequestDto,
    ): ApplicationDto

    @GET("api/v1/applications")
    suspend fun listApplications(): List<ApplicationDto>

    @Multipart
    @POST("api/v1/applications/{id}/documents")
    suspend fun uploadDocument(@Path("id") id: String, @Part("docType") docType: RequestBody,
        @Part file: MultipartBody.Part): DocumentDto

    @GET("api/v1/applications/{id}")
    suspend fun getApplication(@Path("id") id: String): ApplicationDetailDto

    @POST("api/v1/applications/{id}/info-response")
    suspend fun respondToInfoRequest(@Path("id") id: String, @Body body: InfoResponseRequestDto): ApplicationDto

    /** Firm deadline: the server answers 410 DEADLINE_EXPIRED once `offerExpiresAt` has passed. */
    @POST("api/v1/applications/{id}/offer/accept")
    suspend fun acceptOffer(
        @Path("id") id: String,
        @Header(IDEMPOTENCY_KEY) idempotencyKey: String,
    ): ApplicationDto

    @GET("api/v1/applications/{id}/schedule")
    suspend fun getSchedule(@Path("id") id: String): List<ScheduleItemDto>

    @POST("api/v1/applications/{id}/repayments")
    suspend fun repay(
        @Path("id") id: String,
        @Header(IDEMPOTENCY_KEY) idempotencyKey: String,
        @Body body: RepaymentRequestDto,
    ): RepaymentReceiptDto

    // ---- notifications ----

    @GET("api/v1/me/notifications")
    suspend fun listNotifications(): List<NotificationDto>

    @POST("api/v1/me/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): NotificationDto

    companion object {
        const val IDEMPOTENCY_KEY = "Idempotency-Key"
    }
}
