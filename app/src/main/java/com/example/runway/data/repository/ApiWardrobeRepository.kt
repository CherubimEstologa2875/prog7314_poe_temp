package com.example.runway.data.repository

import com.example.runway.data.remote.api.RunwayApi
import com.example.runway.data.remote.api.WardrobeSummaryDto
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.domain.repository.WardrobeRepository

// Reads the totals from GET /api/v1/wardrobe/summary.
class ApiWardrobeRepository(
    private val api: RunwayApi,
) : WardrobeRepository {

    override suspend fun summary(): Result<WardrobeSummary> =
        runCatching { api.getWardrobeSummary().toDomain() }

    private fun WardrobeSummaryDto.toDomain() = WardrobeSummary(
        itemCount = itemCount,
        outfitCount = outfitCount,
        totalWears = totalWears,
        totalValue = totalValue,
        needsWashCount = needsWashCount,
    )
}
