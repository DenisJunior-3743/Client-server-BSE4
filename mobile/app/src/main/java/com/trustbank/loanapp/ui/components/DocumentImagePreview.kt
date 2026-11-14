package com.trustbank.loanapp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trustbank.loanapp.data.model.DocumentUpload
import com.trustbank.loanapp.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.math.max

fun isPreviewImage(contentType: String?, fileName: String): Boolean {
    val type = contentType?.substringBefore(';')?.trim()?.lowercase()
    return type == "image/jpeg" || type == "image/png" ||
        (type.isNullOrBlank() && fileName.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png"))
}

private sealed interface PreviewState {
    data object Loading : PreviewState
    data class Ready(val image: ImageBitmap) : PreviewState
    data class Failed(val message: String) : PreviewState
}

/** Private document bytes remain in memory; no public URL or disk cache is created. */
@Composable
fun SelectedDocumentPreview(upload: DocumentUpload) {
    if (!isPreviewImage(upload.contentType, upload.fileName)) {
        Text("PDF: ${upload.fileName}", style = MaterialTheme.typography.bodySmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        return
    }
    val state by produceState<PreviewState>(PreviewState.Loading, upload) {
        value = PreviewState.Loading
        value = readPreview { decodePreview(upload.bytes) }
    }
    var enlarged by remember(upload) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (val current = state) {
            PreviewState.Loading -> Text("Preparing image preview...", style = MaterialTheme.typography.bodySmall)
            is PreviewState.Failed -> Text(current.message, color = AppColors.Danger600,
                style = MaterialTheme.typography.bodySmall)
            is PreviewState.Ready -> {
                Image(current.image, contentDescription = "Preview ${upload.fileName}", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Enlarge image") { enlarged = true })
                Text("Tap image to enlarge", style = MaterialTheme.typography.bodySmall, color = AppColors.Primary600)
                if (enlarged) ImagePreviewDialog(upload.fileName, onDismiss = { enlarged = false },
                    loadBitmap = { current.image })
            }
        }
        Text(upload.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Fetch only when requested, through the authenticated repository (never an arbitrary remote URL). */
@Composable
fun RemoteImagePreviewButton(fileName: String, onLoad: suspend () -> DocumentUpload) {
    var enlarged by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        SecondaryButton(text = "Preview image", onClick = { enlarged = true })
    }
    if (enlarged) ImagePreviewDialog(fileName, onDismiss = { enlarged = false },
        loadBitmap = { decodePreview(onLoad().bytes) })
}

@Composable
private fun ImagePreviewDialog(fileName: String, onDismiss: () -> Unit, loadBitmap: suspend () -> ImageBitmap) {
    var attempt by remember { mutableIntStateOf(0) }
    val latestLoad by rememberUpdatedState(loadBitmap)
    val state by produceState<PreviewState>(PreviewState.Loading, attempt) {
        value = PreviewState.Loading
        value = readPreview { latestLoad() }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(12.dp).fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fileName, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close image preview", tint = Color.White)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (val current = state) {
                        PreviewState.Loading -> CircularProgressIndicator(color = Color.White)
                        is PreviewState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(current.message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { attempt++ }) { Text("Retry", color = Color.White) }
                        }
                        is PreviewState.Ready -> ZoomableImage(current.image, fileName)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(image: ImageBitmap, fileName: String) {
    var scale by remember(image) { mutableFloatStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }
    val gestures = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().transformable(gestures)) {
            Image(image, contentDescription = fileName, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pinch to zoom", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { scale = 1f; offset = Offset.Zero }) { Text("Reset zoom", color = Color.White) }
        }
    }
}

private suspend fun readPreview(load: suspend () -> ImageBitmap): PreviewState = try {
    PreviewState.Ready(load())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    PreviewState.Failed(e.message ?: "Cannot preview this image")
}

/** Sample before decoding to bound memory, and honor camera EXIF rotation/mirroring. */
private suspend fun decodePreview(bytes: ByteArray): ImageBitmap = withContext(Dispatchers.Default) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot preview this image. Choose a valid JPEG or PNG." }
    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)) { "Cannot decode this image" }
    val orientation = runCatching {
        ByteArrayInputStream(bytes).use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
    }
    val oriented = if (matrix.isIdentity) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (oriented !== bitmap) bitmap.recycle()
    oriented.asImageBitmap()
}
