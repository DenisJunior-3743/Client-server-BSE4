package com.trustbank.loanapp.update

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** A Reviden Store release, embedded in the update-check response (integration.md §2). */
data class ReleaseInfo(
    val id: String? = null,
    val versionCode: Int? = null,
    val versionName: String? = null,
    val updateType: String? = null,
    val releaseNotes: String? = null,
    val sha256: String? = null,
    val fileSize: Long? = null,
    val downloadUrl: String? = null,
)

/** The `GET /api/updates/latest` response shape (integration.md §2), deserialized by Gson field-for-field. */
data class UpdateCheckResponse(
    val updateAvailable: Boolean = false,
    val updateRequired: Boolean = false,
    val currentVersionCode: Int? = null,
    val release: ReleaseInfo? = null,
    val deviceStatus: String? = null,
)

/**
 * Plain-HTTP client for Reviden Store's update-check/download endpoints
 * (`integration.md`). Nothing here is Reviden-specific beyond the two URLs —
 * nothing to embed, no SDK, no API key.
 */
class AppUpdateClient(
    private val baseUrl: String,
    private val packageName: String,
    private val channel: String = "production",
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/updates/latest".toHttpUrl().newBuilder()
            .addQueryParameter("packageName", packageName)
            .addQueryParameter("versionCode", currentVersionCode.toString())
            .addQueryParameter("channel", channel)
            .build()

        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Update check failed: HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            gson.fromJson(body, UpdateCheckResponse::class.java)
        }
    }

    /** Downloads to [destination] and verifies its SHA-256 against [expectedSha256] (integration.md §3). Returns false — and leaves the file for the caller to delete — on a mismatch. */
    suspend fun downloadAndVerify(downloadUrl: String, expectedSha256: String, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            client.newCall(Request.Builder().url(downloadUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
                destination.outputStream().use { output ->
                    val body = response.body ?: throw IOException("Empty download response")
                    body.byteStream().copyTo(output)
                }
            }
            sha256Of(destination).equals(expectedSha256, ignoreCase = true)
        }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
