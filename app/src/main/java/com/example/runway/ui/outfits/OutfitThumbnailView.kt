package com.example.runway.ui.outfits

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitCanvas
import com.example.runway.ui.components.OutfitCanvasView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// A small preview of an outfit, drawn from the garments laid out in the builder.
class OutfitThumbnailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val collage = OutfitCanvasView(context)

    private var job: Job? = null
    // Stops a recycled list row from reloading the same pictures on every update.
    private var shownKey: String? = null

    init {
        addView(collage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bind(outfit: Outfit?, itemsById: Map<String, Item>, scope: CoroutineScope) {
        val key = outfit?.let { "${it.id}|${it.updatedAt}|${itemsById.keys.hashCode()}" }
        if (key == shownKey) return
        shownKey = key
        job?.cancel()

        collage.images = emptyMap()
        if (outfit == null) {
            collage.layers = emptyList()
            return
        }

        collage.layers = OutfitCanvas.autoLayout(OutfitCanvas.layersFor(outfit))

        val px = maxOf(width, MIN_DECODE_PX)
        job = scope.launch {
            collage.images = GarmentImages.load(outfit.itemIds, itemsById, px / 2)
        }
    }

    private companion object {
        const val MIN_DECODE_PX = 240
    }
}
