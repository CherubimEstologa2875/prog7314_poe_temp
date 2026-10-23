package com.example.runway.data.remote.api

import kotlinx.serialization.Serializable

// Network DTOs for the Runway REST API client (IIE, 2026; Tagliaferri, 2016).
// Data classes are serialised with kotlinx.serialization (JetBrains, 2026).
@Serializable
data class ItemDto(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearCount: Long = 0,
    val wearLimit: Long = 3,
    // Computed by the API so every client shows the same figure.
    val costPerWear: Double? = null,
    // True once the item has been worn as many times as its limit allows.
    val needsWash: Boolean = false,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateItemDto(
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    // Left null to inherit the limit from the user's settings.
    val wearLimit: Int? = null,
)

@Serializable
data class UpdateItemDto(
    val name: String? = null,
    val category: String? = null,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearLimit: Int? = null,
    val archived: Boolean? = null,
)

@Serializable
data class LogWearDto(
    val outfitId: String? = null,
    val wornOn: String? = null,
)

@Serializable
data class WearDto(
    val id: String = "",
    val clothingItemId: String = "",
    val outfitId: String? = null,
    val wornOn: String? = null,
    val newWearCount: Long = 0,
)

@Serializable
data class OutfitItemDto(
    val id: String = "",
    val clothingItemId: String = "",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val zIndex: Long = 0,
)

@Serializable
data class OutfitDto(
    val id: String = "",
    val name: String = "",
    val occasion: String? = null,
    val season: String? = null,
    val items: List<OutfitItemDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

// A partial update. Sending items replaces every placement on the outfit.
@Serializable
data class UpdateOutfitDto(
    val name: String? = null,
    val occasion: String? = null,
    val season: String? = null,
    val items: List<OutfitItemDto>? = null,
)

@Serializable
data class LogOutfitWearDto(
    val wornOn: String? = null,
)

@Serializable
data class OutfitWearDto(
    val outfitId: String = "",
    val wornOn: String? = null,
    // One entry per garment in the outfit, with its new count.
    val items: List<WearDto> = emptyList(),
)

// Totals for the home and profile screens.
@Serializable
data class WardrobeSummaryDto(
    val itemCount: Int = 0,
    val outfitCount: Int = 0,
    val totalWears: Long = 0,
    val totalValue: Double = 0.0,
    val needsWashCount: Int = 0,
)

@Serializable
data class RatingDto(
    val id: String = "",
    val outfitId: String = "",
    val score: Long = 0,
    val note: String? = null,
    val ratedAt: String? = null,
)

@Serializable
data class CreateRatingDto(
    val score: Int,
    val note: String? = null,
)

@Serializable
data class RatingSummaryDto(
    val outfitId: String = "",
    val count: Int = 0,
    // Null until the outfit has been rated once.
    val average: Double? = null,
    val ratings: List<RatingDto> = emptyList(),
)

@Serializable
data class UserDto(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class SettingsDto(
    val theme: String = "SYSTEM",
    val language: String = "en",
    val units: String = "METRIC",
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val defaultWearLimit: Int = 3,
)

// One page of results.
@Serializable
data class PageDto<T>(
    val data: List<T> = emptyList(),
    val nextCursor: String? = null,
)

// The single error shape every failing endpoint returns.
@Serializable
data class ApiErrorDto(
    val code: String = "unknown",
    val message: String = "",
)

@Serializable
data class PlanDto(
    val id: String = "",
    // YYYY-MM-DD
    val date: String = "",
    val outfitId: String = "",
    val status: String = "",
)

@Serializable
data class SetPlanDto(
    val outfitId: String,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
Tagliaferri, L., 2016. An Introduction to JSON. [online] Available at: <https://www.digitalocean.com/community/tutorials/an-introduction-to-json> [Accessed 31 July 2023].
*/
