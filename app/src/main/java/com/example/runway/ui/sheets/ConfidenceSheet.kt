package com.example.runway.ui.sheets

import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.runway.R
import com.example.runway.domain.model.RatingSummary
import com.example.runway.ui.components.FieldView
import com.example.runway.ui.components.RunwayBottomSheet
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.components.StarRatingView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

// "How confident did you feel?" - shown after marking an outfit worn.

class ConfidenceSheet : RunwayBottomSheet(R.layout.sheet_confidence) {

    private val viewModel: ConfidenceViewModel by viewModels { ConfidenceViewModel.Factory }

    override val sheetTitle: CharSequence get() = getString(R.string.rw_sheet_confidence_title)

    override val footerLayoutRes: Int = R.layout.sheet_confidence_footer

    private var stars: StarRatingView? = null
    private var scoreLabel: android.widget.TextView? = null
    private var saveButton: MaterialButton? = null

    override fun onContentCreated(content: View) {
        stars = content.findViewById<StarRatingView>(R.id.confidenceStars).apply {
            onRatingChanged { viewModel.onScoreChanged(it) }
        }
        scoreLabel = content.findViewById(R.id.confidenceScoreLabel)

        content.findViewById<FieldView>(R.id.confidenceNote).apply {
            setMultiline(lines = 2)
            onTextChanged { viewModel.onNoteChanged(it) }
        }

        observeState(content)
    }

    override fun onFooterCreated(footer: View) {
        saveButton = footer.findViewById<MaterialButton>(R.id.confidenceSave).apply {
            setOnClickListener { viewModel.onSave() }
        }
        footer.findViewById<MaterialButton>(R.id.confidenceLater).setOnClickListener { dismiss() }
    }

    private fun observeState(content: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    content.findViewById<android.widget.TextView>(R.id.confidencePrompt).text =
                        if (state.outfitName.isBlank()) {
                            getString(R.string.rw_confidence_prompt_generic)
                        } else {
                            getString(R.string.rw_confidence_prompt, state.outfitName)
                        }

                    scoreLabel?.text = state.score.takeIf { it > 0 }?.let { getString(scoreLabel(it)) }.orEmpty()
                    saveButton?.isEnabled = state.canSave
                    saveButton?.setText(
                        if (state.isSaving) R.string.rw_confidence_saving else R.string.rw_confidence_save
                    )

                    if (state.savedAt != null) {
                        RunwayToast.show(content, R.string.rw_confidence_saved, R.drawable.ic_rw_star_filled)
                        // Tells the outfit screen behind us to reload its ratings.
                        setFragmentResult(TAG, bundleOf())
                        dismiss()
                    }

                    state.errorMessage?.let {
                        RunwayToast.show(content, it, R.drawable.ic_rw_alert)
                        viewModel.onMessageShown()
                    }
                }
            }
        }
    }

    @StringRes
    private fun scoreLabel(score: Int): Int = when (score) {
        1 -> R.string.rw_confidence_1
        2 -> R.string.rw_confidence_2
        3 -> R.string.rw_confidence_3
        4 -> R.string.rw_confidence_4
        else -> R.string.rw_confidence_5
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stars = null
        scoreLabel = null
        saveButton = null
    }

    companion object {
        const val TAG = "ConfidenceSheet"

        // Kept so callers can talk about the range without reaching into the model.
        const val MIN_SCORE = RatingSummary.MIN_SCORE
        const val MAX_SCORE = RatingSummary.MAX_SCORE
    }
}
