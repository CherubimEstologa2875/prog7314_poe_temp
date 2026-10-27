package com.example.runway.ui.sheets

import androidx.annotation.StringRes

// State of the confidence rating sheet.

data class ConfidenceUiState(
    val outfitName: String = "",
    val score: Int = 0,
    val note: String = "",
    val isSaving: Boolean = false,
    val savedAt: Long? = null,
    @StringRes val errorMessage: Int? = null,
) {
    // Nothing to save until a star has been picked.
    val canSave: Boolean get() = score > 0 && !isSaving
}
