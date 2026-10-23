package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import java.time.LocalDate
import java.time.ZoneOffset

// Firestore data access for the outfit planner. One outfit per day, per user.
class PlanService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val plans get() = db.collection(Collections.OUTFIT_PLANS)

    // Filtered by owner in Firestore and by date here, so no composite index is needed.
    suspend fun list(uid: String, from: LocalDate, to: LocalDate): List<PlanResponse> {
        validatePlanRange(from, to)

        return plans
            .whereEqualTo(Fields.OWNER_UID, uid)
            .get()
            .await()
            .documents
            .map { it.toObject(OutfitPlanDocument::class.java) }
            .filterNot { it.deleted }
            .map { it.toResponse() }
            .filter { it.date.isNotEmpty() && LocalDate.parse(it.date) in from..to }
            .sortedBy { it.date }
    }

    // Planning a day that already has an outfit replaces it.
    suspend fun set(uid: String, date: LocalDate, body: SetPlanRequest): PlanResponse {
        body.validate()
        requireOwnedOutfit(db, uid, body.outfitId)

        val now = Timestamp.now()
        val document = OutfitPlanDocument(
            id = planId(uid, date),
            // From the verified token, never from the request body.
            ownerUid = uid,
            date = midnightUtc(date),
            outfitId = body.outfitId,
            status = STATUS_PLANNED,
            createdAt = now,
            updatedAt = now,
        )

        plans.document(document.id).set(document).await()
        return document.toResponse()
    }

    // Clearing a day with nothing planned is fine, so this doesn't 404.
    suspend fun clear(uid: String, date: LocalDate) {
        plans.document(planId(uid, date)).delete().await()
    }

    // The uid is part of the id, so one user can never overwrite another's day.
    private fun planId(uid: String, date: LocalDate) = "${uid}_$date"

    private fun midnightUtc(date: LocalDate): Timestamp =
        Timestamp.ofTimeSecondsAndNanos(date.atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0)

    private companion object {
        const val STATUS_PLANNED = "PLANNED"
    }
}
