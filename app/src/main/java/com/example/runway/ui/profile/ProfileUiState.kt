package com.example.runway.ui.profile

import com.example.runway.domain.model.WardrobeSummary
import java.time.YearMonth

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val initials: String = "",
    // Null until the API tells us when the account was made.
    val memberSince: YearMonth? = null,
    val summary: WardrobeSummary = WardrobeSummary(),
) {
    val needsWashCount: Int get() = summary.needsWashCount ?: 0
}
