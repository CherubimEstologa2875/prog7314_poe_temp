package com.example.runway.ui.navigation

// Argument names used by the nav graph, kept in one place so they cannot drift apart.
object NavArgs {

    /** The wardrobe item a detail, edit, matcher or listing destination is showing. */
    const val ITEM_ID = "itemId"

    /** The outfit an outfit-detail or sheet destination is showing. */
    const val OUTFIT_ID = "outfitId"

    /** The swap/donation listing a listing-detail destination is showing. */
    const val LISTING_ID = "listingId"

    /** ISO date (yyyy-MM-dd) a planner day-detail sheet is showing. */
    const val DATE_ISO = "dateISO"
}
