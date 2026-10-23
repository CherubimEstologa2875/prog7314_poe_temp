package com.example

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Unit tests for the Runway REST API (IIE, 2026).
// Built on the Ktor server framework (Ktor, 2026).

class ServerTest {
    @Test
    fun `swagger definition is served without authentication`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/swagger/documentation.yaml")
        assertEquals(HttpStatusCode.OK, response.status)
        val definition = response.bodyAsText()
        assertTrue(definition.contains("openapi: 3.0.3"))
        assertTrue(definition.contains("/api/v1/users/me/settings:"))
        assertTrue(definition.contains("- url: /"))
        assertFalse(definition.contains("https://runway-api.onrender.com"))
    }

    @Test
    fun `create rejects a blank name`() {
        val body = CreateItemRequest(name = "   ", category = "TOP")
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `create rejects a negative price`() {
        val body = CreateItemRequest(name = "Coat", category = "OUTERWEAR", purchasePrice = -1.0)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `create accepts a valid item`() {
        CreateItemRequest(name = "Black wool coat", category = "OUTERWEAR", purchasePrice = 1299.0)
            .validate()
    }

    @Test
    fun `update rejects an empty patch`() {
        // A PATCH with no fields is a request that cannot mean anything.
        assertFailsWith<ValidationException> { UpdateItemRequest().validate() }
    }

    @Test
    fun `settings reject an unknown theme`() {
        assertFailsWith<ValidationException> { SettingsDto(theme = "NEON").validate() }
    }

    @Test
    fun `cost per wear divides price by wears and rounds to cents`() {
        assertEquals(92.79, costPerWear(1299.0, 14))
    }

    @Test
    fun `cost per wear is undefined before the first wear`() {
        assertNull(costPerWear(1299.0, 0))
    }

    @Test
    fun `cost per wear is undefined when no price was recorded`() {
        assertNull(costPerWear(null, 5))
    }

    @Test
    fun `an item may not have a wear limit of zero`() {
        val body = CreateItemRequest(name = "Jeans", category = "BOTTOM", wearLimit = 0)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `an item may not have a wear limit above the maximum`() {
        val body = CreateItemRequest(name = "Jeans", category = "BOTTOM", wearLimit = 61)
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `leaving the wear limit out is allowed`() {
        // Means "use the limit from the user's settings".
        CreateItemRequest(name = "Jeans", category = "BOTTOM").validate()
    }

    @Test
    fun `settings reject a wear limit outside the allowed range`() {
        assertFailsWith<ValidationException> { SettingsDto(defaultWearLimit = 0).validate() }
        assertFailsWith<ValidationException> { SettingsDto(defaultWearLimit = 99).validate() }
    }

    @Test
    fun `an item needs a wash once it reaches its limit`() {
        assertFalse(needsWash(wearCount = 2, wearLimit = 3))
        assertTrue(needsWash(wearCount = 3, wearLimit = 3))
        assertTrue(needsWash(wearCount = 4, wearLimit = 3))
    }

    @Test
    fun `an item with no limit never needs a wash`() {
        assertFalse(needsWash(wearCount = 40, wearLimit = 0))
    }

    @Test
    fun `an outfit patch rejects a blank name`() {
        assertFailsWith<ValidationException> { UpdateOutfitRequest(name = "  ").validate() }
    }

    @Test
    fun `an outfit patch rejects a placement with no item`() {
        val body = UpdateOutfitRequest(items = listOf(OutfitItemDto(clothingItemId = "")))
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `an outfit patch with no fields is rejected`() {
        assertFailsWith<ValidationException> { UpdateOutfitRequest().validate() }
    }

    @Test
    fun `an outfit patch accepts a rename`() {
        UpdateOutfitRequest(name = "Friday dinner").validate()
    }

    @Test
    fun `an outfit patch accepts replacing the placements`() {
        UpdateOutfitRequest(items = listOf(OutfitItemDto(clothingItemId = "item-1"))).validate()
    }

    @Test
    fun `a rating below one is rejected`() {
        assertFailsWith<ValidationException> { CreateRatingRequest(score = 0).validate() }
    }

    @Test
    fun `a rating above five is rejected`() {
        assertFailsWith<ValidationException> { CreateRatingRequest(score = 6).validate() }
    }

    @Test
    fun `every score from one to five is accepted`() {
        (1..5).forEach { CreateRatingRequest(score = it).validate() }
    }

    @Test
    fun `a rating note has a length limit`() {
        assertFailsWith<ValidationException> {
            CreateRatingRequest(score = 4, note = "x".repeat(281)).validate()
        }
    }

    @Test
    fun `a rating may be left without a note`() {
        CreateRatingRequest(score = 4).validate()
    }

    @Test
    fun `the average score is rounded to one decimal place`() {
        assertEquals(4.3, averageScore(listOf(5L, 4L, 4L)))
    }

    @Test
    fun `an outfit with no ratings has no average`() {
        // Null rather than zero: never rated is not the same as rated badly.
        assertNull(averageScore(emptyList()))
    }

    @Test
    fun `health check needs no token`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // SCRUM-142: input validation added for the planner and the outfit builder.

    @Test
    fun `an item name over the limit is rejected`() {
        val body = CreateItemRequest(name = "a".repeat(121), category = "TOP")
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `an item needs a category`() {
        assertFailsWith<ValidationException> { CreateItemRequest(name = "Coat", category = " ").validate() }
    }

    @Test
    fun `a new outfit needs a name`() {
        assertFailsWith<ValidationException> { CreateOutfitRequest(name = "  ").validate() }
    }

    @Test
    fun `every garment on a new outfit needs an item id`() {
        val body = CreateOutfitRequest(name = "Friday", items = listOf(OutfitItemDto(clothingItemId = "")))
        assertFailsWith<ValidationException> { body.validate() }
    }

    @Test
    fun `a new outfit with its canvas layout is accepted`() {
        CreateOutfitRequest(
            name = "Friday",
            occasion = "Evening",
            items = listOf(OutfitItemDto(clothingItemId = "shirt", x = 0.3, y = 0.4, scale = 1.2, rotation = -10.0)),
        ).validate()
    }

    @Test
    fun `plan dates must be YYYY-MM-DD`() {
        assertEquals(java.time.LocalDate.of(2026, 9, 22), parsePlanDate("2026-09-22"))
        assertFailsWith<ValidationException> { parsePlanDate("22/09/2026") }
        assertFailsWith<ValidationException> { parsePlanDate("2026-9-2") }
        assertFailsWith<ValidationException> { parsePlanDate("2026-02-30") }
        assertFailsWith<ValidationException> { parsePlanDate(null) }
    }

    @Test
    fun `a plan needs an outfit`() {
        assertFailsWith<ValidationException> { SetPlanRequest(outfitId = " ").validate() }
        SetPlanRequest(outfitId = "o1").validate()
    }

    @Test
    fun `a plan range can't run backwards`() {
        val from = java.time.LocalDate.of(2026, 9, 22)
        assertFailsWith<ValidationException> { validatePlanRange(from, from.minusDays(1)) }
    }

    @Test
    fun `a plan range is capped so one call can't ask for years`() {
        val from = java.time.LocalDate.of(2026, 9, 1)
        validatePlanRange(from, from.plusDays(MAX_PLAN_RANGE_DAYS))
        assertFailsWith<ValidationException> { validatePlanRange(from, from.plusDays(MAX_PLAN_RANGE_DAYS + 1)) }
    }

    @Test
    fun `planner endpoints need a token`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/api/v1/plans?from=2026-09-01&to=2026-09-30")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `the planner is in the API docs`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val definition = client.get("/swagger/documentation.yaml").bodyAsText()
        assertTrue(definition.contains("/api/v1/plans:"))
        assertTrue(definition.contains("/api/v1/plans/{date}:"))
    }
}


/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/
