package com.example.runway.data.remote.api

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

// Attaches the Firebase ID token to every outgoing request (IIE, 2026; Seobility, n.d.).
// The token is never written to a log or shipped in the APK (CodePath Android Cliffnotes, n.d.).

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Not signed in: send the request without a token and let the API answer 401.
        val user = FirebaseAuth.getInstance().currentUser
            ?: return chain.proceed(chain.request())

        val token = try {
            // false means "reuse the cached token unless it has expired".
            Tasks.await(user.getIdToken(false)).token
        } catch (e: Exception) {
            Log.w(TAG, "Could not obtain a Firebase ID token: ${e.message}")
            null
        }

        val request = chain.request().newBuilder()
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        return chain.proceed(request)
    }

    private companion object {
        const val TAG = "AuthInterceptor"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
CodePath Android Cliffnotes, n.d.. Storing Secret Keys in Android. [online] Available at: <https://guides.codepath.com/android/Storing-Secret-Keys-in-Android> [Accessed 31 July 2023].
Seobility, n.d.. HTTP headers. [online] Available at: <https://www.seobility.net/en/wiki/HTTP_headers> [Accessed 31 July 2023].
*/
