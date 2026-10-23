package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import java.time.Instant

// Firestore data access for clothing items (IIE, 2026).

class ItemService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val items get() = db.collection(Collections.CLOTHING_ITEMS)

    // Lists the caller's items, newest change first.
    suspend fun list(
        uid: String,
        category: String?,
        updatedSince: String?,
        limit: Int,
        cursor: String?,
    ): PageResponse<ItemResponse> {
        val safeLimit = limit.coerceIn(1, MAX_PAGE)

        var query: Query = items
            // The ownership filter. Without it this endpoint returns everyone's wardrobe.
            .whereEqualTo(Fields.OWNER_UID, uid)
            .orderBy(Fields.UPDATED_AT, Query.Direction.DESCENDING)
            .limit(safeLimit)

        if (category != null) {
            query = query.whereEqualTo(Fields.CATEGORY, category)
        }

        if (updatedSince != null) {
            // An incremental sync wants deletions too, so "deleted" is not filtered here.
            query = query.whereGreaterThan(Fields.UPDATED_AT, updatedSince.toTimestamp())
        } else {
            // A first-time load has nothing to reconcile, so hide removed items.
            query = query.whereEqualTo(Fields.DELETED, false)
        }

        if (cursor != null) {
            val last = items.document(cursor).get().await()
            if (last.exists()) query = query.startAfter(last)
        }

        val documents = query.get().await().documents

        return PageResponse(
            data = documents.map { it.toObject(ClothingItemDocument::class.java).toResponse() },
            // Only offer a cursor when the page was full; a short page is the last one.
            nextCursor = documents.lastOrNull()?.id?.takeIf { documents.size == safeLimit },
        )
    }

    suspend fun get(uid: String, id: String): ItemResponse =
        requireOwned(uid, id).toResponse()

    suspend fun create(uid: String, body: CreateItemRequest): ItemResponse {
        body.validate()

        val now = Timestamp.now()
        val ref = items.document()

        val document = ClothingItemDocument(
            id = ref.id,
            // Taken from the verified token, never from the request body.
            ownerUid = uid,
            name = body.name.trim(),
            category = body.category.trim(),
            colour = body.colour?.trim(),
            brand = body.brand?.trim(),
            size = body.size?.trim(),
            purchasePrice = body.purchasePrice,
            wearCount = 0,
            wearLimit = body.wearLimit?.toLong() ?: defaultWearLimitFor(uid),
            archived = false,
            deleted = false,
            createdAt = now,
            updatedAt = now,
        )

        ref.set(document).await()
        return document.toResponse()
    }

    suspend fun update(uid: String, id: String, body: UpdateItemRequest): ItemResponse {
        body.validate()
        requireOwned(uid, id)

        val changes = buildMap<String, Any> {
            body.name?.let { put("name", it.trim()) }
            body.category?.let { put(Fields.CATEGORY, it.trim()) }
            body.colour?.let { put("colour", it.trim()) }
            body.brand?.let { put("brand", it.trim()) }
            body.size?.let { put("size", it.trim()) }
            body.purchasePrice?.let { put("purchasePrice", it) }
            body.wearLimit?.let { put(Fields.WEAR_LIMIT, it.toLong()) }
            body.archived?.let { put(Fields.ARCHIVED, it) }
            put(Fields.UPDATED_AT, Timestamp.now())
        }

        items.document(id).update(changes).await()
        return requireOwned(uid, id).toResponse()
    }

    // Marks an item deleted instead of removing it.
    suspend fun softDelete(uid: String, id: String) {
        requireOwned(uid, id)

        items.document(id).update(
            mapOf(
                Fields.DELETED to true,
                Fields.UPDATED_AT to Timestamp.now(),
            )
        ).await()
    }

    // Records a wear and increments the counter in one atomic batch.
    suspend fun logWear(uid: String, itemId: String, body: LogWearRequest): WearResponse {
        val item = requireOwned(uid, itemId)

        val wornOn = body.wornOn?.toTimestamp() ?: Timestamp.now()
        val wearRef = db.collection(Collections.WEAR_HISTORY).document()

        val entry = WearHistoryDocument(
            id = wearRef.id,
            ownerUid = uid,
            clothingItemId = itemId,
            outfitId = body.outfitId,
            wornOn = wornOn,
        )

        val batch = db.batch()
        batch.set(wearRef, entry)
        batch.update(
            items.document(itemId),
            mapOf(
                Fields.WEAR_COUNT to FieldValue.increment(1),
                Fields.UPDATED_AT to Timestamp.now(),
            )
        )
        batch.commit().await()

        return WearResponse(
            id = wearRef.id,
            clothingItemId = itemId,
            outfitId = body.outfitId,
            wornOn = wornOn.toIso(),
            newWearCount = item.wearCount + 1,
        )
    }

    // Totals behind the home and profile screens. Counted here so every client agrees.
    suspend fun summary(uid: String): WardrobeSummaryResponse {
        val itemDocuments = items
            .whereEqualTo(Fields.OWNER_UID, uid)
            .whereEqualTo(Fields.DELETED, false)
            .get()
            .await()
            .documents
            .map { it.toObject(ClothingItemDocument::class.java) }

        val outfitCount = db.collection(Collections.OUTFITS)
            .whereEqualTo(Fields.OWNER_UID, uid)
            .whereEqualTo(Fields.DELETED, false)
            .get()
            .await()
            .size()

        return WardrobeSummaryResponse(
            itemCount = itemDocuments.size,
            outfitCount = outfitCount,
            totalWears = itemDocuments.sumOf { it.wearCount },
            // Rounded to cents so the figure prints cleanly as money.
            totalValue = Math.round(itemDocuments.sumOf { it.purchasePrice ?: 0.0 } * 100.0) / 100.0,
            needsWashCount = itemDocuments.count { needsWash(it.wearCount, it.wearLimit) },
        )
    }

    // Falls back to the shared default if the user has never opened settings.
    private suspend fun defaultWearLimitFor(uid: String): Long {
        val snapshot = db.collection(Collections.USERS)
            .document(uid)
            .collection(Collections.SETTINGS)
            .document(Collections.SETTINGS_DOC)
            .get()
            .await()

        if (!snapshot.exists()) return DEFAULT_WEAR_LIMIT

        return snapshot.toObject(UserSettingsDocument::class.java)?.defaultWearLimit
            ?: DEFAULT_WEAR_LIMIT
    }

    // Fetches an item and proves the caller owns it.
    private suspend fun requireOwned(uid: String, id: String): ClothingItemDocument {
        val snapshot = items.document(id).get().await()

        if (!snapshot.exists()) throw NotFoundException("Item $id was not found")

        val document = snapshot.toObject(ClothingItemDocument::class.java)
            ?: throw NotFoundException("Item $id was not found")

        if (document.ownerUid != uid) throw NotFoundException("Item $id was not found")

        return document
    }

    private companion object {
        const val MAX_PAGE = 100
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
