package com.trustbank.loanapp.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ApplicationsUiState(
    val isLoading: Boolean = true,
    val applications: List<LoanApplication> = emptyList(),
)

class ApplicationsViewModel(
    private val applicationRepository: ApplicationRepository = AppContainer.applicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationsUiState())
    val uiState: StateFlow<ApplicationsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = ApplicationsUiState(isLoading = false, applications = applicationRepository.listApplications())
        }
    }
}
