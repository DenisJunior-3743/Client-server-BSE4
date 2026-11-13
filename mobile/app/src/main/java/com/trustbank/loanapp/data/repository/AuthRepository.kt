package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun updateAccount(fullName: String, email: String, phone: String): Result<User>
    suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User>

    /** True when a new account must confirm a one-time code before it can sign in (real backend). */
    val requiresPhoneVerification: Boolean

    suspend fun verifyPhone(email: String, code: String): Result<Unit>

    suspend fun resendCode(email: String): Result<Unit>
}
