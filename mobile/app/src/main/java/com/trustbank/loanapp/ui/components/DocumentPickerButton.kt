package com.trustbank.loanapp.ui.components

import android.provider.OpenableColumns
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
fun DocumentPickerButton(label: String, enabled: Boolean = true,
        onSelected: (DocumentUpload) -> Unit, onError: (String) -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
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
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 5_242_880) { "Files must be under 5MB" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("Cannot read the selected file")
                    require(bytes.isNotEmpty()) { "Choose a non-empty file" }
                    DocumentUpload(name, type, bytes)
                }
                onSelected(upload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Cannot read the selected file")
            } finally { reading = false }
        }
    }
    SecondaryButton(text = if (reading) "Reading file…" else label,
        enabled = enabled && !reading,
        onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) })
}
