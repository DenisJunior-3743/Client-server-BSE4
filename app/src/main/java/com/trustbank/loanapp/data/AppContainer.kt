package com.trustbank.loanapp.data

import com.trustbank.loanapp.data.model.User
import com.trustbank.loanapp.data.repository.ApplicationRepository
import com.trustbank.loanapp.data.repository.AuthRepository
import com.trustbank.loanapp.data.repository.FakeApplicationRepository
import com.trustbank.loanapp.data.repository.FakeAuthRepository
import com.trustbank.loanapp.data.repository.FakeNotificationRepository
import com.trustbank.loanapp.data.repository.FakeProductRepository
import com.trustbank.loanapp.data.repository.FakeProfileRepository
import com.trustbank.loanapp.data.repository.NotificationRepository
import com.trustbank.loanapp.data.repository.ProductRepository
import com.trustbank.loanapp.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    fun signIn(user: User) {
        _currentUser.value = user
    }

    fun signOut() {
        _currentUser.value = null
    }

    fun updateUser(user: User) {
        _currentUser.value = user
    }
}

/** Global "hide sensitive amounts" toggle (loan amounts, income) — an in-memory-only
 * preference, consistent with the rest of the app not persisting state across launches. */
class VisibilitySettings {
    private val _amountsVisible = MutableStateFlow(true)
    val amountsVisible: StateFlow<Boolean> = _amountsVisible

    fun setVisible(value: Boolean) {
        _amountsVisible.value = value
    }
}

// Simple hand-rolled service locator — this is a small, single-module app so
// a full DI framework (Hilt) would be overhead the coursework doesn't need.
object AppContainer {
    val session = SessionManager()
    val visibility = VisibilitySettings()
    val authRepository: AuthRepository = FakeAuthRepository()
    val applicationRepository: ApplicationRepository = FakeApplicationRepository()
    val productRepository: ProductRepository = FakeProductRepository()
    val profileRepository: ProfileRepository = FakeProfileRepository()
    val notificationRepository: NotificationRepository = FakeNotificationRepository()
}
