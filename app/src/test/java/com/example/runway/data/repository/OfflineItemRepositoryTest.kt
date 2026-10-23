package com.example.runway.data.repository

import com.example.runway.data.local.ItemEntity
import com.example.runway.data.remote.api.ItemDto
import com.example.runway.data.remote.api.PageDto
import com.example.runway.domain.model.Item
import com.example.runway.fake.FakeItemDao
import com.example.runway.fake.FakeRunwayApi
import com.example.runway.fake.FakeSyncStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// SCRUM-145: the offline-first sync between Room and the API.
class OfflineItemRepositoryTest {

    private val dao = FakeItemDao()
    private val api = FakeRunwayApi()
    private val sync = FakeSyncStore()
    private var uid: String? = "uid-1"

    private val repository = OfflineItemRepository(dao, api, sync) { uid }

    private fun row(id: String, pending: Boolean = false, deleted: Boolean = false, imagePath: String? = null) =
        ItemEntity(
            id = id, name = id, category = "TOP", colour = null, brand = null, size = null,
            purchasePrice = null, imagePath = imagePath, wearCount = 0, wearLimit = null, needsWash = false,
            archived = false, deleted = deleted, updatedAt = 1L, createdAt = 1L, pendingSync = pending,
        )

    private fun notFound() = HttpException(Response.error<Any>(404, "".toResponseBody(null)))

    @Test
    fun `saving offline keeps the item locally and queues it`() = runTest {
        api.createdItem = { throw IOException("offline") }

        repository.save(Item(name = "Linen shirt", category = "TOP"))

        val saved = dao.all.single()
        assertTrue(saved.id.startsWith("local-"))
        assertTrue(saved.pendingSync)
        assertEquals(1, repository.observePendingCount().first())
    }

    @Test
    fun `once the server accepts it, the temporary id is swapped and the photo kept`() = runTest {
        repository.save(Item(name = "Linen shirt", category = "TOP", imagePath = "/photos/shirt.png"))

        val saved = dao.all.single()
        assertEquals("server-1", saved.id)
        assertEquals("/photos/shirt.png", saved.imagePath)
        assertFalse(saved.pendingSync)
    }

    @Test
    fun `the wear limit is sent when an item is created`() = runTest {
        repository.save(Item(name = "Jeans", category = "BOTTOM", wearLimit = 7))
        assertEquals(7, api.createdItems.single().wearLimit)
    }

    @Test
    fun `editing a synced item updates it on the server`() = runTest {
        dao.put(row("server-9"))

        repository.save(Item(id = "server-9", name = "Renamed", category = "TOP"))

        assertEquals("server-9", api.updatedItems.single().first)
        assertEquals("Renamed", api.updatedItems.single().second.name)
        assertFalse(dao.row("server-9")!!.pendingSync)
    }

    @Test
    fun `deleting something the server never saw makes no API call`() = runTest {
        dao.put(row("local-abc", pending = true))

        repository.delete("local-abc")

        assertTrue(api.deletedItems.isEmpty())
        assertNull(dao.row("local-abc"))
    }

    @Test
    fun `deleting a synced item deletes it on the server too`() = runTest {
        dao.put(row("server-9"))

        repository.delete("server-9")

        assertEquals(listOf("server-9"), api.deletedItems)
        assertNull(dao.row("server-9"))
    }

    @Test
    fun `an item the server no longer has is dropped instead of retried forever`() = runTest {
        dao.put(row("server-9"))
        api.updateItemError = notFound()

        repository.save(Item(id = "server-9", name = "Gone", category = "TOP"))

        assertNull(dao.row("server-9"))
    }

    @Test
    fun `refresh pulls every page, removes deleted rows and keeps local photos`() = runTest {
        dao.put(row("keep-photo", imagePath = "/photos/a.png"), row("was-deleted"))
        sync.cursors["uid-1"] = "2026-09-01T00:00:00Z"
        api.itemPages = listOf(
            PageDto(
                data = listOf(
                    ItemDto(id = "keep-photo", name = "Shirt", category = "TOP", updatedAt = "2026-09-10T08:00:00Z"),
                    ItemDto(id = "was-deleted", name = "Old", category = "TOP", deleted = true, updatedAt = "2026-09-11T08:00:00Z"),
                ),
                nextCursor = "page-2",
            ),
            PageDto(data = listOf(ItemDto(id = "new", name = "Jeans", category = "BOTTOM", updatedAt = "2026-09-12T08:00:00Z"))),
        )

        assertTrue(repository.refresh().isSuccess)

        assertEquals(listOf(null, "page-2"), api.itemPageRequests.map { it.second })
        assertEquals("/photos/a.png", dao.row("keep-photo")?.imagePath)
        assertNull(dao.row("was-deleted"))
        assertEquals("Jeans", dao.row("new")?.name)
        // The next sync starts from the newest change it saw.
        assertEquals("2026-09-12T08:00:00Z", sync.cursors["uid-1"])
    }

    @Test
    fun `refresh only asks for changes since the last sync`() = runTest {
        dao.put(row("server-9"))
        sync.cursors["uid-1"] = "2026-09-01T00:00:00Z"

        repository.refresh()

        assertEquals("2026-09-01T00:00:00Z", api.itemPageRequests.single().first)
    }

    @Test
    fun `an empty table pulls everything, whatever the cursor says`() = runTest {
        // What happens after the local cache is rebuilt by a database upgrade.
        sync.cursors["uid-1"] = "2026-09-01T00:00:00Z"

        repository.refresh()

        assertNull(api.itemPageRequests.single().first)
    }

    @Test
    fun `queued changes are sent before pulling`() = runTest {
        dao.put(row("local-abc", pending = true))

        repository.refresh()

        assertEquals(1, api.createdItems.size)
        assertEquals(0, repository.observePendingCount().first())
    }

    @Test
    fun `refresh fails cleanly when nobody is signed in`() = runTest {
        uid = null
        assertTrue(repository.refresh().isFailure)
        assertTrue(api.itemPageRequests.isEmpty())
    }
}
