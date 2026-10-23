package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore

// Firestore data access for the caller's profile and settings (IIE, 2026).

class UserService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val users get() = db.collection(Collections.USERS)

    // Returns the profile, creating it on first sign-in.
    suspend fun getOrCreate(principal: RunwayPrincipal): UserResponse {
        val ref = users.document(principal.uid)
        val snapshot = ref.get().await()

        if (snapshot.exists()) {
            return snapshot.toObject(UserDocument::class.java)!!.toResponse()
        }

        val now = Timestamp.now()
        val document = UserDocument(
            uid = principal.uid,
            displayName = principal.email?.substringBefore('@') ?: "Runway user",
            email = principal.email ?: "",
            createdAt = now,
            updatedAt = now,
        )

        ref.set(document).await()
        return document.toResponse()
    }

    // Settings live in a fixed document at users/{uid}/settings/preferences.
    suspend fun getSettings(uid: String): SettingsDto {
        val snapshot = settingsRef(uid).get().await()

        return if (snapshot.exists()) {
            snapshot.toObject(UserSettingsDocument::class.java)!!.toDto()
        } else {
            SettingsDto()
        }
    }

    suspend fun saveSettings(uid: String, body: SettingsDto): SettingsDto {
        body.validate()

        val document = body.toDocument().apply { updatedAt = Timestamp.now() }
        settingsRef(uid).set(document).await()

        return document.toDto()
    }

    private fun settingsRef(uid: String) =
        users.document(uid)
            .collection(Collections.SETTINGS)
            .document(Collections.SETTINGS_DOC)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
