package com.trustbank.loanapp.ui.screens.auth

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DigitsField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

/**
 * Phone verification with a FIRM deadline. The code is only worth something until its expiry:
 * the countdown shows the borrower how long is left, and once it reaches zero the Verify button
 * is disabled. This is the client mirroring the rule to save a pointless round trip; the server
 * enforces it regardless (a late code gets HTTP 410 even if it is the right one).
 */
@Composable
fun VerifyPhoneScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    SetStatusBarAppearance(darkIcons = true)

    val uiState by viewModel.uiState.collectAsState()
    var pending by remember { mutableStateOf(AppContainer.pendingVerification) }
    var code by remember { mutableStateOf(AppContainer.pendingVerification?.devCode ?: "") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }

    val expiresAt = pending?.expiresAt
    val secondsLeft = if (expiresAt == null) 0L else Duration.between(now, expiresAt).seconds
    val expired = secondsLeft <= 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(title = "Verify your phone", onBack = onBack)

        Text(
            "We sent a 6-digit code to your phone number. Enter it below to finish creating your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Neutral700,
        )

        DigitsField(
            label = "Verification code",
            value = code,
            onValueChange = { code = it },
            maxLength = 6,
            placeholder = "6 digits",
        )

        if (expired) {
            Text(
                "This code has expired. Request a new one.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Danger600,
            )
        } else {
            Text(
                "Code expires in ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = if (secondsLeft <= 60L) AppColors.Warning600 else AppColors.Neutral500,
            )
        }

        if (pending?.devCode != null) {
            Text(
                "Development server: the code was filled in for you.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral500,
            )
        }

        val error = uiState.error
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger600)
        }

        PrimaryButton(
            text = "Verify and continue",
            enabled = code.length == 6 && !expired,
            isLoading = uiState.isLoading,
            onClick = { viewModel.verifyPhone(code, onVerified) },
        )

        SecondaryButton(
            text = "Send a new code",
            enabled = !uiState.isLoading,
            onClick = {
                viewModel.resendCode {
                    pending = AppContainer.pendingVerification
                    code = AppContainer.pendingVerification?.devCode ?: ""
                    now = LocalDateTime.now()
                }
            },
        )
    }
}
