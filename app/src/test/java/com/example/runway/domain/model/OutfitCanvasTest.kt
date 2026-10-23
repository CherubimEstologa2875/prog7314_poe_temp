package com.example.runway.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// SCRUM-142 and 146: the builder canvas never lets a garment end up somewhere it can't be seen or grabbed.
class OutfitCanvasTest {

    private fun layer(id: String, z: Int = 0) = OutfitLayer(itemId = id, z = z)

    @Test
    fun `adding puts the new garment on top`() {
        val layers = OutfitCanvas.add(OutfitCanvas.add(emptyList(), "shirt"), "jeans")

        assertEquals(listOf("shirt", "jeans"), OutfitCanvas.drawOrder(layers).map { it.itemId })
    }

    @Test
    fun `adding the same garment twice changes nothing`() {
        val once = OutfitCanvas.add(emptyList(), "shirt")
        assertSame(once, OutfitCanvas.add(once, "shirt"))
    }

    @Test
    fun `a garment can't be dragged off the canvas`() {
        val layers = OutfitCanvas.moveTo(listOf(layer("shirt")), "shirt", x = -2f, y = 5f)
        assertEquals(OutfitCanvas.MIN_POSITION, layers[0].x)
        assertEquals(OutfitCanvas.MAX_POSITION, layers[0].y)
    }

    @Test
    fun `size and rotation stay inside the slider ranges`() {
        var layers = listOf(layer("shirt"))
        layers = OutfitCanvas.setScale(layers, "shirt", 10f)
        layers = OutfitCanvas.setRotation(layers, "shirt", -300f)

        assertEquals(OutfitCanvas.MAX_SCALE, layers[0].scale)
        assertEquals(OutfitCanvas.MIN_ROTATION, layers[0].rotation)
    }

    @Test
    fun `bring forward swaps with the garment above`() {
        val layers = listOf(layer("shirt", 0), layer("jacket", 1), layer("scarf", 2))
        val moved = OutfitCanvas.bringForward(layers, "shirt")

        assertEquals(listOf("jacket", "shirt", "scarf"), OutfitCanvas.drawOrder(moved).map { it.itemId })
    }

    @Test
    fun `the top garment can't go any higher, the bottom one any lower`() {
        val layers = listOf(layer("shirt", 0), layer("jacket", 1))
        assertEquals(layers, OutfitCanvas.bringForward(layers, "jacket"))
        assertEquals(layers, OutfitCanvas.sendBackward(layers, "shirt"))
    }

    @Test
    fun `remove takes only that garment off`() {
        val layers = OutfitCanvas.remove(listOf(layer("shirt"), layer("jeans")), "shirt")
        assertEquals(listOf("jeans"), layers.map { it.itemId })
    }

    @Test
    fun `outfits from the model screen are spread out instead of piled at the corner`() {
        val unplaced = listOf(
            OutfitLayer("shirt", x = 0f, y = 0f, z = 0),
            OutfitLayer("jeans", x = 0f, y = 0f, z = 1),
            OutfitLayer("shoes", x = 0f, y = 0f, z = 2),
        )
        val laidOut = OutfitCanvas.autoLayout(unplaced)

        assertTrue(laidOut.all { it.x == 0.5f })
        assertEquals(laidOut.map { it.y }.distinct().size, 3)
        assertTrue(laidOut[0].y < laidOut[2].y)
    }

    @Test
    fun `a layout the user made is left alone`() {
        val placed = listOf(OutfitLayer("shirt", x = 0.3f, y = 0.4f))
        assertSame(placed, OutfitCanvas.autoLayout(placed))
    }

    @Test
    fun `every garment gets a layer and saved positions are kept`() {
        val outfit = Outfit(
            name = "Friday",
            itemIds = listOf("shirt", "jeans"),
            layers = listOf(OutfitLayer("shirt", x = 0.2f, y = 0.3f, z = 4)),
        )
        val layers = OutfitCanvas.layersFor(outfit)

        assertEquals(0.2f, layers.first { it.itemId == "shirt" }.x)
        assertEquals(listOf("shirt", "jeans"), layers.map { it.itemId })
    }
}
