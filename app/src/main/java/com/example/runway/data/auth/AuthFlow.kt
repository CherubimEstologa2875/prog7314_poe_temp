package com.example.runway.data.auth

enum class AuthDestination {
    SIGN_IN,
    HOME,
    BIOMETRIC,
}

/** Keeps SSO success and returning-session routing consistent and unit-testable. */
object AuthFlow {
    fun afterSso(biometricRequested: Boolean, biometricAvailable: Boolean): AuthDestination =
        if (biometricRequested && biometricAvailable) AuthDestination.BIOMETRIC
        else AuthDestination.HOME

    fun onLaunch(firebaseSessionActive: Boolean, session: AuthSession?): AuthDestination {
        if (!firebaseSessionActive || session == null) return AuthDestination.SIGN_IN
        return if (session.biometricEnabled) AuthDestination.BIOMETRIC else AuthDestination.HOME
    }
}
