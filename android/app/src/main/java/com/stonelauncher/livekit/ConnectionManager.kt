package com.stonelauncher.livekit

import android.content.Context
import android.util.Log
import com.stonelauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Manages LiveKit connections with automatic retry logic.
 *
 * TICKET_014: Cloud Agent Integration
 *
 * Responsibilities:
 * - Fetch connection tokens from cloud server
 * - Retry on transient failures
 * - Handle network errors gracefully
 * - Provide meaningful error messages
 */
class ConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "ConnectionManager"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 2000L
        private const val MAX_RETRY_DELAY_MS = 10000L
    }

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)

        // Add logging in debug builds
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    /**
     * Connection details returned from token server.
     */
    data class ConnectionDetails(
        val url: String,
        val token: String,
        val roomName: String
    )

    /**
     * Fetch connection details from the token server with retry logic.
     *
     * @param participantId Unique participant identifier
     * @param roomName Room name to join
     * @param agentType Type of agent to connect to (default: "router")
     * @return ConnectionDetails on success
     * @throws ConnectionException on failure after all retries
     */
    suspend fun fetchConnectionDetails(
        participantId: String,
        roomName: String,
        agentType: String = "router"
    ): Result<ConnectionDetails> {
        var lastException: Exception? = null
        var retryDelay = INITIAL_RETRY_DELAY_MS

        repeat(MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "Fetching connection details (attempt ${attempt + 1}/$MAX_RETRIES)")

                val details = fetchConnectionDetailsInternal(participantId, roomName, agentType)

                Log.d(TAG, "Successfully fetched connection details")
                return Result.success(details)

            } catch (e: UnknownHostException) {
                Log.w(TAG, "Network unavailable (attempt ${attempt + 1}/$MAX_RETRIES)", e)
                lastException = ConnectionException(
                    "Network unavailable. Check your internet connection.",
                    ConnectionErrorCode.NETWORK_UNAVAILABLE,
                    e
                )
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Request timeout (attempt ${attempt + 1}/$MAX_RETRIES)", e)
                lastException = ConnectionException(
                    "Server request timed out. Please try again.",
                    ConnectionErrorCode.TIMEOUT,
                    e
                )
            } catch (e: IOException) {
                Log.w(TAG, "Network error (attempt ${attempt + 1}/$MAX_RETRIES)", e)
                lastException = ConnectionException(
                    "Network error: ${e.message}",
                    ConnectionErrorCode.NETWORK_ERROR,
                    e
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error (attempt ${attempt + 1}/$MAX_RETRIES)", e)
                lastException = ConnectionException(
                    "Connection failed: ${e.message}",
                    ConnectionErrorCode.UNKNOWN,
                    e
                )
            }

            // Don't delay after the last attempt
            if (attempt < MAX_RETRIES - 1) {
                Log.d(TAG, "Retrying in ${retryDelay}ms...")
                delay(retryDelay)
                // Exponential backoff with max limit
                retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
            }
        }

        Log.e(TAG, "All connection attempts failed")
        return Result.failure(
            lastException ?: ConnectionException(
                "Connection failed after $MAX_RETRIES attempts",
                ConnectionErrorCode.MAX_RETRIES_EXCEEDED
            )
        )
    }

    /**
     * Internal method to fetch connection details (single attempt).
     */
    private suspend fun fetchConnectionDetailsInternal(
        participantId: String,
        roomName: String,
        agentType: String
    ): ConnectionDetails = withContext(Dispatchers.IO) {
        val serverUrl = BuildConfig.TOKEN_SERVER_URL
        val url = "$serverUrl/api/connection-details" +
                "?participantId=$participantId" +
                "&roomName=$roomName" +
                "&agentType=$agentType"

        Log.d(TAG, "Requesting connection details from: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned ${response.code}: ${response.message}")
            }

            val body = response.body?.string()
                ?: throw IOException("Empty response from server")

            val json = JSONObject(body)

            // Parse response (server returns: serverUrl, participantToken, roomName, participantName)
            val livekitUrl = json.getString("serverUrl")
            val token = json.getString("participantToken")
            val responseName = json.getString("roomName")

            return@withContext ConnectionDetails(
                url = livekitUrl,
                token = token,
                roomName = responseName
            )
        }
    }

    /**
     * Check if the token server is reachable.
     *
     * @return true if server responds to health check
     */
    suspend fun checkServerHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverUrl = BuildConfig.TOKEN_SERVER_URL
            val url = "$serverUrl/health"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed", e)
            false
        }
    }
}

/**
 * Exception thrown when connection fails.
 */
class ConnectionException(
    message: String,
    val errorCode: ConnectionErrorCode,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Error codes for connection failures.
 */
enum class ConnectionErrorCode {
    NETWORK_UNAVAILABLE,
    TIMEOUT,
    NETWORK_ERROR,
    SERVER_ERROR,
    INVALID_RESPONSE,
    MAX_RETRIES_EXCEEDED,
    UNKNOWN
}
