package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the WearHistory collection. */
data class WearHistoryDocument(
    var itemId: String? = null,
    var outfitId: String? = null,
    var userId: String? = null,
    var wearDate: Timestamp? = null,
    var wearId: String? = null,
)
