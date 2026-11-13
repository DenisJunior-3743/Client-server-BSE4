package com.trustbank.loanapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.update.UpdateStatus
import com.trustbank.loanapp.update.UpdateUiState

/**
 * Shown whenever a Reviden Store release is available. Per integration.md §2:
 * a `updateRequired` release can't be dismissed (no dismiss button, and
 * back-press/outside-tap is a no-op), an optional one can be postponed.
 */
@Composable
fun UpdateDialog(
    uiState: UpdateUiState,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val response = uiState.response ?: return
    val release = response.release ?: return
    if (uiState.status != UpdateStatus.AVAILABLE && uiState.status != UpdateStatus.DOWNLOADING) return

    val isDownloading = uiState.status == UpdateStatus.DOWNLOADING

    AlertDialog(
        onDismissRequest = { if (!response.updateRequired && !isDownloading) onDismiss() },
        title = { Text(if (response.updateRequired) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Version ${release.versionName ?: release.versionCode?.toString() ?: ""} is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Neutral800,
                )
                if (!release.releaseNotes.isNullOrBlank()) {
                    Text(release.releaseNotes, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
                }
                if (response.updateRequired) {
                    Text(
                        "This update is required to keep using the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Danger600,
                    )
                }
                if (uiState.errorMessage != null) {
                    Text(uiState.errorMessage, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger600)
                }
                if (isDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.Primary600)
                        Text("Downloading and verifying…", style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick, enabled = !isDownloading) {
                Text(if (isDownloading) "Downloading…" else "Update now")
            }
        },
        dismissButton = if (!response.updateRequired) {
            { TextButton(onClick = onDismiss, enabled = !isDownloading) { Text("Later") } }
        } else {
            null
        },
    )
}
