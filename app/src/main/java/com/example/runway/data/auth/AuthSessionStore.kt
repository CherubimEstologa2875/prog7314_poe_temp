package com.example.runway.data.auth

import android.content.Context

class AuthSessionStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    val currentSession: AuthSession?
        get() {
            val id = preferences.getString(KEY_ID, null) ?: return null
            val email = preferences.getString(KEY_EMAIL, null) ?: return null
            return AuthSession(
                id = id,
                email = email,
                displayName = preferences.getString(KEY_DISPLAY_NAME, "").orEmpty(),
                photoUrl = preferences.getString(KEY_PHOTO_URL, null),
                signedInAtMillis = preferences.getLong(KEY_SIGNED_IN_AT, 0L),
                biometricEnabled = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            )
        }

    fun saveSession(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ID, session.id)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_PHOTO_URL, session.photoUrl)
            .putLong(KEY_SIGNED_IN_AT, session.signedInAtMillis)
            .putBoolean(KEY_BIOMETRIC_ENABLED, session.biometricEnabled)
            .apply()
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "runway_auth_session"
        const val KEY_ID = "id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PHOTO_URL = "photo_url"
        const val KEY_SIGNED_IN_AT = "signed_in_at"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
