package com.example.runway.data.image

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await

// Separates the garment from its background (IIE, 2026; Google, 2026b).
// The model is downloaded by Google Play services rather than shipped in the APK,
// so the first attempt on a new device can fail while it arrives. That is why the
// screen always offers to keep the original photo instead.

class SubjectCutout {

    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
        )
    }

    // Returns the subject on a transparent background, or a failure the caller can
    // fall back from. Never throws into the UI.
    suspend fun cutOut(source: Bitmap): Result<Bitmap> = runCatching {
        val result = segmenter.process(InputImage.fromBitmap(source, 0)).await()
        val foreground = result.foregroundBitmap ?: error("No subject was found in the photo")
        trimTransparentEdges(foreground)
    }

    fun close() = segmenter.close()

    // The cut-out keeps the original photo's dimensions, so a shirt photographed
    // from a distance ends up mostly empty space. Cropping to the subject's own
    // bounds is what makes the preview look like the garment rather than the room.
    private fun trimTransparentEdges(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val row = IntArray(width)

        var top = height
        var bottom = -1
        var left = width
        var right = -1

        for (y in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                if (Color.alpha(row[x]) <= ALPHA_FLOOR) continue
                if (y < top) top = y
                if (y > bottom) bottom = y
                if (x < left) left = x
                if (x > right) right = x
            }
        }

        if (bottom < top || right < left) return bitmap

        val pad = (maxOf(right - left, bottom - top) * PADDING).toInt()
        val cropLeft = (left - pad).coerceAtLeast(0)
        val cropTop = (top - pad).coerceAtLeast(0)
        val cropRight = (right + pad).coerceAtMost(width - 1)
        val cropBottom = (bottom + pad).coerceAtMost(height - 1)

        return Bitmap.createBitmap(
            bitmap,
            cropLeft,
            cropTop,
            cropRight - cropLeft + 1,
            cropBottom - cropTop + 1,
        )
    }

    private companion object {
        // Anything fainter than this is the segmenter's soft edge, not the garment.
        const val ALPHA_FLOOR = 24
        const val PADDING = 0.03f
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026b. Subject segmentation. [online] Available at: <https://developers.google.com/ml-kit/vision/subject-segmentation> [Accessed 15 September 2026].
*/
