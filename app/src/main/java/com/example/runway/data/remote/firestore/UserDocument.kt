package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the Users collection. */
data class UserDocument(
    var createdAt: Timestamp? = null,
    var email: String? = null,
    var name: String? = null,
    var userId: String? = null,
)
