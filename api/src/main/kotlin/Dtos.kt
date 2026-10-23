package com.example

import com.google.cloud.Timestamp
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Request and response DTOs for the Runway REST API (IIE, 2026; Tagliaferri, 2016).
// Serialisation is driven by annotations (Kotlin Foundation, 2023).
// One page of results plus the cursor needed to ask for the next (Google, 2026).
// Data classes are serialised with kotlinx.serialization (JetBrains, 2026).
@Serializable
data class PageResponse<T>(
    val data: List<T>,
    val nextCursor: String? = null,
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
)

@Serializable
data class ItemResponse(
    val id: String,
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearCount: Long = 0,
    val wearLimit: Long = DEFAULT_WEAR_LIMIT,
    // Computed on the server so every client shows the same number.
    val costPerWear: Double? = null,
    // Saves every client working out the laundry rule for itself.
    val needsWash: Boolean = false,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    // Left out means "use whatever the user's settings say".
    val wearLimit: Int? = null,
) {
    // Validation happens on the server even though the app validates too: the app can be bypassed by anyone with the API's URL and a token.
    fun validate() {
        if (name.isBlank()) throw ValidationException("name is required")
        if (name.length > MAX_NAME) throw ValidationException("name must be $MAX_NAME characters or fewer")
        if (category.isBlank()) throw ValidationException("category is required")
        if (purchasePrice != null && purchasePrice < 0) {
            throw ValidationException("purchasePrice cannot be negative")
        }
        validateWearLimit(wearLimit)
    }
}

// A partial update.
@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val category: String? = null,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearLimit: Int? = null,
    val archived: Boolean? = null,
) {
    fun validate() {
        if (name != null && name.isBlank()) throw ValidationException("name cannot be blank")
        if (category != null && category.isBlank()) throw ValidationException("category cannot be blank")
        if (purchasePrice != null && purchasePrice < 0) {
            throw ValidationException("purchasePrice cannot be negative")
        }
        validateWearLimit(wearLimit)
        if (name == null && category == null && colour == null && brand == null &&
            size == null && purchasePrice == null && wearLimit == null && archived == null
        ) {
            throw ValidationException("Provide at least one field to update")
        }
    }
}

@Serializable
data class LogWearRequest(
    val outfitId: String? = null,
    // ISO-8601 instant.
    val wornOn: String? = null,
)

@Serializable
data class WearResponse(
    val id: String,
    val clothingItemId: String,
    val outfitId: String? = null,
    val wornOn: String? = null,
    val newWearCount: Long = 0,
)

@Serializable
data class OutfitItemDto(
    val id: String = "",
    val clothingItemId: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val zIndex: Long = 0,
)

