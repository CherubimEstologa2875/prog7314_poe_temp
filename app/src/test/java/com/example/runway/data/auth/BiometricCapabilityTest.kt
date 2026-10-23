package com.example.runway.data.auth

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricCapabilityTest {
    @Test fun mapsSupportedHardwareToAvailable() {
        assertEquals(
            BiometricAvailability.AVAILABLE,
            BiometricCapability.fromCanAuthenticateResult(BiometricManager.BIOMETRIC_SUCCESS),
        )
    }

    @Test fun mapsMissingEnrollmentToSetupRequired() {
        assertEquals(
            BiometricAvailability.NOT_ENROLLED,
            BiometricCapability.fromCanAuthenticateResult(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED),
        )
    }

    @Test fun mapsMissingHardwareToUnavailable() {
        assertEquals(
            BiometricAvailability.NO_HARDWARE,
            BiometricCapability.fromCanAuthenticateResult(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE),
        )
    }
}
