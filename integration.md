# Reviden Store — APK Integration Guide

This is the contract your Android app needs to follow to be hosted on
Reviden Store and get real, in-place update checking — the same experience
users get from apps on the Play Store, without your app ever going through
the Play Store.

You do **not** need to add any special manifest tag, embed any SDK, or
change your build process to be hosted here. All of this is plain HTTP your
app's own code makes — there's nothing Reviden-specific baked into the APK
itself.

---

## 1. Before you upload anything

1. Sign up for a Reviden Store account and create a **project** with your
   app's exact Android **package name** (its `applicationId` in
   `build.gradle`/`build.gradle.kts`). This can never be changed later —
   it's your app's real identity, so get it right the first time.
2. Every release you publish under that project must keep the **same
   package name and the same signing key** as the one before it. This
   isn't a Reviden rule — it's Android itself: the OS refuses to install an
   "update" whose package name or signing certificate doesn't match what's
   already installed. If you ever need to change your signing key, users
   will have to uninstall the old app first, which defeats the entire
   point of using an update-check flow.
3. Every release needs a **version code** that is a plain, always-increasing
   integer (`1`, `2`, `3`, …) — this is the same `versionCode` Android
   itself already requires in every APK's manifest. Reviden Store rejects
   a release whose version code isn't strictly higher than what's currently
   published on the same project + channel.

---

## 2. The update-check call

Your app calls this on its own schedule (e.g. on launch, or on a timer) —
Reviden Store never pushes anything to your app.

```
GET https://reviden-store.onrender.com/api/updates/latest
      ?packageName=com.yourcompany.yourapp
      &versionCode=<the version code currently installed>
      &channel=production
```

