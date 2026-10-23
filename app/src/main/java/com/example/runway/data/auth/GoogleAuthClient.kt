package com.example.runway.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.runway.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/** Wraps Credential Manager and the Firebase exchange so the fragments stay focused on UI. */
class GoogleAuthClient(
    context: Context,
    private val firebaseAuth: FirebaseAuth,
) {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    /** Firebase only accepts an ID token minted for the web client, not the Android one. */
    private val webClientId = appContext.getString(R.string.default_web_client_id)

    /** Firebase persists its own user across launches, so it decides whether a session exists. */
    val hasActiveSession: Boolean get() = firebaseAuth.currentUser != null

    /** Needs an Activity context: Credential Manager draws the account sheet over the caller. */
    suspend fun signIn(activityContext: Context): SignInOutcome {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val response = try {
            credentialManager.getCredential(activityContext, request)
        } catch (e: GetCredentialException) {
            Log.w(TAG, "credential request failed: ${e::class.simpleName}: ${e.message}", e)
            return e.toFailure()
        }

        return exchangeWithFirebase(response)
    }

    suspend fun signOut(): Boolean {
        firebaseAuth.signOut()
        return try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            true
        } catch (e: ClearCredentialException) {
            Log.w(TAG, "could not clear credential state", e)
            false
        }
    }

    private suspend fun exchangeWithFirebase(response: GetCredentialResponse): SignInOutcome {
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "unexpected credential type: ${credential.type}")
            return SignInOutcome.Failure.IncompleteAccount
        }

        val idToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "could not parse the Google ID token", e)
            return SignInOutcome.Failure.IncompleteAccount
        }

        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val user = firebaseAuth.signInWithCredential(firebaseCredential).await().user
                ?: return SignInOutcome.Failure.Failed
            val email = user.email
            if (email.isNullOrBlank()) return SignInOutcome.Failure.IncompleteAccount

            SignInOutcome.Success(
                AuthSession(
                    id = user.uid,
                    email = email,
                    displayName = user.displayName.orEmpty(),
                    photoUrl = user.photoUrl?.toString(),
                    signedInAtMillis = System.currentTimeMillis(),
                    biometricEnabled = false,
                )
            )
        } catch (_: FirebaseNetworkException) {
            SignInOutcome.Failure.Network
        } catch (e: Exception) {
            Log.w(TAG, "credential exchange failed", e)
            SignInOutcome.Failure.Failed
        }
    }

    private fun GetCredentialException.toFailure(): SignInOutcome.Failure = when (this) {
        // Credential Manager reports a refused request as a cancellation, so only the message tells them apart.
        is GetCredentialCancellationException ->
            if (message?.contains(REAUTH_FAILED, ignoreCase = true) == true) {
                SignInOutcome.Failure.Unregistered
            } else {
                SignInOutcome.Failure.Cancelled
            }

        is NoCredentialException -> SignInOutcome.Failure.NoAccount
        is GetCredentialInterruptedException -> SignInOutcome.Failure.Network
        is GetCredentialProviderConfigurationException -> SignInOutcome.Failure.Configuration
        else -> SignInOutcome.Failure.Failed
    }

    private companion object {
        const val TAG = "GoogleAuthClient"

        // Google's wording when it rejects the app's signing certificate.
        const val REAUTH_FAILED = "reauth"
    }
}

/* References:
    1. Google. 2026. Authenticate with Google on Android. [Online].
        Available at: https://firebase.google.com/docs/auth/android/google-signin [Accessed 10 September 2026]

    2. Android Developers. 2026. Implement Sign in with Google. [Online].
        Available at: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation [Accessed 10 September 2026]
*/
