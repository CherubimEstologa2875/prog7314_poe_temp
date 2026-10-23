package com.example

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Bridges the Firebase Admin SDK's ApiFuture into Kotlin coroutines (IIE, 2026).
// Suspends instead of blocking a request thread (Android Open Source Project, 2020b).
// Uses kotlinx.coroutines to suspend the call (JetBrains, 2026).

suspend fun <T> ApiFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->

    ApiFutures.addCallback(
        this,
        object : ApiFutureCallback<T> {
            override fun onSuccess(result: T) = continuation.resume(result)
            override fun onFailure(t: Throwable) = continuation.resumeWithException(t)
        },
        MoreExecutors.directExecutor()
    )

    // If the client disconnects, stop waiting on Firestore.
    continuation.invokeOnCancellation { cancel(true) }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020b. Processes and threads overview. [online] Available at: <https://developer.android.com/guide/components/processes-and-threads> [Accessed 31 July 2023].
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
*/
