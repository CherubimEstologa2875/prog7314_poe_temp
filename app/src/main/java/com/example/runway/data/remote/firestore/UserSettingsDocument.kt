package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/** Firestore document in the Settings collection. */
data class UserSettingsDocument(
    @get:PropertyName("Language")
    @set:PropertyName("Language")
    var language: String? = null,
    var theme: String? = null,
    var updatedAt: Timestamp? = null,
    var userId: String? = null,
)
