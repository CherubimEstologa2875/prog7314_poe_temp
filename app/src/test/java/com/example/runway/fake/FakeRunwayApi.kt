package com.example.runway.fake

import com.example.runway.data.remote.api.CreateItemDto
import com.example.runway.data.remote.api.CreateRatingDto
import com.example.runway.data.remote.api.ItemDto
import com.example.runway.data.remote.api.LogOutfitWearDto
import com.example.runway.data.remote.api.LogWearDto
import com.example.runway.data.remote.api.OutfitDto
import com.example.runway.data.remote.api.OutfitWearDto
import com.example.runway.data.remote.api.PageDto
import com.example.runway.data.remote.api.PlanDto
import com.example.runway.data.remote.api.RatingDto
import com.example.runway.data.remote.api.RatingSummaryDto
import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.SetPlanDto
import com.example.runway.data.remote.api.SettingsDto
import com.example.runway.data.remote.api.UpdateItemDto
import com.example.runway.data.remote.api.UpdateOutfitDto
import com.example.runway.data.remote.api.UserDto
import com.example.runway.data.remote.api.WardrobeSummaryDto
import com.example.runway.data.remote.api.WearDto

// Stands in for the REST API. Anything a test doesn't set up throws, so nothing quietly passes.
class FakeRunwayApi : RunwayApi {

    // Items
    var itemPages: List<PageDto<ItemDto>> = listOf(PageDto())
    val itemPageRequests = mutableListOf<Pair<String?, String?>>()
    var createdItem: (CreateItemDto) -> ItemDto = { ItemDto(id = "server-1", name = it.name, category = it.category) }
    var updateItemError: Throwable? = null
    val createdItems = mutableListOf<CreateItemDto>()
    val updatedItems = mutableListOf<Pair<String, UpdateItemDto>>()
    val deletedItems = mutableListOf<String>()

    // Outfits
    var outfits: List<OutfitDto> = emptyList()
    var createdOutfit: OutfitDto? = null
    var updatedOutfit: Pair<String, UpdateOutfitDto>? = null

    // Ratings
    var ratingSummary = RatingSummaryDto()
    var ratingSent: CreateRatingDto? = null

    // Plans
    var plans: List<PlanDto> = emptyList()
    var planRequest: Pair<String, String>? = null
    val setPlans = mutableListOf<Pair<String, SetPlanDto>>()
    val clearedPlans = mutableListOf<String>()

    override suspend fun getItems(category: String?, updatedSince: String?, limit: Int?, cursor: String?): PageDto<ItemDto> {
        itemPageRequests += updatedSince to cursor
        // Pages are handed out in order, one per call.
        return itemPages[itemPageRequests.size - 1]
    }

    override suspend fun createItem(body: CreateItemDto): ItemDto {
        createdItems += body
        return createdItem(body)
    }

    override suspend fun updateItem(id: String, body: UpdateItemDto): ItemDto {
        updateItemError?.let { throw it }
        updatedItems += id to body
        return ItemDto(id = id, name = body.name.orEmpty(), category = body.category.orEmpty())
    }

    override suspend fun deleteItem(id: String) {
        deletedItems += id
    }

    override suspend fun getOutfits(limit: Int?, cursor: String?): PageDto<OutfitDto> = PageDto(outfits)

    override suspend fun getOutfit(id: String): OutfitDto = outfits.first { it.id == id }

    override suspend fun createOutfit(body: OutfitDto): OutfitDto {
        createdOutfit = body
        return body.copy(id = "outfit-new")
    }

    override suspend fun updateOutfit(id: String, body: UpdateOutfitDto): OutfitDto {
        updatedOutfit = id to body
        return OutfitDto(id = id, name = body.name.orEmpty(), occasion = body.occasion, items = body.items.orEmpty())
    }

    override suspend fun getOutfitRatings(id: String): RatingSummaryDto = ratingSummary

    override suspend fun rateOutfit(id: String, body: CreateRatingDto): RatingDto {
        ratingSent = body
        return RatingDto(id = "rating-1", outfitId = id, score = body.score.toLong(), note = body.note)
    }

    override suspend fun getPlans(from: String, to: String): List<PlanDto> {
        planRequest = from to to
        return plans
    }

    override suspend fun setPlan(date: String, body: SetPlanDto): PlanDto {
        setPlans += date to body
        return PlanDto(id = "uid_$date", date = date, outfitId = body.outfitId, status = "PLANNED")
    }

    override suspend fun clearPlan(date: String) {
        clearedPlans += date
    }

    override suspend fun getItem(id: String): ItemDto = unused()
    override suspend fun logWear(id: String, body: LogWearDto): WearDto = unused()
    override suspend fun deleteOutfit(id: String) = unused()
    override suspend fun logOutfitWear(id: String, body: LogOutfitWearDto): OutfitWearDto = unused()
    override suspend fun getWardrobeSummary(): WardrobeSummaryDto = unused()
    override suspend fun getProfile(): UserDto = unused()
    override suspend fun getSettings(): SettingsDto = unused()
    override suspend fun saveSettings(body: SettingsDto): SettingsDto = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("Not set up in this test")
}
