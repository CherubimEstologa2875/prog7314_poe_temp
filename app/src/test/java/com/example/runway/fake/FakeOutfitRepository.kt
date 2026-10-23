package com.example.runway.fake

import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.OutfitRepository

// In-memory outfits for the confidence sheet, Home, the builder and the planner.
class FakeOutfitRepository(
    private var outfit: Outfit = Outfit(id = "outfit-1", name = "Friday dinner"),
) : OutfitRepository {

    var wearCount = 0
        private set
    var lastWornId: String? = null
        private set
    var created: Outfit? = null
        private set
    var updated: Outfit? = null
        private set

    var getError: Throwable? = null
    var wearError: Throwable? = null
    var listError: Throwable? = null
    var saveError: Throwable? = null

    // Set this to hand back more than the one outfit.
    var outfits: List<Outfit>? = null

    override suspend fun list(): Result<List<Outfit>> =
        listError?.let { Result.failure(it) } ?: Result.success(outfits ?: listOf(outfit))

    override suspend fun get(id: String): Result<Outfit> {
        getError?.let { return Result.failure(it) }
        val found = outfits?.firstOrNull { it.id == id } ?: outfit.takeIf { it.id == id }
        return found?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(id))
    }

    override suspend fun create(outfit: Outfit): Result<Outfit> {
        saveError?.let { return Result.failure(it) }
        created = outfit
        return Result.success(outfit.copy(id = "outfit-new"))
    }

    override suspend fun update(outfit: Outfit): Result<Outfit> {
        saveError?.let { return Result.failure(it) }
        updated = outfit
        this.outfit = outfit
        return Result.success(outfit)
    }

    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun logWear(id: String): Result<Unit> {
        wearCount++
        lastWornId = id
        return wearError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
