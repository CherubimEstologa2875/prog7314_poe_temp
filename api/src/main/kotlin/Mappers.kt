package com.example

// Conversions between Firestore documents and API DTOs (IIE, 2026; Freeman, 2019).
// Data classes are serialised with kotlinx.serialization (JetBrains, 2026).

fun ClothingItemDocument.toResponse(): ItemResponse = ItemResponse(
    id = id,
    name = name,
    category = category,
    colour = colour,
    brand = brand,
    size = size,
    purchasePrice = purchasePrice,
    wearCount = wearCount,
    wearLimit = wearLimit,
    costPerWear = costPerWear(purchasePrice, wearCount),
    needsWash = needsWash(wearCount, wearLimit),
    archived = archived,
    deleted = deleted,
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
)

fun OutfitDocument.toResponse(items: List<OutfitItemDocument>): OutfitResponse = OutfitResponse(
    id = id,
    name = name,
    occasion = occasion,
    season = season,
    items = items.map { it.toDto() },
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
)

fun OutfitItemDocument.toDto(): OutfitItemDto = OutfitItemDto(
    id = id,
    clothingItemId = clothingItemId,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    zIndex = zIndex,
)

fun OutfitItemDto.toDocument(generatedId: String): OutfitItemDocument = OutfitItemDocument(
    id = generatedId,
    clothingItemId = clothingItemId,
    x = x,
    y = y,
    scale = scale,
    rotation = rotation,
    zIndex = zIndex,
)

fun ConfidenceRatingDocument.toResponse(): RatingResponse = RatingResponse(
    id = id,
    outfitId = outfitId,
    score = score,
    note = note,
    ratedAt = ratedAt.toIso(),
)

fun UserDocument.toResponse(): UserResponse = UserResponse(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    createdAt = createdAt.toIso(),
)

fun UserSettingsDocument.toDto(): SettingsDto = SettingsDto(
    theme = theme,
    language = language,
    units = units,
    notificationsEnabled = notificationsEnabled,
    biometricEnabled = biometricEnabled,
    defaultWearLimit = defaultWearLimit.toInt(),
)

fun SettingsDto.toDocument(): UserSettingsDocument = UserSettingsDocument(
    theme = theme,
    language = language,
    units = units,
    notificationsEnabled = notificationsEnabled,
    biometricEnabled = biometricEnabled,
    defaultWearLimit = defaultWearLimit.toLong(),
)

// Stored as midnight UTC, so reading it back in UTC gives the same calendar day.
fun OutfitPlanDocument.toResponse(): PlanResponse = PlanResponse(
    id = id,
    date = date?.toDate()?.toInstant()?.atZone(java.time.ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty(),
    outfitId = outfitId,
    status = status,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Freeman, J., 2019. What is JSON? A better format for data exchange. [online] Available at: <https://www.infoworld.com/article/3222851/what-is-json-a-better-format-for-data-exchange.html> [Accessed 31 July 2023].
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
*/
