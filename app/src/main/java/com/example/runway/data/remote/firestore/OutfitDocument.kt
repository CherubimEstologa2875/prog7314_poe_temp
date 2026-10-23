package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/** Firestore document in the Outfits collection. */
data class OutfitDocument(
    @get:PropertyName("Occasion")
    @set:PropertyName("Occasion")
    var occasion: String? = null,
    var createdAt: Timestamp? = null,
    var imageString: String? = null,
    var name: String? = null,
    var outfitId: String? = null,
    var userId: String? = null,
    var wearCount: Long? = null,
)
