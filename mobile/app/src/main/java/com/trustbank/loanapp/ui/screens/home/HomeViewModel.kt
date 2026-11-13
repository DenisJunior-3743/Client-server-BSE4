package com.trustbank.loanapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val applications: List<LoanApplication> = emptyList(),
)

class HomeViewModel(
    private val applicationRepository: ApplicationRepository = AppContainer.applicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val applications = applicationRepository.listApplications()
            _uiState.value = HomeUiState(isLoading = false, applications = applications)
        }
    }
}
