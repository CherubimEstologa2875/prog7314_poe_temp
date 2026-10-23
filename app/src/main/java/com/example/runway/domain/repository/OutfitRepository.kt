package com.example.runway.domain.repository

import com.example.runway.domain.model.Outfit

// Repository contract for saved outfits (IIE, 2026).
// Unlike the wardrobe these are read straight from the API rather than from Room,
// so they need a connection. The wardrobe stays usable offline; outfits do not.

interface OutfitRepository {

    suspend fun list(): Result<List<Outfit>>

    suspend fun get(id: String): Result<Outfit>

    suspend fun create(outfit: Outfit): Result<Outfit>

    suspend fun update(outfit: Outfit): Result<Outfit>

    suspend fun delete(id: String): Result<Unit>

    // Counts a wear against every garment in the outfit.
    suspend fun logWear(id: String): Result<Unit>
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
