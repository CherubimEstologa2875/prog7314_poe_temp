package com.example.runway.data.mapper

import com.example.runway.data.local.ItemEntity
import com.example.runway.data.remote.api.ItemDto
import com.example.runway.domain.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

// SCRUM-145: converting a garment between the API, Room and the app.
class ItemMappersTest {

    private fun entity(price: Double?, wears: Long) = ItemEntity(
        id = "a", name = "Shirt", category = "TOP", colour = "Navy", brand = null, size = "M",
        purchasePrice = price, imagePath = null, wearCount = wears, wearLimit = 3, needsWash = false,
        archived = false, deleted = false, updatedAt = 10L, createdAt = 5L, pendingSync = false,
    )

    @Test
    fun `cost per wear is price over wears, rounded to cents`() {
        assertEquals(33.33, entity(price = 100.0, wears = 3).toDomain().costPerWear!!, 0.0001)
    }

    @Test
    fun `cost per wear is blank before the first wear or without a price`() {
        assertNull(entity(price = 100.0, wears = 0).toDomain().costPerWear)
        assertNull(entity(price = null, wears = 4).toDomain().costPerWear)
    }

    @Test
    fun `a row from the server is already synced and keeps this phone's photo`() {
        val dto = ItemDto(
            id = "a", name = "Shirt", category = "TOP", wearLimit = 5, needsWash = true,
            createdAt = "2026-09-01T10:00:00Z", updatedAt = "2026-09-02T10:00:00Z",
        )

        val row = dto.toEntity(existingImagePath = "/photos/a.png")

        assertFalse(row.pendingSync)
        assertEquals("/photos/a.png", row.imagePath)
        assertEquals(5, row.wearLimit)
        assertTrue(row.needsWash)
        assertEquals(Instant.parse("2026-09-01T10:00:00Z").toEpochMilli(), row.createdAt)
        assertEquals(Instant.parse("2026-09-02T10:00:00Z").toEpochMilli(), row.updatedAt)
    }

    @Test
    fun `a bad or missing timestamp becomes zero rather than crashing`() {
        val row = ItemDto(id = "a", name = "Shirt", category = "TOP", updatedAt = "yesterday").toEntity()
        assertEquals(0L, row.updatedAt)
        assertEquals(0L, row.createdAt)
    }

    @Test
    fun `a brand new item takes its save time as its creation time`() {
        val row = Item(name = "Shirt", category = "TOP").toEntity(updatedAt = 1234L)
        assertEquals(1234L, row.createdAt)
        assertTrue(row.pendingSync)
    }

    @Test
    fun `an existing item keeps its original creation time`() {
        val row = Item(name = "Shirt", category = "TOP", createdAt = 99L).toEntity(updatedAt = 1234L)
        assertEquals(99L, row.createdAt)
    }

    @Test
    fun `create and update both carry the wear limit`() {
        val row = entity(price = 200.0, wears = 1)
        assertEquals(3, row.toCreateDto().wearLimit)
        assertEquals(3, row.toUpdateDto().wearLimit)
        assertEquals("Navy", row.toCreateDto().colour)
    }
}
