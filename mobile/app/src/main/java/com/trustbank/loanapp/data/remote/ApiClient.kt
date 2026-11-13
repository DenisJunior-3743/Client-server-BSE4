package com.trustbank.loanapp.data.remote

import com.google.gson.Gson
import com.trustbank.loanapp.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** A failure the UI can show. [code] is the server's stable error code; "NETWORK" means no reply at all. */
class ApiException(val status: Int, val code: String, message: String) : Exception(message) {
    /** True when the request may be sent again as-is (nothing reached us, or the server was busy). */
    val isTransient: Boolean get() = code == "NETWORK" || status == 503
}

/**
 * Holds the JWT for this app run. The server is stateless: it keeps no session for us, so
 * every request must carry this token, and logging out is simply forgetting it.
 * In memory only, like the rest of the app's state.
 */
object TokenStore {
    @Volatile
    var accessToken: String? = null

    fun clear() {
        accessToken = null
    }
}

object IdempotencyKeys {
    /** One key per user action. Reuse it for every retry of that action; never for a different one. */
    fun newKey(): String = UUID.randomUUID().toString()
}

object ApiClient {
    /** Empty means "no backend configured": AppContainer then uses the in-memory fakes. */
    val isConfigured: Boolean get() = BuildConfig.API_BASE_URL.isNotBlank()

    private val gson = Gson()

    private val authInterceptor = Interceptor { chain ->
        val token = TokenStore.accessToken
        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        chain.proceed(request)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            // Bounded waits on the client side too: a dead connection fails instead of hanging the screen.
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val api: LoanApi by lazy {
        val baseUrl = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoanApi::class.java)
    }

    /** Runs one API call and turns every failure into an [ApiException] with a message fit for the UI. */
    suspend fun <T> call(block: suspend LoanApi.() -> T): T {
        try {
            return api.block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw toApiException(e)
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Cannot reach the server. Check your connection and try again.")
        }
    }

    /**
     * Retransmits a request that is SAFE to repeat: a GET/PUT, or a POST that carries an
     * Idempotency-Key. On a lost connection we cannot know whether the server did the work
     * before the reply was lost, and that is exactly the case the key exists for: the same
     * key goes out again and the server either does the work now or replays its first reply.
     */
    suspend fun <T> callWithRetry(attempts: Int = 3, block: suspend LoanApi.() -> T): T {
        var lastError: ApiException? = null
        repeat(attempts) { attempt ->
            try {
                return call(block)
            } catch (e: ApiException) {
                if (!e.isTransient) throw e
                lastError = e
            }
            if (attempt < attempts - 1) delay(500L * (attempt + 1))
        }
        throw lastError ?: ApiException(0, "NETWORK", "Cannot reach the server.")
    }

    private fun toApiException(e: HttpException): ApiException {
        val parsed = try {
            e.response()?.errorBody()?.string()?.let { gson.fromJson(it, ApiErrorDto::class.java) }
        } catch (_: Exception) {
            null
        }
        if (e.code() == 401) TokenStore.clear()
        val message = parsed?.fieldErrors?.values?.firstOrNull()
            ?: parsed?.message
            ?: "Request failed (${e.code()})"
        return ApiException(e.code(), parsed?.code ?: "ERROR", message)
    }
}
