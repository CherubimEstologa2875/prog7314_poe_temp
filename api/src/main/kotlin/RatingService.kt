package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore

// Firestore data access for confidence ratings.

class RatingService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val ratings get() = db.collection(Collections.CONFIDENCE_RATINGS)

    // Ratings are only ever read for one outfit, newest first.
    suspend fun listForOutfit(uid: String, outfitId: String): RatingSummaryResponse {
        requireOwnedOutfit(db, uid, outfitId)

        // Sorted here instead of in the query, which would need a composite index.
        val documents = ratings
            .whereEqualTo(Fields.OWNER_UID, uid)
            .whereEqualTo(Fields.OUTFIT_ID, outfitId)
            .get()
            .await()
            .documents
            .map { it.toObject(ConfidenceRatingDocument::class.java) }
            .sortedByDescending { it.ratedAt }
            .take(MAX_RATINGS)

        return RatingSummaryResponse(
            outfitId = outfitId,
            count = documents.size,
            average = averageScore(documents.map { it.score }),
            ratings = documents.map { it.toResponse() },
        )
    }

    suspend fun create(uid: String, outfitId: String, body: CreateRatingRequest): RatingResponse {
        body.validate()
        // Stops a rating being attached to someone else's outfit.
        requireOwnedOutfit(db, uid, outfitId)

        val ref = ratings.document()
        val document = ConfidenceRatingDocument(
            id = ref.id,
            // From the verified token, never from the request body.
            ownerUid = uid,
            outfitId = outfitId,
            score = body.score.toLong(),
            note = body.note?.trim()?.takeIf { it.isNotEmpty() },
            ratedAt = Timestamp.now(),
        )

        ref.set(document).await()
        return document.toResponse()
    }

    private companion object {
        const val MAX_RATINGS = 50
    }
}
