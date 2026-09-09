package com.trustbank.loanapp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.NameField
import com.trustbank.loanapp.ui.components.PasswordField
import com.trustbank.loanapp.ui.components.PhoneField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val fullName = rememberFieldState {
        when {
            !Validators.isRequired(it) -> "Full name is required"
            !Validators.isValidFullName(it) -> "Enter your full name (at least 2 letters)"
            else -> null
        }
    }
    val email = rememberFieldState {
        when {
            !Validators.isRequired(it) -> "Email is required"
            !Validators.isValidEmail(it) -> "Enter a valid email address"
            else -> null
        }
    }
    val phone = rememberFieldState {
        when {
            !Validators.isRequired(it) -> "Phone number is required"
            !Validators.isValidUgandaPhone(it) -> Validators.PHONE_ERROR_MESSAGE
            else -> null
        }
    }
    val password = rememberFieldState {
        when {
            !Validators.isRequired(it) -> "Password is required"
            !Validators.isStrongPassword(it) -> Validators.PASSWORD_ERROR_MESSAGE
            else -> null
        }
    }
    val confirmPassword = rememberFieldState { if (it != password.value) "Passwords do not match" else null }

    fun submit() {
        val allValid = listOf(
            fullName.validateForSubmit(),
            email.validateForSubmit(),
            phone.validateForSubmit(),
            password.validateForSubmit(),
            confirmPassword.validateForSubmit(),
        ).all { it }
        if (allValid) {
            viewModel.register(fullName.value, email.value, phone.value, password.value, onRegisterSuccess)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Neutral50)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Primary600)
                .padding(top = 56.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
        ) {
            Text("Create your account", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                "Apply for a loan and track it from your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            NameField(
                label = "Full name",
                value = fullName.value,
                onValueChange = fullName::onValueChange,
                onFocusLost = fullName::onFocusLost,
                required = true,
                error = fullName.error,
                placeholder = "e.g. Allan Tumusiime",
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                label = "Email address",
                value = email.value,
                onValueChange = email::onValueChange,
                onFocusLost = email::onFocusLost,
                required = true,
                error = email.error,
                placeholder = "you@mail.com",
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(14.dp))
            PhoneField(
                value = phone.value,
                onValueChange = phone::onValueChange,
                onFocusLost = phone::onFocusLost,
                error = phone.error,
            )
            Spacer(Modifier.height(14.dp))
            PasswordField(
                label = "Password",
                value = password.value,
                onValueChange = { v ->
                    password.onValueChange(v)
                    confirmPassword.revalidate()
                },
                onFocusLost = password::onFocusLost,
                required = true,
                error = password.error,
                showStrengthMeter = true,
            )
            Spacer(Modifier.height(14.dp))
            PasswordField(
                label = "Confirm password",
                value = confirmPassword.value,
                onValueChange = confirmPassword::onValueChange,
                onFocusLost = confirmPassword::onFocusLost,
                required = true,
                error = confirmPassword.error,
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(uiState.error ?: "", color = AppColors.Danger600, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Create account",
                isLoading = uiState.isLoading,
                onClick = ::submit,
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("Already have an account? ", color = AppColors.Neutral500, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Sign in",
                    color = AppColors.Primary600,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin),
                )
            }
        }
    }
}
