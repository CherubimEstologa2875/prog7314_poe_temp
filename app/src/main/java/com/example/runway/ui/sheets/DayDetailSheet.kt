package com.example.runway.ui.sheets

import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.domain.model.Item
import com.example.runway.ui.components.EmptyStateView
import com.example.runway.ui.components.RunwayBottomSheet
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.navigation.NavArgs
import com.example.runway.ui.outfits.OutfitThumbnailView
import com.example.runway.ui.outfits.PlanDates
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Shows what's planned for one calendar day, with ways to change or clear it.
class DayDetailSheet : RunwayBottomSheet(R.layout.sheet_day_detail) {

    private val viewModel: DayDetailViewModel by viewModels { DayDetailViewModel.Factory }

    override val sheetTitle: CharSequence get() = getString(R.string.rw_sheet_day_detail_title)

    override val footerLayoutRes: Int = R.layout.sheet_day_detail_footer

    private var clearButton: MaterialButton? = null
    private var pickButton: MaterialButton? = null
    private var itemsById: Map<String, Item> = emptyMap()

    override fun onContentCreated(content: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            // The thumbnail needs the photo paths, which live with the items.
            val container = (requireActivity().application as RunwayApplication).container
            itemsById = container.itemRepository.observeItems().first().associateBy { it.id }

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(content, it) }
            }
        }
    }

    override fun onFooterCreated(footer: View) {
        clearButton = footer.findViewById<MaterialButton>(R.id.dayDetailClear).apply {
            setOnClickListener { viewModel.onClear() }
        }
        pickButton = footer.findViewById<MaterialButton>(R.id.dayDetailPick).apply {
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_dayDetailSheet_to_pickOutfitSheet,
                    bundleOf(NavArgs.DATE_ISO to viewModel.uiState.value.date.toString()),
                )
            }
        }
    }

    private fun render(content: View, state: DayDetailUiState) {
        content.findViewById<TextView>(R.id.dayDetailDate).text = PlanDates.label(requireContext(), state.date)
        content.findViewById<LinearProgressIndicator>(R.id.dayDetailLoading).isVisible = state.isLoading

        val outfit = state.outfit
        val card = content.findViewById<MaterialCardView>(R.id.dayDetailOutfitCard)
        card.isVisible = !state.isLoading && outfit != null

        val empty = content.findViewById<EmptyStateView>(R.id.dayDetailEmpty)
        empty.isVisible = !state.isLoading && outfit == null
        if (state.loadFailed) {
            empty.title = getString(R.string.rw_plan_load_failed)
            empty.body = null
        }

        if (outfit != null) {
            content.findViewById<OutfitThumbnailView>(R.id.dayDetailThumb)
                .bind(outfit, itemsById, viewLifecycleOwner.lifecycleScope)
            content.findViewById<TextView>(R.id.dayDetailOutfitName).text = outfit.name
            content.findViewById<TextView>(R.id.dayDetailOutfitMeta).text = listOfNotNull(
                outfit.occasion,
                resources.getQuantityString(R.plurals.rw_outfits_pieces, outfit.garmentCount, outfit.garmentCount),
            ).joinToString(" · ")
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_dayDetailSheet_to_outfitDetail,
                    bundleOf(NavArgs.OUTFIT_ID to outfit.id),
                )
            }
        }

        clearButton?.isVisible = outfit != null
        clearButton?.isEnabled = !state.isClearing
        pickButton?.setText(if (outfit != null) R.string.rw_plan_change else R.string.rw_plan_pick)
        pickButton?.isEnabled = !state.isLoading

        if (state.cleared) {
            RunwayToast.show(content, R.string.rw_plan_cleared)
            // Lets the planner behind this sheet reload.
            setFragmentResult(PickOutfitSheet.RESULT_KEY, bundleOf())
            dismiss()
        }

        state.errorMessage?.let {
            RunwayToast.show(content, it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearButton = null
        pickButton = null
    }

    companion object {
        const val TAG = "DayDetailSheet"
    }
}
