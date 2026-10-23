package com.example.runway.domain.model

// The rules for moving garments around the outfit builder canvas, kept apart from the view so they can be tested.
object OutfitCanvas {

    const val MIN_POSITION = 0.05f
    const val MAX_POSITION = 0.95f
    const val MIN_SCALE = 0.4f
    const val MAX_SCALE = 1.9f
    const val MIN_ROTATION = -45f
    const val MAX_ROTATION = 45f

    // Adds a garment in the middle, on top. Adding one that's already there changes nothing.
    fun add(layers: List<OutfitLayer>, itemId: String): List<OutfitLayer> {
        if (layers.any { it.itemId == itemId }) return layers
        val top = (layers.maxOfOrNull { it.z } ?: -1) + 1
        return layers + OutfitLayer(itemId = itemId, x = 0.5f, y = 0.45f, z = top)
    }

    fun remove(layers: List<OutfitLayer>, itemId: String): List<OutfitLayer> =
        layers.filterNot { it.itemId == itemId }

    // Stops a garment being dragged completely off the canvas.
    fun moveTo(layers: List<OutfitLayer>, itemId: String, x: Float, y: Float): List<OutfitLayer> =
        update(layers, itemId) {
            it.copy(x = x.coerceIn(MIN_POSITION, MAX_POSITION), y = y.coerceIn(MIN_POSITION, MAX_POSITION))
        }

    fun setScale(layers: List<OutfitLayer>, itemId: String, scale: Float): List<OutfitLayer> =
        update(layers, itemId) { it.copy(scale = scale.coerceIn(MIN_SCALE, MAX_SCALE)) }

    fun setRotation(layers: List<OutfitLayer>, itemId: String, degrees: Float): List<OutfitLayer> =
        update(layers, itemId) { it.copy(rotation = degrees.coerceIn(MIN_ROTATION, MAX_ROTATION)) }

    fun bringForward(layers: List<OutfitLayer>, itemId: String) = swapWithNeighbour(layers, itemId, 1)

    fun sendBackward(layers: List<OutfitLayer>, itemId: String) = swapWithNeighbour(layers, itemId, -1)

    // Bottom to top, the order they should be drawn in.
    fun drawOrder(layers: List<OutfitLayer>): List<OutfitLayer> = layers.sortedBy { it.z }

    // Outfits saved from the model screen have no positions, so they are stacked top to bottom instead.
    fun autoLayout(layers: List<OutfitLayer>): List<OutfitLayer> {
        val unplaced = layers.isNotEmpty() && layers.all { it.x == 0f && it.y == 0f }
        if (!unplaced) return layers

        val step = if (layers.size > 1) 0.6f / (layers.size - 1) else 0f
        return drawOrder(layers).mapIndexed { index, layer ->
            layer.copy(x = 0.5f, y = if (layers.size == 1) 0.5f else 0.2f + index * step, scale = 1f, rotation = 0f)
        }
    }

    // Gives every garment in the outfit a layer, keeping any positions already saved.
    fun layersFor(outfit: Outfit): List<OutfitLayer> {
        val saved = outfit.layers.associateBy { it.itemId }
        return outfit.itemIds.mapIndexed { index, id -> saved[id] ?: OutfitLayer(itemId = id, x = 0f, y = 0f, z = index) }
    }

    private fun swapWithNeighbour(layers: List<OutfitLayer>, itemId: String, direction: Int): List<OutfitLayer> {
        val ordered = drawOrder(layers)
        val index = ordered.indexOfFirst { it.itemId == itemId }
        val other = index + direction
        if (index < 0 || other !in ordered.indices) return layers

        val a = ordered[index]
        val b = ordered[other]
        return layers.map {
            when (it.itemId) {
                a.itemId -> it.copy(z = b.z)
                b.itemId -> it.copy(z = a.z)
                else -> it
            }
        }
    }

    private fun update(layers: List<OutfitLayer>, itemId: String, change: (OutfitLayer) -> OutfitLayer) =
        layers.map { if (it.itemId == itemId) change(it) else it }
}
