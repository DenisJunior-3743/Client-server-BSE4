package com.trustbank.loanapp.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.AppNotification
import com.trustbank.loanapp.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<AppNotification> = emptyList(),
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = AppContainer.notificationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = NotificationsUiState(isLoading = false, notifications = notificationRepository.listNotifications())
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markRead(id)
            load()
        }
    }
}
