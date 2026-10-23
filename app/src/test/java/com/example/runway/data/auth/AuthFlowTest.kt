package com.example.runway.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthFlowTest {
    private val session = AuthSession("uid", "user@example.com", "User", null, 1L, true)

    @Test fun ssoWithVerifiedBiometricChoiceGoesToBiometricGate() {
        assertEquals(AuthDestination.BIOMETRIC, AuthFlow.afterSso(true, true))
    }

    @Test fun ssoFallsBackToHomeWhenBiometricIsUnavailable() {
        assertEquals(AuthDestination.HOME, AuthFlow.afterSso(true, false))
    }

    @Test fun returningFirebaseSessionWithBiometricEnabledUsesBiometricGate() {
        assertEquals(AuthDestination.BIOMETRIC, AuthFlow.onLaunch(true, session))
    }

    @Test fun missingFirebaseSessionAlwaysReturnsToSso() {
        assertEquals(AuthDestination.SIGN_IN, AuthFlow.onLaunch(false, session))
    }
}
