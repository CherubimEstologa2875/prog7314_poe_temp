package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the ClothingItems collection. */
data class ClothingItemDocument(
    var brand: String? = null,
    var category: String? = null,
    var clothingName: String? = null,
    var colour: String? = null,
    var createdAt: Timestamp? = null,
    var imageId: String? = null,
    var itemId: String? = null,
    var material: String? = null,
    var price: Double? = null,
    var size: String? = null,
    var userId: String? = null,
)
