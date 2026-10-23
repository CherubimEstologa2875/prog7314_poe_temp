package com.example.runway.domain.repository

import com.example.runway.domain.model.WardrobeSummary

interface WardrobeRepository {
    suspend fun summary(): Result<WardrobeSummary>
}
