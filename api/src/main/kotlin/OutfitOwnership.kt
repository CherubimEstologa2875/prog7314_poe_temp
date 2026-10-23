package com.example

import com.google.cloud.firestore.Firestore

// Checks the outfit belongs to the caller. Missing and someone else's both answer 404.
suspend fun requireOwnedOutfit(db: Firestore, uid: String, outfitId: String) {
    val snapshot = db.collection(Collections.OUTFITS).document(outfitId).get().await()
    if (!snapshot.exists()) throw NotFoundException("Outfit $outfitId was not found")

    val outfit = snapshot.toObject(OutfitDocument::class.java)
        ?: throw NotFoundException("Outfit $outfitId was not found")

    if (outfit.ownerUid != uid || outfit.deleted) throw NotFoundException("Outfit $outfitId was not found")
}
