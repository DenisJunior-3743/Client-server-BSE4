package com.trustbank.loanapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.login(email, password)
                .onSuccess { user ->
                    AppContainer.session.signIn(user)
                    _uiState.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Unable to log in")
                }
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit,
        onVerificationNeeded: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.register(fullName, email, phone, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState()
                    if (authRepository.requiresPhoneVerification) {
                        // Real backend: not signed in yet. The one-time code must be confirmed first.
                        onVerificationNeeded()
                    } else {
                        AppContainer.session.signIn(user)
                        onSuccess()
                    }
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Unable to register")
                }
        }
    }

    /** Confirms the one-time code, then signs in with the credentials the borrower just registered. */
    fun verifyPhone(code: String, onSuccess: () -> Unit) {
        val pending = AppContainer.pendingVerification
        if (pending == null) {
            _uiState.value = AuthUiState(error = "Nothing to verify. Please register again.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val verified = authRepository.verifyPhone(pending.email, code)
            if (verified.isFailure) {
                _uiState.value = AuthUiState(error = verified.exceptionOrNull()?.message ?: "Could not verify the code")
                return@launch
            }
            val login = authRepository.login(pending.email, pending.password)
            val user = login.getOrNull()
            if (user == null) {
                _uiState.value = AuthUiState(error = login.exceptionOrNull()?.message ?: "Verified. Please sign in.")
                return@launch
            }
            AppContainer.pendingVerification = null
            AppContainer.session.signIn(user)
            _uiState.value = AuthUiState()
            onSuccess()
        }
    }

    fun resendCode(onSent: () -> Unit) {
        val pending = AppContainer.pendingVerification ?: return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authRepository.resendCode(pending.email)
            _uiState.value = AuthUiState(error = result.exceptionOrNull()?.message)
            if (result.isSuccess) onSent()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
