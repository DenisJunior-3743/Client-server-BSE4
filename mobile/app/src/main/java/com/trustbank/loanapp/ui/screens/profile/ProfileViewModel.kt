package com.trustbank.loanapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.ApplicantProfile
import com.trustbank.loanapp.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val profile: ApplicantProfile? = null,
    val savedMessage: String? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository = AppContainer.profileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            _uiState.value = ProfileUiState(isLoading = false, profile = profile)
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true, savedMessage = null)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    // The edit form only calls this once every field has already passed its
    // own live validation, so this just persists the already-valid profile.
    fun save(profile: ApplicantProfile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val updated = profileRepository.updateProfile(profile)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                isEditing = false,
                profile = updated,
                savedMessage = "Profile updated.",
            )
        }
    }
}
