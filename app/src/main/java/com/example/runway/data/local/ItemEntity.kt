package com.example.runway.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entity for a stored garment.

// How a garment is stored on the device.
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val colour: String?,
    val brand: String?,
    val size: String?,
    val purchasePrice: Double?,
    // Absolute path to the cut-out photo on this device. Not part of the API yet,
    // so a row that arrives from a sync must not overwrite it with null.
    val imagePath: String?,
    val wearCount: Long,
    val wearLimit: Int?,
    val needsWash: Boolean,
    val archived: Boolean,
    val deleted: Boolean,
    // Epoch milliseconds.
    val updatedAt: Long,
    val createdAt: Long,
    val pendingSync: Boolean,
)
