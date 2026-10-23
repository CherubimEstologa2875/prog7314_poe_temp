package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the ListingRequests collection. */
data class ListingRequestDocument(
    var createAt: Timestamp? = null,
    var listingId: String? = null,
    var listingRequestId: String? = null,
    var ownerUserId: String? = null,
    var requestUserId: String? = null,
    var status: String? = null,
)
