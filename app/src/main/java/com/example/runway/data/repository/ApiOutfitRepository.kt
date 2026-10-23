package com.example.runway.data.repository

import com.example.runway.data.remote.api.LogOutfitWearDto
import com.example.runway.data.remote.api.OutfitDto
import com.example.runway.data.remote.api.OutfitItemDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.UpdateOutfitDto
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitLayer
import com.example.runway.domain.repository.OutfitRepository

// Saved outfits, read and written through the Runway REST API (IIE, 2026; Square, Inc., n.d.).

class ApiOutfitRepository(
    private val api: RunwayApi,
) : OutfitRepository {

    override suspend fun list(): Result<List<Outfit>> = runCatching {
        api.getOutfits().data.map { it.toDomain() }
    }

    override suspend fun get(id: String): Result<Outfit> = runCatching {
        api.getOutfit(id).toDomain()
    }

    override suspend fun create(outfit: Outfit): Result<Outfit> = runCatching {
        api.createOutfit(outfit.toCreateDto()).toDomain()
    }

    override suspend fun update(outfit: Outfit): Result<Outfit> = runCatching {
        api.updateOutfit(
            outfit.id,
            UpdateOutfitDto(
                name = outfit.name,
                occasion = outfit.occasion,
                items = outfit.toItemDtos(),
            ),
        ).toDomain()
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deleteOutfit(id)
    }

    override suspend fun logWear(id: String): Result<Unit> = runCatching {
        api.logOutfitWear(id, LogOutfitWearDto())
    }
}

// itemIds decides what is in the outfit; layers only says where each garment sits.
private fun Outfit.toItemDtos(): List<OutfitItemDto> {
    val placed = layers.associateBy { it.itemId }
    return itemIds.mapIndexed { index, itemId ->
        val layer = placed[itemId]
        OutfitItemDto(
            clothingItemId = itemId,
            x = layer?.x?.toDouble() ?: 0.0,
            y = layer?.y?.toDouble() ?: 0.0,
            scale = layer?.scale?.toDouble() ?: 1.0,
            rotation = layer?.rotation?.toDouble() ?: 0.0,
            zIndex = layer?.z?.toLong() ?: index.toLong(),
        )
    }
}

private fun Outfit.toCreateDto(): OutfitDto = OutfitDto(
    name = name,
    occasion = occasion,
    items = toItemDtos(),
)

private fun OutfitDto.toDomain(): Outfit {
    val ordered = items.sortedBy { it.zIndex }
    return Outfit(
        id = id,
        name = name,
        itemIds = ordered.map { it.clothingItemId },
        updatedAt = updatedAt,
        occasion = occasion,
        layers = ordered.map {
            OutfitLayer(
                itemId = it.clothingItemId,
                x = it.x.toFloat(),
                y = it.y.toFloat(),
                scale = it.scale.toFloat(),
                rotation = it.rotation.toFloat(),
                z = it.zIndex.toInt(),
            )
        },
    )
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/
