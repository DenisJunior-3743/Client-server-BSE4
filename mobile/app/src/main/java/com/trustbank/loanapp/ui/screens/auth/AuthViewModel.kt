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

    fun register(fullName: String, email: String, phone: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.register(fullName, email, phone, password)
                .onSuccess { user ->
                    AppContainer.session.signIn(user)
                    _uiState.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState(error = err.message ?: "Unable to register")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
