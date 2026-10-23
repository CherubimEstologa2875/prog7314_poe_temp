package com.example.runway.data.remote.api

import kotlinx.serialization.json.Json
import java.io.IOException
import retrofit2.HttpException
import java.net.SocketTimeoutException

private val json = Json { ignoreUnknownKeys = true }

// Only wardrobe items queue in Room, so only those callers may promise a later sync.
fun Throwable.toUserMessage(queuesOffline: Boolean = false): String = when (this) {
    is SocketTimeoutException -> "The server took too long to answer. Please try again."
    is IOException -> if (queuesOffline) {
        "You are offline. Changes are saved and will sync later."
    } else {
        "You are offline. Please try again once you have a connection."
    }
    is HttpException -> serverMessage() ?: statusMessage(code())
    else -> "Something went wrong. Please try again."
}

private fun HttpException.serverMessage(): String? {
    val raw = response()?.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return null
    return runCatching { json.decodeFromString<ApiErrorDto>(raw) }
        .getOrNull()?.message?.takeIf { it.isNotBlank() }
}

// Umbraco (n.d.)
private fun statusMessage(code: Int): String = when (code) {
    401 -> "Your session has expired. Please sign in again."
    404 -> "That item no longer exists."
    in 500..599 -> "The server is having trouble. Please try again shortly."
    else -> "Something went wrong. Please try again."
}

// References:
// 1. Umbraco. n.d. What are HTTP status codes? [Online]. Available at: https://umbraco.com/knowledge-base/http-status-codes/ [Accessed 11 September 2026].
