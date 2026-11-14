package com.trustbank.loanapp.ui.components

import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.util.readDocumentBytes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.trustbank.loanapp.data.model.DocumentUpload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** System document picker requires no broad storage permission and supports cloud providers. */
@Composable
fun DocumentPickerButton(label: String, enabled: Boolean = true, selected: DocumentUpload? = null,
        onSelected: (DocumentUpload) -> Unit, onError: (String) -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<DocumentUpload?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            reading = true
            try {
                val upload = withContext(Dispatchers.IO) {
                    var name = "document"
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
                    }
                    val type = resolver.getType(uri).orEmpty()
                    require(type in setOf("application/pdf", "image/jpeg", "image/png")) { "Choose a PDF, JPEG or PNG file" }
                    val bytes = resolver.openInputStream(uri)?.use { input ->
                        readDocumentBytes(input)
                    } ?: error("Cannot read the selected file")
                    DocumentUpload(name, type, bytes)
                }
                onSelected(upload)
                picked = upload
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Cannot read the selected file")
            } finally { reading = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SecondaryButton(text = if (reading) "Reading file…" else label,
            enabled = enabled && !reading,
            onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) })
        (selected ?: picked)?.let { SelectedDocumentPreview(it) }
    }
}
