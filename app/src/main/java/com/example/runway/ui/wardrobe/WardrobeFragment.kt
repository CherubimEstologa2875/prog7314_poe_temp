package com.example.runway.ui.wardrobe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.runway.R
import com.example.runway.databinding.FragmentWardrobeBinding
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.items.ItemsUiState
import com.example.runway.ui.items.ItemsViewModel
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.launch
import com.google.android.material.chip.Chip

// The wardrobe tab: every garment the user owns.
// Room is the source of truth, so a newly saved item appears here before the
// server has heard about it.

class WardrobeFragment : Fragment() {

    private var _binding: FragmentWardrobeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ItemsViewModel by viewModels { ItemsViewModel.Factory }

    private lateinit var adapter: WardrobeAdapter
    private var query = ""
    private var selectedCategory = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WardrobeAdapter(viewLifecycleOwner.lifecycleScope, ::openItem)
        binding.wardrobeList.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.wardrobeList.adapter = adapter

        buildCategoryChips()
        binding.wardrobeSearch.doAfterTextChanged { query = it?.toString().orEmpty(); render(viewModel.uiState.value) }
        binding.wardrobeFilterButton.setOnClickListener {
            findNavController().navigate(R.id.action_wardrobe_to_filterSheet)
        }
        binding.wardrobeSortButton.setOnClickListener {
            findNavController().navigate(R.id.action_wardrobe_to_sortSheet)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ItemsUiState) {
        val visibleItems = state.items
            .filter { item ->
                selectedCategory == "All" || categoryMatches(item.category, selectedCategory)
            }
            .filter { item ->
                val needle = query.trim()
                needle.isBlank() || listOf(item.name, item.brand, item.colour, item.category)
                    .filterNotNull()
                    .any { value -> value.contains(needle, ignoreCase = true) }
            }
            .sortedByDescending { it.updatedAt }

        adapter.submitList(visibleItems)

        val isEmpty = visibleItems.isEmpty() && !state.isLoading
        binding.wardrobeList.isVisible = !isEmpty
        binding.wardrobeEmpty.isVisible = isEmpty
        if (isEmpty) {
            binding.wardrobeEmpty.title = getString(R.string.rw_wardrobe_empty_title)
            binding.wardrobeEmpty.body = if (state.items.isEmpty()) {
                getString(R.string.rw_wardrobe_empty_body)
            } else {
                getString(R.string.rw_wardrobe_no_matches)
            }
        }

        binding.wardrobeCount.text = resources.getQuantityString(
            R.plurals.rw_wardrobe_count, state.items.size, state.items.size
        )

        // Says plainly that some rows are only on this phone so far.
        binding.wardrobeUnsynced.isVisible = state.hasUnsyncedChanges
        binding.wardrobeUnsynced.setText(R.string.rw_wardrobe_unsynced)

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onErrorShown()
        }
    }

    private fun buildCategoryChips() {
        listOf("All", "Tops", "Bottoms", "Outerwear", "Dresses", "Shoes", "Other")
            .forEach { label ->
                val chip = Chip(requireContext()).apply {
                    text = label
                    isCheckable = true
                    isChecked = label == "All"
                    setTextAppearance(R.style.TextAppearance_Runway_Button_Small)
                    setChipStyle()
                    setOnClickListener {
                        selectedCategory = label
                        render(viewModel.uiState.value)
                    }
                }
                binding.wardrobeCategories.addView(chip)
            }
    }

    private fun categoryMatches(itemCategory: String, chipCategory: String): Boolean {
        val normalized = itemCategory.trim().uppercase()
        return when (chipCategory) {
            "Tops" -> normalized == "TOP" || normalized == "TOPS"
            "Bottoms" -> normalized == "BOTTOM" || normalized == "BOTTOMS"
            "Outerwear" -> normalized == "OUTERWEAR"
            "Dresses" -> normalized == "DRESS" || normalized == "DRESSES"
            "Shoes" -> normalized == "SHOES" || normalized == "SHOE"
            "Other" -> normalized !in setOf("TOP", "TOPS", "BOTTOM", "BOTTOMS", "OUTERWEAR", "DRESS", "DRESSES", "SHOES", "SHOE")
            else -> false
        }
    }

    private fun Chip.setChipStyle() {
        setChipBackgroundColorResource(R.color.rw_chip_background_state)
        chipStrokeColor = resources.getColorStateList(R.color.rw_chip_stroke, null)
        setTextColor(resources.getColorStateList(R.color.rw_chip_text, null))
        chipStrokeWidth = resources.getDimension(R.dimen.rw_border_width)
    }

    private fun openItem(itemId: com.example.runway.domain.model.Item) {
        findNavController().navigate(
            R.id.action_wardrobe_to_itemDetail,
            bundleOf(NavArgs.ITEM_ID to itemId.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The adapter holds decoded bitmaps; dropping it with the view frees them.
        binding.wardrobeList.adapter = null
        _binding = null
    }
}
