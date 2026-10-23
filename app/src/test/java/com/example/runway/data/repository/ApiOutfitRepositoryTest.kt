package com.example.runway.data.repository

import com.example.runway.data.remote.api.OutfitDto
import com.example.runway.data.remote.api.OutfitItemDto
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitLayer
import com.example.runway.fake.FakeRunwayApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// SCRUM-145: outfit layouts surviving the trip to the API and back.
class ApiOutfitRepositoryTest {

    private val api = FakeRunwayApi()
    private val repository = ApiOutfitRepository(api)

    @Test
    fun `a builder layout is sent with every position`() = runTest {
        repository.create(
            Outfit(
                name = "Friday dinner",
                itemIds = listOf("shirt", "jeans"),
                occasion = "Evening",
                layers = listOf(
                    OutfitLayer("shirt", x = 0.3f, y = 0.25f, scale = 1.2f, rotation = -10f, z = 0),
                    OutfitLayer("jeans", x = 0.5f, y = 0.7f, z = 1),
                ),
            )
        )

        val sent = api.createdOutfit!!
        assertEquals("Evening", sent.occasion)
        val shirt = sent.items.first { it.clothingItemId == "shirt" }
        assertEquals(0.3, shirt.x, 0.001)
        assertEquals(1.2, shirt.scale, 0.001)
        assertEquals(-10.0, shirt.rotation, 0.001)
    }

    @Test
    fun `layers come back bottom to top`() = runTest {
        api.outfits = listOf(
            OutfitDto(
                id = "o1",
                name = "Friday",
                items = listOf(
                    OutfitItemDto(clothingItemId = "jacket", zIndex = 2, x = 0.5, y = 0.4),
                    OutfitItemDto(clothingItemId = "shirt", zIndex = 0, x = 0.5, y = 0.3),
                ),
            )
        )

        val outfit = repository.get("o1").getOrThrow()

        assertEquals(listOf("shirt", "jacket"), outfit.itemIds)
        assertEquals(listOf(0, 2), outfit.layers.map { it.z })
        assertEquals(0.4f, outfit.layers.last().y)
    }

    @Test
    fun `a garment taken out on the detail screen leaves no stray layer behind`() = runTest {
        val outfit = Outfit(
            id = "o1",
            name = "Friday",
            // Jeans were removed, but its layer is still on the object.
            itemIds = listOf("shirt"),
            layers = listOf(OutfitLayer("shirt", z = 0), OutfitLayer("jeans", z = 1)),
        )

        repository.update(outfit)

        assertEquals(listOf("shirt"), api.updatedOutfit!!.second.items!!.map { it.clothingItemId })
    }

    @Test
    fun `outfits from the model screen still save in wearing order`() = runTest {
        repository.create(Outfit(name = "From model", itemIds = listOf("shirt", "jeans", "shoes")))

        assertEquals(listOf(0L, 1L, 2L), api.createdOutfit!!.items.map { it.zIndex })
    }
}
