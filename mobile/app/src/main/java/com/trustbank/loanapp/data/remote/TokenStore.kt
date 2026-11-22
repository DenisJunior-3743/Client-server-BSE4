package com.trustbank.loanapp.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import com.trustbank.loanapp.BuildConfig
import com.trustbank.loanapp.data.model.User
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private data class SavedSessionDto(val accessToken: String, val expiresAt: Long, val user: UserDto)

object TokenStore {
    @Volatile
    var accessToken: String? = null
        private set

    private var disk: RememberedSessionStore? = null
    private var remembered: SavedSessionDto? = null

    @Synchronized
    fun restore(context: Context): User? {
        if (disk != null) return null
        val storage = RememberedSessionStore(context)
        disk = storage
        val saved = storage.read()
        val user = runCatching {
            saved?.takeIf { it.expiresAt > System.currentTimeMillis() && !it.accessToken.isNullOrBlank() }
                ?.user?.takeIf { it.role == "BORROWER" }?.toModel()
        }.getOrNull()
        if (user == null) {
            storage.clear()
            return null
        }

        remembered = saved
        accessToken = saved?.accessToken
        return user
    }

    @Synchronized
    fun save(reply: LoginResponseDto, rememberMe: Boolean) {
        clear()
        accessToken = reply.accessToken
        if (rememberMe && reply.expiresIn > 0) {
            val saved = SavedSessionDto(reply.accessToken, System.currentTimeMillis() + reply.expiresIn * 1000, reply.user)
            if (disk?.write(saved) == true) remembered = saved
        }
    }

    @Synchronized
    fun updateUser(user: User) {
        val saved = remembered?.takeIf { it.user.id == user.id } ?: return
        val next = saved.copy(user = saved.user.copy(fullName = user.fullName, email = user.email, phone = user.phone))
        if (disk?.write(next) == true) remembered = next
    }

    @Synchronized
    fun clearIfMatches(token: String): Boolean {
        if (accessToken != token) return false
        clear()
        return true
    }

    @Synchronized
    fun clear() {
        accessToken = null
        remembered = null
        disk?.clear()
    }
}

private class RememberedSessionStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "remembered-session"))
    private val gson = Gson()

    fun read(): SavedSessionDto? = runCatching {
        val encoded = file.readFully()
        require(encoded.size > NONCE_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encoded, 0, NONCE_BYTES))
        cipher.updateAAD(BuildConfig.API_BASE_URL.toByteArray(Charsets.UTF_8))
        val json = cipher.doFinal(encoded, NONCE_BYTES, encoded.size - NONCE_BYTES)
        gson.fromJson(json.toString(Charsets.UTF_8), SavedSessionDto::class.java)
    }.getOrNull()

    fun write(session: SavedSessionDto): Boolean = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        check(cipher.iv.size == NONCE_BYTES)
        cipher.updateAAD(BuildConfig.API_BASE_URL.toByteArray(Charsets.UTF_8))
        val encoded = cipher.iv + cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try {
            output.write(encoded)
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }.onFailure { Log.w("TrustBankSession", "Could not save the remembered sign-in") }.isSuccess

    fun clear() = file.delete()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }

    companion object {
        private const val KEY_ALIAS = "trustbank-remembered-session"
        private const val NONCE_BYTES = 12
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
