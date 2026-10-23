package com.example.runway.domain.model

// Domain model for a wardrobe garment.

// A garment in the user's wardrobe - the model everything above the data layer reasons about.
data class Item(
    val id: String = "",
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    // Absolute path to the garment's photo on this device, once one is taken.
    val imagePath: String? = null,
    val wearCount: Long = 0,
    // How many wears before it needs a wash. Null means use the default from settings.
    val wearLimit: Int? = null,
    // Worked out by the API from wearCount and wearLimit.
    val needsWash: Boolean = false,
    // purchasePrice / wearCount.
    val costPerWear: Double? = null,
    val archived: Boolean = false,
    // Epoch milliseconds of the last change, local or remote.
    val updatedAt: Long = 0L,
    // Epoch milliseconds of when it was first added.
    val createdAt: Long = 0L,
    // True when this item has local changes the server has not accepted yet.
    val pendingSync: Boolean = false,
)
