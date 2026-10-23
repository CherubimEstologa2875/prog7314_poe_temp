package com.example.runway.data.remote.firestore

import com.google.firebase.Timestamp

/** Firestore document in the OutfitPlans collection. */
data class OutfitPlanDocument(
    var createdAt: Timestamp? = null,
    var outfitId: String? = null,
    var outfitPlanId: String? = null,
    var plannedDate: Timestamp? = null,
    var userId: String? = null,
)
