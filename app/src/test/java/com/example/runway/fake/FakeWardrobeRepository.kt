package com.example.runway.fake

import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.domain.repository.WardrobeRepository

// Hands back a fixed summary, or fails when error is set.
class FakeWardrobeRepository(
    var summary: WardrobeSummary = WardrobeSummary(),
) : WardrobeRepository {

    var error: Throwable? = null
    var callCount = 0
        private set

    override suspend fun summary(): Result<WardrobeSummary> {
        callCount++
        return error?.let { Result.failure(it) } ?: Result.success(summary)
    }
}
