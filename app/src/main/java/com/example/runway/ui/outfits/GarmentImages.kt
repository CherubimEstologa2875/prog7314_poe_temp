package com.example.runway.ui.outfits

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.runway.domain.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Decodes garment photos for the outfit canvas, shrunk to what's needed and cached so lists don't redo it.
object GarmentImages {

    // Measured in kilobytes. Big enough for a screen of outfit cards.
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    // Returns a photo per item id. Items without a photo, or whose photo won't open, map to null.
    suspend fun load(itemIds: List<String>, items: Map<String, Item>, maxPx: Int): Map<String, Bitmap?> =
        withContext(Dispatchers.IO) {
            itemIds.associateWith { id -> items[id]?.imagePath?.let { decode(it, maxPx) } }
        }

    private fun decode(path: String, maxPx: Int): Bitmap? {
        val key = "$path@$maxPx"
        cache.get(key)?.let { return it }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        cache.put(key, bitmap)
        return bitmap
    }
}
