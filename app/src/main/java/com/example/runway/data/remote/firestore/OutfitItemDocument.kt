package com.example.runway.data.remote.firestore

import com.google.firebase.firestore.PropertyName

/** Firestore document in the OutfitItems collection. */
data class OutfitItemDocument(
    @get:PropertyName("OutfitItemId")
    @set:PropertyName("OutfitItemId")
    var outfitItemId: String? = null,
    var itemId: String? = null,
    var layerOrder: Long? = null,
    var outfitId: String? = null,
    var positionX: Double? = null,
    var positionY: Double? = null,
    @get:PropertyName("rotation ")
    @set:PropertyName("rotation ")
    var rotation: Double? = null,
    var scale: Double? = null,
)
