package com.example.runway.data.auth

import androidx.biometric.BiometricManager

/**
 * The states the UI needs to distinguish before offering native unlock.
 * BIOMETRIC_WEAK is intentional: Android may expose face unlock as weak while
 * still providing a supported, user-verified authenticator.
 */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNSUPPORTED,
    UNKNOWN,
}

object BiometricCapability {
    const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun fromCanAuthenticateResult(result: Int): BiometricAvailability = when (result) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
        else -> BiometricAvailability.UNKNOWN
    }
}
