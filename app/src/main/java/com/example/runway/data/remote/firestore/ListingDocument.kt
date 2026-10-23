package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the Listings collection. */
data class ListingDocument(
    var createdAt: Timestamp? = null,
    var itemId: String? = null,
    var listingType: String? = null,
    var listingsId: String? = null,
    var status: String? = null,
    var suburb: String? = null,
    var userId: String? = null,
)
