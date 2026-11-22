package com.trustbank.loanapp.data

import android.content.Context
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.User
import com.trustbank.loanapp.data.remote.RemoteApplicationRepository
import com.trustbank.loanapp.data.remote.RemoteAuthRepository
import com.trustbank.loanapp.data.remote.RemoteNotificationRepository
import com.trustbank.loanapp.data.remote.RemoteProductRepository
import com.trustbank.loanapp.data.remote.RemoteProfileRepository
import com.trustbank.loanapp.data.remote.TokenStore
import com.trustbank.loanapp.data.repository.ApplicationRepository
import com.trustbank.loanapp.data.repository.AuthRepository
import com.trustbank.loanapp.data.repository.NotificationRepository
import com.trustbank.loanapp.data.repository.ProductRepository
import com.trustbank.loanapp.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class SessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    fun restore(context: Context) {
        TokenStore.restore(context)?.let(::signIn)
    }

    fun signIn(user: User) {
        _currentUser.value = user
    }

    fun signOut() {
        _currentUser.value = null
        // Stateless server: there is no session to end over there. Logging out = dropping the token.
        TokenStore.clear()
    }

    fun updateUser(user: User) {
        if (_currentUser.value?.id != user.id) return
        _currentUser.value = user
        TokenStore.updateUser(user)
    }

    fun rejectToken(token: String) {
        if (TokenStore.clearIfMatches(token)) _currentUser.value = null
    }
}

/** In-memory toggle for hiding sensitive amounts such as loan amounts and income. */
class VisibilitySettings {
    private val _amountsVisible = MutableStateFlow(true)
    val amountsVisible: StateFlow<Boolean> = _amountsVisible

    fun setVisible(value: Boolean) {
        _amountsVisible.value = value
    }
}

/**
 * A just-registered account waiting for its one-time code. Kept in memory only, and only until
 * the code is confirmed, so the app can sign the borrower in straight after verification.
 * [expiresAt] is the code's firm deadline; [devCode] is set only by a development server.
 */
data class PendingVerification(
    val email: String,
    val password: String,
    val expiresAt: LocalDateTime,
    val devCode: String?,
)

// Simple hand-rolled service locator — this is a small, single-module app so
// a full DI framework (Hilt) would be overhead the coursework doesn't need.
object AppContainer {
    val session = SessionManager()
    val visibility = VisibilitySettings()

    @Volatile
    var pendingVerification: PendingVerification? = null

    /** Product id -> product, filled by the remote product repository so rows can show product names. */
    val productCache = ConcurrentHashMap<String, LoanProduct>()

    val authRepository: AuthRepository = RemoteAuthRepository()
    val applicationRepository: ApplicationRepository = RemoteApplicationRepository()
    val productRepository: ProductRepository = RemoteProductRepository()
    val profileRepository: ProfileRepository = RemoteProfileRepository()
    val notificationRepository: NotificationRepository = RemoteNotificationRepository()
}
