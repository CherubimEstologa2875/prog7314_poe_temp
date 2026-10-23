package com.example

import com.google.cloud.Timestamp

// Cloud Firestore document classes for the Runway data model (IIE, 2026).

data class UserDocument(
    var uid: String = "",
    var displayName: String = "",
    var email: String = "",
    var photoUrl: String? = null,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)

// users/{uid}/settings/preferences - one fixed document per user.
data class UserSettingsDocument(
    var theme: String = "SYSTEM",
    var language: String = "en",
    var units: String = "METRIC",
    var notificationsEnabled: Boolean = true,
    var biometricEnabled: Boolean = false,
    var defaultWearLimit: Long = DEFAULT_WEAR_LIMIT,
    var updatedAt: Timestamp? = null,
)

// clothingItems/{itemId} - a root collection, because items are filtered and listed across a whole wardrobe.
data class ClothingItemDocument(
    var id: String = "",
    var ownerUid: String = "",
    var name: String = "",
    var category: String = "",
    var colour: String? = null,
    var brand: String? = null,
    var size: String? = null,
    var purchasePrice: Double? = null,
    var purchaseDate: Timestamp? = null,
    var wearCount: Long = 0,
    // Wears before the garment is due for a wash. Copied from the owner's
    // settings when the item is created, so changing the default later does not
    // silently re-rule every existing garment.
    var wearLimit: Long = DEFAULT_WEAR_LIMIT,
    var archived: Boolean = false,
    var deleted: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)

// clothingItems/{itemId}/images/{imageId} - a subcollection, because images are only ever read with their item.
data class ClothingImageDocument(
    var id: String = "",
    var storagePath: String = "",
    var primary: Boolean = false,
    var width: Long = 0,
    var height: Long = 0,
    var uploadedAt: Timestamp? = null,
)

data class OutfitDocument(
    var id: String = "",
    var ownerUid: String = "",
    var name: String = "",
    var occasion: String? = null,
    var season: String? = null,
    var deleted: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)

// outfits/{outfitId}/items/{id} - the placement of one garment on the outfit canvas.
data class OutfitItemDocument(
    var id: String = "",
    var clothingItemId: String = "",
    var x: Double = 0.0,
    var y: Double = 0.0,
    var scale: Double = 1.0,
    var rotation: Double = 0.0,
    var zIndex: Long = 0,
)

// outfitPlans/{planId} - one planned outfit for one date.
data class OutfitPlanDocument(
    var id: String = "",
    var ownerUid: String = "",
    var date: Timestamp? = null,
    var outfitId: String = "",
    var weatherSummary: String? = null,
    var status: String = "PLANNED",
    var deleted: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)

// wearHistory/{entryId} - one record of a garment being worn; drives cost-per-wear.
data class WearHistoryDocument(
    var id: String = "",
    var ownerUid: String = "",
    var clothingItemId: String = "",
    var outfitId: String? = null,
    var wornOn: Timestamp? = null,
)

// confidenceRatings/{id} - how the user felt in an outfit, 1 to 5.
data class ConfidenceRatingDocument(
    var id: String = "",
    var ownerUid: String = "",
    var outfitId: String = "",
    var score: Long = 0,
    var note: String? = null,
    var ratedAt: Timestamp? = null,
)

// listings/{listingId} - a garment offered for swap or donation.
data class ListingDocument(
    var id: String = "",
    var ownerUid: String = "",
    var clothingItemId: String = "",
    var type: String = "SWAP",
    var status: String = "OPEN",
    var description: String? = null,
    var deleted: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)

// listingRequests/{requestId} - someone asking for a listed garment.
data class ListingRequestDocument(
    var id: String = "",
    var listingId: String = "",
    var listingOwnerUid: String = "",
    var requesterUid: String = "",
    var message: String? = null,
    var status: String = "PENDING",
    var createdAt: Timestamp? = null,
)

// Collection names in one place, so a typo is a compile error rather than an empty result set at run time.
object Collections {
    const val USERS = "users"
    const val SETTINGS = "settings"
    const val SETTINGS_DOC = "preferences"
    const val CLOTHING_ITEMS = "clothingItems"
    const val IMAGES = "images"
    const val OUTFITS = "outfits"
    const val OUTFIT_ITEMS = "items"
    const val OUTFIT_PLANS = "outfitPlans"
    const val WEAR_HISTORY = "wearHistory"
    const val CONFIDENCE_RATINGS = "confidenceRatings"
    const val LISTINGS = "listings"
    const val LISTING_REQUESTS = "listingRequests"
}

// Field names used in queries.
// Wears before a garment needs washing, when neither the item nor the user's
// settings specify one.
// TODO: confirm this value against the original - see the note in the chat.
const val DEFAULT_WEAR_LIMIT: Long = 3

object Fields {
    const val OWNER_UID = "ownerUid"
    const val CATEGORY = "category"
    const val DELETED = "deleted"
    const val ARCHIVED = "archived"
    const val UPDATED_AT = "updatedAt"
    const val CREATED_AT = "createdAt"
    const val WEAR_COUNT = "wearCount"
    const val WEAR_LIMIT = "wearLimit"
    const val STATUS = "status"
    const val LISTING_ID = "listingId"
    const val OUTFIT_ID = "outfitId"
    const val RATED_AT = "ratedAt"
    const val DATE = "date"
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
