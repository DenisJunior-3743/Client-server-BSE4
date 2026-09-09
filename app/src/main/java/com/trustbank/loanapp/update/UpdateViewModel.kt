package com.trustbank.loanapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val REVIDEN_BASE_URL = "https://reviden-store.onrender.com"

enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, ERROR }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val response: UpdateCheckResponse? = null,
    val errorMessage: String? = null,
)

/** The installed APK's own versionCode — what Reviden Store compares its latest release against. */
fun installedVersionCode(context: Context): Int {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(info).toInt()
}

class UpdateViewModel(
    private val client: AppUpdateClient = AppUpdateClient(baseUrl = REVIDEN_BASE_URL, packageName = BuildConfig.APPLICATION_ID),
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState

    fun checkForUpdate(context: Context) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState(status = UpdateStatus.CHECKING)
            try {
                val response = client.checkForUpdate(installedVersionCode(context))
                _uiState.value = UpdateUiState(
                    status = if (response.updateAvailable) UpdateStatus.AVAILABLE else UpdateStatus.UP_TO_DATE,
                    response = response,
                )
            } catch (e: Exception) {
                _uiState.value = UpdateUiState(status = UpdateStatus.ERROR, errorMessage = e.message ?: "Unable to check for updates")
            }
        }
    }

    fun downloadAndInstall(context: Context) {
        val release = _uiState.value.response?.release ?: return
        val downloadUrl = release.downloadUrl ?: return
        val sha256 = release.sha256 ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = UpdateStatus.DOWNLOADING, errorMessage = null)
            try {
                val destination = File(File(context.cacheDir, "updates"), "update-${release.versionCode}.apk")
                val verified = client.downloadAndVerify(downloadUrl, sha256, destination)
                if (!verified) {
                    destination.delete()
                    _uiState.value = _uiState.value.copy(
                        status = UpdateStatus.AVAILABLE,
                        errorMessage = "Downloaded file didn't match the expected checksum — please try again.",
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(status = UpdateStatus.AVAILABLE)
                launchInstall(context, destination)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(status = UpdateStatus.AVAILABLE, errorMessage = e.message ?: "Unable to download the update")
            }
        }
    }

    private fun launchInstall(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** No-op once a required update is pending — integration.md: a forced update can't be dismissed. */
    fun dismiss() {
        if (_uiState.value.response?.updateRequired == true) return
        _uiState.value = UpdateUiState()
    }
}
