package com.example.runway.ui.sheets

// State of the confidence rating sheet.

data class ConfidenceUiState(
    val outfitName: String = "",
    val score: Int = 0,
    val note: String = "",
    val isSaving: Boolean = false,
    val savedAt: Long? = null,
    val errorMessage: String? = null,
) {
    // Nothing to save until a star has been picked.
    val canSave: Boolean get() = score > 0 && !isSaving
}
