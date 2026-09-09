package com.trustbank.loanapp.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.ApplicantProfile
import com.trustbank.loanapp.data.model.EmploymentType
import com.trustbank.loanapp.ui.common.employmentTypeLabel
import com.trustbank.loanapp.ui.common.hexToColor
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppDropdownField
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.AvatarInitials
import com.trustbank.loanapp.ui.components.DateField
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.DigitsField
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.NameField
import com.trustbank.loanapp.ui.components.NationalIdField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters
import com.trustbank.loanapp.util.Validators
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by AppContainer.session.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("My Profile", style = MaterialTheme.typography.headlineSmall, color = AppColors.Neutral900)
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = AppColors.Neutral700)
            }
        }

        val user = currentUser
        if (user != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AvatarInitials(name = user.fullName, color = hexToColor(user.avatarColorHex), size = 56)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.fullName, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
                }
            }
        }

        val profile = uiState.profile
        when {
            uiState.isLoading || profile == null -> LoadingIndicator(modifier = Modifier.height(200.dp))
            uiState.isEditing -> ProfileEditForm(
                initial = profile,
                isSaving = uiState.isSaving,
                onCancel = viewModel::cancelEditing,
                onSave = viewModel::save,
            )
            else -> ProfileView(profile = profile, savedMessage = uiState.savedMessage, onEdit = viewModel::startEditing)
        }
    }
}

@Composable
private fun ProfileView(profile: ApplicantProfile, savedMessage: String?, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (savedMessage != null) {
            Text(savedMessage, color = AppColors.Success600, style = MaterialTheme.typography.bodySmall)
        }
        SectionCard(title = "KYC Details") {
            DetailRow("National ID", profile.nationalId)
            DetailRow("Date of birth", Formatters.date(profile.dateOfBirth))
            DetailRow("Employer", profile.employer)
            DetailRow("Job title", profile.jobTitle)
            DetailRow("Employment type", employmentTypeLabel(profile.employmentType))
            DetailRow("Monthly income", Formatters.ugx(profile.incomeMonthly))
            DetailRow("District", profile.district)
            DetailRow("Address", profile.address)
        }
        SecondaryButton(text = "Edit profile", onClick = onEdit)
    }
}

@Composable
private fun ProfileEditForm(
    initial: ApplicantProfile,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: (ApplicantProfile) -> Unit,
) {
    val nationalId = rememberFieldState(initial.nationalId) {
        when {
            !Validators.isRequired(it) -> "National ID is required"
            !Validators.isValidNationalId(it) -> Validators.NATIONAL_ID_ERROR_MESSAGE
            else -> null
        }
    }
    val dateOfBirthText = rememberFieldState(initial.dateOfBirth.toString()) { text ->
        if (text.isBlank()) return@rememberFieldState "Date of birth is required"
        val parsed = try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            null
        }
        when {
            parsed == null -> "Use format YYYY-MM-DD"
            !Validators.isAdult(parsed) -> "You must be at least 18 years old"
            else -> null
        }
    }
    val employer = rememberFieldState(initial.employer) { if (!Validators.isRequired(it)) "Employer is required" else null }
    val jobTitle = rememberFieldState(initial.jobTitle) { if (!Validators.isRequired(it)) "Job title is required" else null }
    var employmentType by remember { mutableStateOf(initial.employmentType) }
    val incomeText = rememberFieldState(initial.incomeMonthly.toInt().toString()) { text ->
        val amount = text.toDoubleOrNull()
        if (amount == null || amount <= 0) "Enter a valid monthly income" else null
    }
    val district = rememberFieldState(initial.district) { if (!Validators.isRequired(it)) "District is required" else null }
    val address = rememberFieldState(initial.address) { if (!Validators.isRequired(it)) "Address is required" else null }

    fun submit() {
        val allValid = listOf(
            nationalId.validateForSubmit(),
            dateOfBirthText.validateForSubmit(),
            employer.validateForSubmit(),
            jobTitle.validateForSubmit(),
            incomeText.validateForSubmit(),
            district.validateForSubmit(),
            address.validateForSubmit(),
        ).all { it }
        if (!allValid) return

        onSave(
            ApplicantProfile(
                userId = initial.userId,
                nationalId = nationalId.value.trim(),
                dateOfBirth = LocalDate.parse(dateOfBirthText.value),
                employer = employer.value.trim(),
                jobTitle = jobTitle.value.trim(),
                employmentType = employmentType,
                incomeMonthly = incomeText.value.toDouble(),
                district = district.value.trim(),
                address = address.value.trim(),
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NationalIdField(
            value = nationalId.value,
            onValueChange = nationalId::onValueChange,
            onFocusLost = nationalId::onFocusLost,
            required = true,
            error = nationalId.error,
        )
        DateField(
            label = "Date of birth",
            value = dateOfBirthText.value,
            onValueChange = dateOfBirthText::onValueChange,
            required = true,
            error = dateOfBirthText.error,
        )
        AppTextField(
            label = "Employer",
            value = employer.value,
            onValueChange = employer::onValueChange,
            onFocusLost = employer::onFocusLost,
            required = true,
            error = employer.error,
        )
        AppTextField(
            label = "Job title",
            value = jobTitle.value,
            onValueChange = jobTitle::onValueChange,
            onFocusLost = jobTitle::onFocusLost,
            required = true,
            error = jobTitle.error,
        )
        AppDropdownField(
            label = "Employment type",
            options = EmploymentType.entries,
            selected = employmentType,
            optionLabel = ::employmentTypeLabel,
            onSelect = { employmentType = it },
            required = true,
        )
        DigitsField(
            label = "Monthly income (UGX)",
            value = incomeText.value,
            onValueChange = incomeText::onValueChange,
            onFocusLost = incomeText::onFocusLost,
            required = true,
            error = incomeText.error,
        )
        NameField(
            label = "District",
            value = district.value,
            onValueChange = district::onValueChange,
            onFocusLost = district::onFocusLost,
            required = true,
            error = district.error,
        )
        AppTextField(
            label = "Address",
            value = address.value,
            onValueChange = address::onValueChange,
            onFocusLost = address::onFocusLost,
            required = true,
            error = address.error,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Save", isLoading = isSaving, onClick = ::submit, modifier = Modifier.weight(1f))
        }
    }
}
