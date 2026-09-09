package com.trustbank.loanapp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.PhoneField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators

@Composable
fun SettingsScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val user by AppContainer.session.currentUser.collectAsState()
    var saved by remember { mutableStateOf(false) }

    val fullName = rememberFieldState(user?.fullName ?: "") { if (!Validators.isRequired(it)) "Full name is required" else null }
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
        user?.let {
            AppContainer.session.updateUser(it.copy(fullName = fullName.value.trim(), email = email.value.trim(), phone = phone.value))
        }
        saved = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(title = "Settings", onBack = onBack)

        SectionCard(title = "Account Details") {
            AppTextField(
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

            PrimaryButton(text = "Save changes", onClick = ::submit)
        }

        SectionCard(title = "About") {
            DetailRow("App version", "0.1.0 (Task 1)")
            DetailRow("Backend", "Not connected yet — using local demo data")
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
