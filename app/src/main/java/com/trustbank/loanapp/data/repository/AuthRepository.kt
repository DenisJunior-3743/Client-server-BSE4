package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.User
import kotlinx.coroutines.delay

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User>
}

// Task 1 has no backend yet — there's no real account database to check
// credentials against, and the screen has already validated email
// format/required-ness before calling this. So any well-formed attempt signs
// you in as the single demo applicant (with the email you typed), rather
// than only accepting one hardcoded demo password.
class FakeAuthRepository : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        delay(500)
        return if (email.isNotBlank() && password.isNotBlank()) {
            Result.success(MockData.currentUser.copy(email = email.trim()))
        } else {
            Result.failure(Exception("Invalid email or password"))
        }
    }

    override suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User> {
        delay(500)
        return Result.success(MockData.currentUser.copy(fullName = fullName, email = email, phone = phone))
    }
}
