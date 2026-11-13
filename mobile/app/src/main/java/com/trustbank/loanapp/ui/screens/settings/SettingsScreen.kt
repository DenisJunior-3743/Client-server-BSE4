package com.trustbank.loanapp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.BuildConfig
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.NameField
import com.trustbank.loanapp.ui.components.PhoneField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators

@Composable
fun SettingsScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    SetStatusBarAppearance(darkIcons = true)

    val user by AppContainer.session.currentUser.collectAsState()
    var saved by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fullName = rememberFieldState(user?.fullName ?: "") {
        when {
            !Validators.isRequired(it) -> "Full name is required"
            !Validators.isValidFullName(it) -> "Enter your full name (at least 2 letters)"
            else -> null
        }
    }
    val email = rememberFieldState(user?.email ?: "") {
        when {
            !Validators.isRequired(it) -> "Email is required"
            !Validators.isValidEmail(it) -> "Enter a valid email address"
            else -> null
        }
    }
    val phone = rememberFieldState(user?.phone ?: "") {
        when {
            !Validators.isRequired(it) -> "Phone number is required"
            !Validators.isValidUgandaPhone(it) -> Validators.PHONE_ERROR_MESSAGE
            else -> null
        }
    }

    fun submit() {
        val allValid = listOf(fullName.validateForSubmit(), email.validateForSubmit(), phone.validateForSubmit()).all { it }
        if (!allValid) return
        if (saving) return
        scope.launch {
            saving = true
            saved = false
            saveError = null
            try {
                AppContainer.authRepository.updateAccount(fullName.value.trim(), email.value.trim(), phone.value)
                    .onSuccess { AppContainer.session.updateUser(it); saved = true }
                    .onFailure { saveError = it.message ?: "Unable to save account changes" }
            } finally { saving = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(title = "Settings", onBack = onBack)

        SectionCard(title = "Account Details") {
            NameField(
                label = "Full name",
                value = fullName.value,
                onValueChange = { v -> fullName.onValueChange(v); saved = false },
                onFocusLost = fullName::onFocusLost,
                required = true,
                error = fullName.error,
            )
            AppTextField(
                label = "Email address",
                value = email.value,
                onValueChange = { v -> email.onValueChange(v); saved = false },
                onFocusLost = email::onFocusLost,
                required = true,
                error = email.error,
                keyboardType = KeyboardType.Email,
            )
            PhoneField(
                value = phone.value,
                onValueChange = { v -> phone.onValueChange(v); saved = false },
                onFocusLost = phone::onFocusLost,
                error = phone.error,
            )

            if (saved) {
                Text("Saved.", color = AppColors.Success600, style = MaterialTheme.typography.bodySmall)
            }

            if (saveError != null) Text(saveError.orEmpty(), color = AppColors.Danger600, style = MaterialTheme.typography.bodySmall)
            PrimaryButton(text = "Save changes", isLoading = saving, onClick = ::submit)
        }

        SectionCard(title = "About") {
            DetailRow("App version", BuildConfig.VERSION_NAME)
            DetailRow("Backend", "Online banking service")
        }

        SecondaryButton(
            text = "Log out",
            onClick = {
                AppContainer.session.signOut()
                onLoggedOut()
            },
        )
    }
}
