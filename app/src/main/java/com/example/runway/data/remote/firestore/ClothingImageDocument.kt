package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the ClothingImages collection. */
data class ClothingImageDocument(
    var createAt: Timestamp? = null,
    var imageString: String? = null,
    // This spelling is present in Firestore and must remain unchanged.
    var imgaeId: String? = null,
    var itemId: String? = null,
    var userId: String? = null,
)
