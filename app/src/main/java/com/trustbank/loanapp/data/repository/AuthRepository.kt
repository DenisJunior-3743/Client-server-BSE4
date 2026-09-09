package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.User
import kotlinx.coroutines.delay

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User>
}

// Task 1 has no backend yet — this simulates the network round trip and
// checks against the single demo applicant account in MockData.
class FakeAuthRepository : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        delay(500)
        return if (email.trim().equals(MockData.currentUser.email, ignoreCase = true) && password == MockData.DEMO_PASSWORD) {
            Result.success(MockData.currentUser)
        } else {
            Result.failure(Exception("Invalid email or password"))
        }
    }

    override suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User> {
        delay(500)
        return Result.success(MockData.currentUser.copy(fullName = fullName, email = email, phone = phone))
    }
}
