package com.trustbank.loanapp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.PasswordField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Validators

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val email = rememberFieldState {
        when {
            !Validators.isRequired(it) -> "Email is required"
            !Validators.isValidEmail(it) -> "Enter a valid email address"
            else -> null
        }
    }
    val password = rememberFieldState { if (!Validators.isRequired(it)) "Password is required" else null }

    fun submit() {
        val emailValid = email.validateForSubmit()
        val passwordValid = password.validateForSubmit()
        if (emailValid && passwordValid) {
            viewModel.login(email.value, password.value, onLoginSuccess)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Neutral50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(AppColors.Primary600, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color.White)
        }

        Spacer(Modifier.height(20.dp))
        Text("TrustBank", style = MaterialTheme.typography.headlineMedium, color = AppColors.Neutral900)
        Text("Welcome back. Sign in to continue.", style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral500)

        Spacer(Modifier.height(28.dp))

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
        PasswordField(
            label = "Password",
            value = password.value,
            onValueChange = password::onValueChange,
            onFocusLost = password::onFocusLost,
            required = true,
            error = password.error,
        )

        if (uiState.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(uiState.error ?: "", color = AppColors.Danger600, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Sign in",
            isLoading = uiState.isLoading,
            onClick = ::submit,
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? ", color = AppColors.Neutral500, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Register",
                color = AppColors.Primary600,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onNavigateToRegister),
            )
        }
    }
}
