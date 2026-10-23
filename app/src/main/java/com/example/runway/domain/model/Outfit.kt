package com.example.runway.domain.model

// A saved outfit: the garments worn together, and where they sit on the canvas (IIE, 2026).

data class Outfit(
    val id: String = "",
    val name: String,
    // The garments in the outfit, in the order they were put on.
    val itemIds: List<String> = emptyList(),
    val updatedAt: String? = null,
    val occasion: String? = null,
    // Where each garment sits on the builder canvas. itemIds still decides what is in the outfit.
    val layers: List<OutfitLayer> = emptyList(),
) {
    val garmentCount: Int get() = itemIds.size
}

// One garment on the canvas. x and y are the centre as a fraction of the canvas, so it fits any screen.
data class OutfitLayer(
    val itemId: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val z: Int = 0,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
