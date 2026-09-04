package com.trustbank.loanapp.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ApplicationDetailUiState(
    val isLoading: Boolean = true,
    val application: LoanApplication? = null,
    val documents: List<LoanDocument> = emptyList(),
    val isResponding: Boolean = false,
    val responseSent: Boolean = false,
)

class ApplicationDetailViewModel(
    private val applicationRepository: ApplicationRepository = AppContainer.applicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationDetailUiState())
    val uiState: StateFlow<ApplicationDetailUiState> = _uiState

    fun load(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val application = applicationRepository.getApplication(applicationId)
            val documents = applicationRepository.getDocuments(applicationId)
            _uiState.value = ApplicationDetailUiState(isLoading = false, application = application, documents = documents)
        }
    }

    // The screen already validated the response message live before calling this.
    fun respondToInfoRequest(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResponding = true)
            val updated = applicationRepository.respondToInfoRequest(applicationId)
            _uiState.value = _uiState.value.copy(isResponding = false, application = updated, responseSent = true)
        }
    }
}
