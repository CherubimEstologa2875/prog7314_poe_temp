package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the ConfidenceRating collection. */
data class ConfidenceRatingDocument(
    var createdAt: Timestamp? = null,
    var outfitId: String? = null,
    var rating: Long? = null,
    var ratingId: String? = null,
    var userId: String? = null,
)
