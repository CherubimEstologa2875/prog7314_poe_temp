package com.example.runway.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// Room database definition for the Runway app.

@Database(
    entities = [ItemEntity::class],
    // 3: items gained imagePath. 4: wearLimit, needsWash and createdAt.
    version = 4,
    exportSchema = true
)
abstract class RunwayDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "runway.db"
    }
}