@Serializable
data class OutfitResponse(
    val id: String,
    val name: String,
    val occasion: String? = null,
    val season: String? = null,
    val items: List<OutfitItemDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateOutfitRequest(
    val name: String,
    val occasion: String? = null,
    val season: String? = null,
    val items: List<OutfitItemDto> = emptyList(),
) {
    fun validate() {
        if (name.isBlank()) throw ValidationException("name is required")
        if (items.any { it.clothingItemId.isBlank() }) {
            throw ValidationException("every outfit item needs a clothingItemId")
        }
    }
}

@Serializable
data class UserResponse(
    val uid: String,
    val displayName: String,
    val email: String,
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
    val defaultWearLimit: Int = DEFAULT_WEAR_LIMIT.toInt(),
) {
    fun validate() {
        if (theme !in setOf("LIGHT", "DARK", "SYSTEM")) {
            throw ValidationException("theme must be LIGHT, DARK or SYSTEM")
        }
        if (units !in setOf("METRIC", "IMPERIAL")) {
            throw ValidationException("units must be METRIC or IMPERIAL")
        }
        validateWearLimit(defaultWearLimit)
    }
}

// A partial update. Sending items replaces every placement on the outfit.
@Serializable
data class UpdateOutfitRequest(
    val name: String? = null,
    val occasion: String? = null,
    val season: String? = null,
    val items: List<OutfitItemDto>? = null,
) {
    fun validate() {
        if (name != null && name.isBlank()) throw ValidationException("name cannot be blank")
        if (name != null && name.length > MAX_NAME) {
            throw ValidationException("name must be $MAX_NAME characters or fewer")
        }
        if (items != null && items.any { it.clothingItemId.isBlank() }) {
            throw ValidationException("every outfit item needs a clothingItemId")
        }
        if (name == null && occasion == null && season == null && items == null) {
            throw ValidationException("Provide at least one field to update")
        }
    }
}

@Serializable
data class LogOutfitWearRequest(
    // ISO-8601 instant.
    val wornOn: String? = null,
)

@Serializable
data class OutfitWearResponse(
    val outfitId: String,
    val wornOn: String? = null,
    // One entry per garment in the outfit, with its new count.
    val items: List<WearResponse> = emptyList(),
)

// Totals for the home and profile screens, worked out on the server.
@Serializable
data class WardrobeSummaryResponse(
    val itemCount: Int = 0,
    val outfitCount: Int = 0,
    val totalWears: Long = 0,
    val totalValue: Double = 0.0,
    // How many items are at or past their wear limit.
    val needsWashCount: Int = 0,
)

// How confident the user felt in an outfit, 1 to 5.
@Serializable
data class CreateRatingRequest(
    val score: Int,
    val note: String? = null,
) {
    fun validate() {
        if (score < MIN_RATING || score > MAX_RATING) {
            throw ValidationException("score must be between $MIN_RATING and $MAX_RATING")
        }
        if (note != null && note.length > MAX_NOTE) {
            throw ValidationException("note must be $MAX_NOTE characters or fewer")
        }
    }
}

@Serializable
data class RatingResponse(
    val id: String,
    val outfitId: String,
    val score: Long,
    val note: String? = null,
    val ratedAt: String? = null,
)

// Every rating on one outfit, plus the figures the detail screen shows.
@Serializable
data class RatingSummaryResponse(
    val outfitId: String,
    val count: Int = 0,
    // Null until the outfit has been rated once, so the screen can show an empty state.
    val average: Double? = null,
    val ratings: List<RatingResponse> = emptyList(),
)

// Firestore timestamp to ISO-8601, for JSON.
fun Timestamp?.toIso(): String? = this?.toDate()?.toInstant()?.toString()

// ISO-8601 to Firestore timestamp, for storage.
fun String.toTimestamp(): Timestamp =
    try {
        Timestamp.ofTimeSecondsAndNanos(Instant.parse(this).epochSecond, Instant.parse(this).nano)
    } catch (e: Exception) {
        throw ValidationException("'$this' is not a valid ISO-8601 timestamp")
    }

// Cost per wear - the number the wardrobe screen is built around.
fun costPerWear(price: Double?, wearCount: Long): Double? =
    if (price == null || wearCount <= 0) null
    else Math.round((price / wearCount) * 100.0) / 100.0

// One rule for every wear limit the API accepts.
fun validateWearLimit(limit: Int?) {
    if (limit == null) return
    if (limit < MIN_WEAR_LIMIT || limit > MAX_WEAR_LIMIT) {
        throw ValidationException("wearLimit must be between $MIN_WEAR_LIMIT and $MAX_WEAR_LIMIT")
    }
}

// True once a garment has been worn as many times as its limit allows.
fun needsWash(wearCount: Long, wearLimit: Long): Boolean =
    wearLimit > 0 && wearCount >= wearLimit

// The average shown on the outfit screen, rounded to one decimal place.
fun averageScore(scores: List<Long>): Double? =
    if (scores.isEmpty()) null
    else Math.round(scores.sum().toDouble() / scores.size * 10.0) / 10.0

private const val MAX_NAME = 120
const val MIN_WEAR_LIMIT = 1
const val MAX_WEAR_LIMIT = 60
const val MIN_RATING = 1
const val MAX_RATING = 5
private const val MAX_NOTE = 280

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 11 September 2026].
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
Kotlin Foundation, 2023. Annotations. [online] Available at: <https://kotlinlang.org/docs/annotations.html> [Accessed 31 July 2023].
Tagliaferri, L., 2016. An Introduction to JSON. [online] Available at: <https://www.digitalocean.com/community/tutorials/an-introduction-to-json> [Accessed 31 July 2023].
*/

// Body of PUT /api/v1/plans/{date}.
@Serializable
data class SetPlanRequest(
    val outfitId: String = "",
) {
    fun validate() {
        if (outfitId.isBlank()) throw ValidationException("outfitId is required")
    }
}

@Serializable
data class PlanResponse(
    val id: String,
    // The day the outfit is planned for, as YYYY-MM-DD.
    val date: String,
    val outfitId: String,
    val status: String,
)

// Plans belong to a calendar day, so dates travel as YYYY-MM-DD rather than full timestamps.
fun parsePlanDate(value: String?): LocalDate {
    if (value.isNullOrBlank()) throw ValidationException("A date is required, in the form YYYY-MM-DD")
    return try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        throw ValidationException("'$value' is not a date in the form YYYY-MM-DD")
    }
}

// Keeps one request from asking for years of plans at once.
fun validatePlanRange(from: LocalDate, to: LocalDate) {
    if (to.isBefore(from)) throw ValidationException("to must be on or after from")
    if (ChronoUnit.DAYS.between(from, to) > MAX_PLAN_RANGE_DAYS) {
        throw ValidationException("A range can cover at most $MAX_PLAN_RANGE_DAYS days")
    }
}

const val MAX_PLAN_RANGE_DAYS = 62L
