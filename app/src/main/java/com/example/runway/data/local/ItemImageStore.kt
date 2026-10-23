package com.example.runway.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Garment photos on this device (IIE, 2026).
// Files are named with a fresh id at capture time rather than with the item's id,
// because an item's id changes when the server accepts it: naming by capture id
// means the stored path stays correct through that swap.

class ItemImageStore(context: Context) {

    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DIRECTORY).apply { mkdirs() }

    // A file the camera can write into. Handed to the camera as a content URI.
    fun newCaptureFile(): File {
        directory.mkdirs()
        return File(directory, "capture-${UUID.randomUUID()}.jpg")
    }

    // PNG, because a cut-out garment has transparent edges that JPEG would fill in.
    suspend fun saveCutout(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val file = File(directory, "item-${UUID.randomUUID()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }

    suspend fun load(path: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    // Reads an image the user picked or the camera wrote, scaled down to something
    // the segmenter and the screen can both work with.
    suspend fun decode(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val sizeStream = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
        sizeStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val pixelStream = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
        val decoded = pixelStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotation = readRotation(uri)
        if (rotation == 0) decoded else rotate(decoded, rotation)
    }

    // A camera records which way up it was held instead of rotating the pixels, so
    // a portrait photo arrives sideways unless this is applied.
    private fun readRotation(uri: Uri): Int {
        val stream = appContext.contentResolver.openInputStream(uri) ?: return 0
        val orientation = stream.use {
            runCatching {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > MAX_DIMENSION) sample *= 2
        return sample
    }

    private companion object {
        const val DIRECTORY = "items"
        const val MAX_DIMENSION = 1280
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
