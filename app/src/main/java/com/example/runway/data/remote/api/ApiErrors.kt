package com.example.runway.data.remote.api

import android.util.Log
import androidx.annotation.StringRes
import com.example.runway.R
import kotlinx.serialization.json.Json
import java.io.IOException
import retrofit2.HttpException
import java.net.SocketTimeoutException

private const val TAG = "ApiErrors"

private val json = Json { ignoreUnknownKeys = true }

// Only wardrobe items queue in Room, so only those callers may promise a later sync.
// Returns a string resource so the screen can show it in the user's language.
@StringRes
fun Throwable.toUserMessage(queuesOffline: Boolean = false): Int = when (this) {
    is SocketTimeoutException -> R.string.rw_error_timeout
    is IOException -> if (queuesOffline) {
        R.string.rw_error_offline_queued
    } else {
        R.string.rw_error_offline
    }
    is HttpException -> {
        // The server only answers in English, so its message goes to the log and the user gets ours.
        serverMessage()?.let { Log.w(TAG, "Server said: $it") }
        statusMessage(code())
    }
    else -> R.string.rw_error_generic
}

private fun HttpException.serverMessage(): String? {
    val raw = response()?.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return null
    return runCatching { json.decodeFromString<ApiErrorDto>(raw) }
        .getOrNull()?.message?.takeIf { it.isNotBlank() }
}

// Umbraco (n.d.)
@StringRes
private fun statusMessage(code: Int): Int = when (code) {
    400 -> R.string.rw_error_bad_request
    401 -> R.string.rw_error_session_expired
    404 -> R.string.rw_error_not_found
    in 500..599 -> R.string.rw_error_server
    else -> R.string.rw_error_generic
}

// References:
// 1. Umbraco. n.d. What are HTTP status codes? [Online]. Available at: https://umbraco.com/knowledge-base/http-status-codes/ [Accessed 11 September 2026].