You can address your project either by `packageName` (shown above) or by
`projectId` (shown in your project's dashboard page) — either works
identically. `channel` is `production` or `testing`, matching whichever
channel you published the release under; omit it and it defaults to
`production`.

Optionally add `&deviceId=<a UUID you generate once and persist>` to opt
into per-device tracking/blocking — see
[the device tracking guide](/api/docs/device-tracking/download) if you want
your project's dashboard to show which installations are checking in, and
to be able to block or require approval for specific ones. Omit it entirely
and everything above works exactly as documented, unchanged.

**Response, no update needed:**
```json
{ "updateAvailable": false, "updateRequired": false, "currentVersionCode": 4 }
```

**Response, an update exists:**
```json
{
  "updateAvailable": true,
  "updateRequired": false,
  "currentVersionCode": 4,
  "release": {
    "id": "6a9f4e58e4666603c7a4c51c",
    "versionCode": 5,
    "versionName": "1.2.0",
    "updateType": "optional",
    "releaseNotes": "Bug fixes and performance improvements.",
    "sha256": "3923052aaa6cb1f885d428d38362aa1df0bec565653d34d3899a57f3e7b6ac30",
    "fileSize": 18234112,
    "downloadUrl": "https://reviden-store.onrender.com/api/updates/download/6a9f4e58e4666603c7a4c51c"
  }
}
```

`updateRequired` is `true` when either:
- you marked the release a **forced** update when publishing it, or
- the installed version code is below the release's **minimum version
  code** (a floor you set per release — use this to force-upgrade anyone
  stuck on a version too old to safely keep using, without forcing
  everyone through every single intermediate release).

Your app's job: if `updateRequired` is true, don't let the user dismiss the
update prompt. If it's `false`, show it as a normal "an update is
available" notice they can postpone.

If you sent `deviceId`, the response also carries a `"deviceStatus"` field
(`"active"`, `"pending"`, or `"blocked"`) — see the device tracking guide.
A `"blocked"` or `"pending"` device gets `updateAvailable: false` and no
`release` at all, even if a newer version exists.

---

## 3. Downloading and verifying the APK

Download the file from `release.downloadUrl`. Before invoking the installer,
**verify the SHA-256 hash matches `release.sha256`** — this confirms the
bytes you received are exactly what was published, not something corrupted
or tampered with in transit. Reject the install if it doesn't match.

```kotlin
fun sha256Of(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

## 4. Installing it

Your app needs the `REQUEST_INSTALL_PACKAGES` permission and must use
Android's own `PackageInstaller` (or a `FileProvider`-backed
`ACTION_VIEW`/`ACTION_INSTALL_PACKAGE` intent for older API levels) to hand
the downloaded file to the system installer — Reviden Store has no part in
the actual install step, that's entirely between your app and Android.

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

---

## 5. A minimal, generic reference client

This is deliberately small and framework-free — adapt it to however your
app already does networking (Retrofit, OkHttp directly, Ktor, whatever you
use elsewhere).

```kotlin
package com.yourcompany.yourapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class UpdateCheckResult(
    val updateAvailable: Boolean,
    val updateRequired: Boolean,
    val versionCode: Int?,
    val versionName: String?,
    val releaseNotes: String?,
    val downloadUrl: String?,
    val sha256: String?,
)

class AppUpdateClient(
    private val baseUrl: String,          // e.g. "https://reviden-store.onrender.com"
    private val packageName: String,      // your app's applicationId
    private val channel: String = "production",
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/updates/latest".toHttpUrlBuilder()
            .addQueryParameter("packageName", packageName)
            .addQueryParameter("versionCode", currentVersionCode.toString())
            .addQueryParameter("channel", channel)
            .build()
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val body = JSONObject(response.body?.string().orEmpty())

        val release = body.optJSONObject("release")
        UpdateCheckResult(
            updateAvailable = body.optBoolean("updateAvailable"),
            updateRequired = body.optBoolean("updateRequired"),
            versionCode = release?.optInt("versionCode"),
            versionName = release?.optString("versionName"),
            releaseNotes = release?.optString("releaseNotes"),
            downloadUrl = release?.optString("downloadUrl"),
            sha256 = release?.optString("sha256"),
        )
    }

    suspend fun downloadAndVerify(downloadUrl: String, expectedSha256: String, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            val response = client.newCall(Request.Builder().url(downloadUrl).build()).execute()
            destination.outputStream().use { output -> response.body?.byteStream()?.copyTo(output) }

            val digest = MessageDigest.getInstance("SHA-256")
            destination.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expectedSha256, ignoreCase = true)
        }

    fun launchInstall(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
```

`toHttpUrlBuilder()` above is a stand-in for however you already build query
strings (e.g. OkHttp's `HttpUrl.Builder`) — swap in whatever your codebase
already uses; nothing here depends on a specific HTTP client, only on
hitting the two plain HTTP endpoints described in §2 and §3.

---

## 6. Device tracking and blocking (optional)

Entirely opt-in — skip this section if you don't need it, and everything
in §2–§5 works exactly as documented with no `deviceId` at all.

If your project's dashboard has device tracking turned on, you can let it
show which installations are actually checking in, and block/unblock or
require-approve specific ones (an org distributing an internal app to its
own devices is the typical case — see the device tracking guide for the
full picture). Two things your app needs to do:

1. **Generate a device ID once, and persist it** — never regenerate it on
   every launch, or your project's device dashboard becomes meaningless
   (every launch would look like a brand-new install):
   ```kotlin
   object DeviceIdentity {
       private const val PREFS = "reviden_device"
       private const val KEY = "device_id"

       fun get(context: Context): String {
           val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           return prefs.getString(KEY, null) ?: java.util.UUID.randomUUID().toString().also {
               prefs.edit().putString(KEY, it).apply()
           }
       }
   }
   ```
2. **Send it** — add `&deviceId=...` to every `/api/updates/latest` call
   (§2's example client's `checkForUpdate` just needs one more
   `addQueryParameter("deviceId", deviceId)` call). Optionally, call
   `POST /api/updates/device-register` once at first launch (and again
   only if any of this metadata changes) to attach richer detail to the
   device's record in your dashboard:
   ```
   POST https://reviden-store.onrender.com/api/updates/device-register
   Content-Type: application/json

   {
     "projectId": "<your project id>",
     "deviceId": "<the persisted UUID from step 1>",
     "manufacturer": "Google",
     "model": "Pixel 8",
     "osVersion": "15",
     "label": "optional — any string your own app's login/user system wants
                Reviden Store to display alongside this device, e.g. an
                employee name or internal user ID; never a Reviden Store
                account"
   }
   ```
   Response: `{"status": "active" | "pending" | "blocked", "deviceId": "..."}`.
   A device you never explicitly register is still tracked/enforced (via
   the bare `deviceId` on `/latest`), just with less metadata attached.

A blocked or not-yet-approved device gets `deviceStatus` back from
`/latest` (see §2) instead of ever being told a newer version exists, and
gets a `403` from the download endpoints if it tries anyway — build your
UI around `deviceStatus` however makes sense for your app (e.g. a
"waiting for approval" screen for `"pending"`).

---

## 7. What Reviden Store does **not** require

- No SDK, no special manifest tag, no build-time code generation.
- No API key for the update-check/download calls above — they're public by
  design, the same way an installed app checking the Play Store for
  updates doesn't authenticate as "you."
- No specific minimum SDK version, obfuscation setting, or build flavor.

The only real technical requirement is the one Android itself already
enforces: **keep the same package name and signing key across every
release.**

---

See also: the [GitHub Actions guide](/api/docs/github-actions/download)
for automating publishing a release every time you push a tag, the
[minification guide](/api/docs/minification/download) for keeping the APK
you publish as small as possible, and the
[device tracking guide](/api/docs/device-tracking/download) for the
optional per-device monitoring/blocking/approval module.

---

Developed by Denis Junior — 0790731277 · denisjunior3743@gmail.com
