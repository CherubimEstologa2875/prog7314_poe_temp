package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query

// Firestore data access for outfits and their item placements (IIE, 2026).

class OutfitService(firestore: Firestore? = null) {
    private val db: Firestore by lazy { firestore ?: FirebaseAdmin.db }

    private val outfits get() = db.collection(Collections.OUTFITS)

    suspend fun list(uid: String, limit: Int, cursor: String?): PageResponse<OutfitResponse> {
        val safeLimit = limit.coerceIn(1, MAX_PAGE)

        var query: Query = outfits
            .whereEqualTo(Fields.OWNER_UID, uid)
            .whereEqualTo(Fields.DELETED, false)
            .orderBy(Fields.UPDATED_AT, Query.Direction.DESCENDING)
            .limit(safeLimit)

        if (cursor != null) {
            val last = outfits.document(cursor).get().await()
            if (last.exists()) query = query.startAfter(last)
        }

        val documents = query.get().await().documents

        val responses = documents.map { snapshot ->
            val outfit = snapshot.toObject(OutfitDocument::class.java)
            outfit.toResponse(loadItems(outfit.id))
        }

        return PageResponse(
            data = responses,
            nextCursor = documents.lastOrNull()?.id?.takeIf { documents.size == safeLimit },
        )
    }

    suspend fun get(uid: String, id: String): OutfitResponse {
        val outfit = requireOwned(uid, id)
        return outfit.toResponse(loadItems(id))
    }

    // Creates the outfit and all of its placements atomically.
    suspend fun create(uid: String, body: CreateOutfitRequest): OutfitResponse {
        body.validate()

        val now = Timestamp.now()
        val ref = outfits.document()

        val document = OutfitDocument(
            id = ref.id,
            ownerUid = uid,
            name = body.name.trim(),
            occasion = body.occasion,
            season = body.season,
            deleted = false,
            createdAt = now,
            updatedAt = now,
        )

        val batch = db.batch()
        batch.set(ref, document)

        val placements = body.items.map { dto ->
            val itemRef = ref.collection(Collections.OUTFIT_ITEMS).document()
            val placement = dto.toDocument(itemRef.id)
            batch.set(itemRef, placement)
            placement
        }

        batch.commit().await()
        return document.toResponse(placements)
    }

    // Updates the outfit, and replaces its placements when the body carries items.
    suspend fun update(uid: String, id: String, body: UpdateOutfitRequest): OutfitResponse {
        body.validate()
        requireOwned(uid, id)

        val ref = outfits.document(id)
        val batch = db.batch()

        val changes = buildMap<String, Any> {
            body.name?.let { put("name", it.trim()) }
            body.occasion?.let { put("occasion", it) }
            body.season?.let { put("season", it) }
            put(Fields.UPDATED_AT, Timestamp.now())
        }
        batch.update(ref, changes)

        // A canvas edit moves and removes pieces, so the old placements go first.
        if (body.items != null) {
            val existing = ref.collection(Collections.OUTFIT_ITEMS).get().await().documents
            existing.forEach { batch.delete(it.reference) }

            body.items.forEach { dto ->
                val itemRef = ref.collection(Collections.OUTFIT_ITEMS).document()
                batch.set(itemRef, dto.toDocument(itemRef.id))
            }
        }

        batch.commit().await()
        return get(uid, id)
    }

    // Records the outfit as worn and counts a wear against each garment in it.
    suspend fun logWear(uid: String, id: String, body: LogOutfitWearRequest): OutfitWearResponse {
        requireOwned(uid, id)

        val placements = loadItems(id)
        if (placements.isEmpty()) {
            throw ValidationException("An outfit needs at least one item before it can be worn")
        }

        val wornOn = body.wornOn?.toTimestamp() ?: Timestamp.now()
        val now = Timestamp.now()
        val itemsCollection = db.collection(Collections.CLOTHING_ITEMS)
        val batch = db.batch()

        val wears = placements.map { placement ->
            val wearRef = db.collection(Collections.WEAR_HISTORY).document()

            batch.set(
                wearRef,
                WearHistoryDocument(
                    id = wearRef.id,
                    ownerUid = uid,
                    clothingItemId = placement.clothingItemId,
                    outfitId = id,
                    wornOn = wornOn,
                ),
            )

            batch.update(
                itemsCollection.document(placement.clothingItemId),
                mapOf(
                    Fields.WEAR_COUNT to FieldValue.increment(1),
                    Fields.UPDATED_AT to now,
                ),
            )

            wearRef.id to placement.clothingItemId
        }

        batch.commit().await()

        // Read back afterwards so the counts returned are the stored ones.
        val responses = wears.map { (wearId, clothingItemId) ->
            val count = itemsCollection.document(clothingItemId).get().await()
                .toObject(ClothingItemDocument::class.java)?.wearCount ?: 0

            WearResponse(
                id = wearId,
                clothingItemId = clothingItemId,
                outfitId = id,
                wornOn = wornOn.toIso(),
                newWearCount = count,
            )
        }

        return OutfitWearResponse(outfitId = id, wornOn = wornOn.toIso(), items = responses)
    }

    suspend fun softDelete(uid: String, id: String) {
        requireOwned(uid, id)
        outfits.document(id).update(
            mapOf(
                Fields.DELETED to true,
                Fields.UPDATED_AT to Timestamp.now(),
            )
        ).await()
    }

    private suspend fun loadItems(outfitId: String): List<OutfitItemDocument> =
        outfits.document(outfitId)
            .collection(Collections.OUTFIT_ITEMS)
            .get()
            .await()
            .documents
            .map { it.toObject(OutfitItemDocument::class.java) }
            .sortedBy { it.zIndex }

    // Same 404-for-everything rule as ItemService (OWASP, 2026).
    private suspend fun requireOwned(uid: String, id: String): OutfitDocument {
        val snapshot = outfits.document(id).get().await()

        if (!snapshot.exists()) throw NotFoundException("Outfit $id was not found")

        val document = snapshot.toObject(OutfitDocument::class.java)
            ?: throw NotFoundException("Outfit $id was not found")

        if (document.ownerUid != uid) throw NotFoundException("Outfit $id was not found")

        return document
    }

    private companion object {
        const val MAX_PAGE = 50
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
OWASP, 2026. Broken access control. [online] Available at: <https://owasp.org/Top10/A01_2021-Broken_Access_Control/> [Accessed 11 September 2026].
*/
