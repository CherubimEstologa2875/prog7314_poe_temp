package com.example.runway.data.auth

data class AuthSession(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val signedInAtMillis: Long,
    val biometricEnabled: Boolean,
)
