package com.example.runway.ui.outfits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentOutfitBuilderBinding
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.home.RecentItemsAdapter
import com.example.runway.ui.navigation.NavArgs
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OutfitBuilderFragment : Fragment() {

    private var _binding: FragmentOutfitBuilderBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: OutfitBuilderViewModel by viewModels { OutfitBuilderViewModel.Factory }

    private lateinit var trayAdapter: RecentItemsAdapter
    // The photos only need decoding again when the set of garments changes.
    private var loadedFor: Set<String> = emptySet()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOutfitBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.builderHeader.onBackClick { findNavController().navigateUp() }

        val canvas = binding.builderCanvas
        canvas.editable = true
        canvas.onLayerSelected { viewModel.onSelect(it) }
        canvas.onLayerMoved { id, x, y -> viewModel.onMove(id, x, y) }

        binding.builderSizeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onScale(value / 100f)
        }
        binding.builderRotateSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.onRotate(value)
        }
        binding.builderLayerUp.setOnClickListener { viewModel.onBringForward() }
        binding.builderLayerDown.setOnClickListener { viewModel.onSendBackward() }
        binding.builderRemove.setOnClickListener { viewModel.onRemoveSelected() }

        binding.builderName.placeholder = getString(R.string.rw_builder_name_placeholder)
        binding.builderName.onTextChanged { viewModel.onNameChanged(it) }

        buildOccasionChips()
        buildCategoryChips()

        trayAdapter = RecentItemsAdapter(viewLifecycleOwner.lifecycleScope) { viewModel.onAdd(it.id) }
        binding.builderTray.adapter = trayAdapter
        // Same place the + button goes; the new item shows up in the tray when it's saved.
        binding.builderAddNew.setOnClickListener { findNavController().navigate(R.id.addItemGraph) }

        binding.builderLoadFailed.setAction(
            com.google.android.material.button.MaterialButton(requireContext()).apply {
                setText(R.string.rw_home_retry)
                setOnClickListener {
                    val id = arguments?.getString(NavArgs.OUTFIT_ID) ?: return@setOnClickListener
                    viewModel.loadExisting(id)
                }
            }
        )

        binding.builderSave.setOnClickListener { viewModel.onSave(getString(R.string.rw_builder_default_name)) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun buildOccasionChips() = OutfitBuilderViewModel.OCCASIONS.forEach { value ->
        binding.builderOccasions.addView(
            Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = getString(occasionLabel(value))
                tag = value
                isCheckable = true
                setOnCheckedChangeListener { _, checked -> if (checked) viewModel.onOccasionChanged(value) }
            }
        )
    }

    private fun buildCategoryChips() = OutfitBuilderViewModel.CATEGORIES.forEach { value ->
        binding.builderCategories.addView(
            Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = getString(categoryLabel(value))
                tag = value
                isCheckable = true
                setOnCheckedChangeListener { _, checked -> if (checked) viewModel.onTrayCategoryChanged(value) }
            }
        )
    }

    private fun render(state: OutfitBuilderUiState) {
        binding.builderHeader.title = getString(
            if (state.isEditing) R.string.rw_builder_edit_title else R.string.rw_builder_new_title
        )

        binding.builderLoading.isVisible = state.isLoading
        binding.builderLoadFailed.isVisible = state.loadFailed
        binding.builderCanvas.isVisible = !state.loadFailed
        binding.builderEmpty.isVisible = state.layers.isEmpty() && !state.isLoading && !state.loadFailed

        binding.builderCanvas.layers = state.layers
        binding.builderCanvas.selectedId = state.selectedId
        loadImagesIfNeeded(state)

        renderControls(state)

        // Only pushed into the box when it differs, or typing would keep moving the cursor.
        if (binding.builderName.text != state.name) binding.builderName.text = state.name
        checkChip(binding.builderOccasions, state.occasion)
        checkChip(binding.builderCategories, state.trayCategory)

        trayAdapter.submitList(state.trayItems)
        binding.builderTrayEmpty.isVisible = state.trayItems.isEmpty()

        binding.builderSave.isEnabled = state.canSave
        binding.builderSave.setText(if (state.isSaving) R.string.rw_builder_saving else R.string.rw_builder_save)

        state.savedOutfitId?.let { id ->
            viewModel.onSavedHandled()
            RunwayToast.show(binding.root, R.string.rw_builder_saved)
            if (state.isEditing) {
                // Outfit Detail is already underneath, so tell it to reload and go back to it.
                setFragmentResult(RESULT_KEY, bundleOf())
                findNavController().navigateUp()
            } else {
                findNavController().navigate(R.id.action_builder_to_outfitDetail, bundleOf(NavArgs.OUTFIT_ID to id))
            }
        }

        state.errorMessage?.let {
            RunwayToast.show(binding.root, it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }
    }

    private fun renderControls(state: OutfitBuilderUiState) {
        val layer = state.selectedLayer
        binding.builderControls.isVisible = layer != null
        if (layer == null) return

        binding.builderSelectedName.text = state.selectedItem?.name.orEmpty()
        // A Slider throws if handed a value that isn't on one of its steps.
        binding.builderSizeSlider.value = ((layer.scale * 100 / 5).roundToInt() * 5).toFloat().coerceIn(40f, 190f)
        binding.builderRotateSlider.value = layer.rotation.roundToInt().toFloat().coerceIn(-45f, 45f)
    }

    private fun loadImagesIfNeeded(state: OutfitBuilderUiState) {
        val ids = state.layers.map { it.itemId }.toSet()
        // Waits for the wardrobe too, since that's where the photo paths come from.
        if (ids == loadedFor || state.wardrobe.isEmpty()) return
        loadedFor = ids

        val size = resources.displayMetrics.widthPixels / 2
        viewLifecycleOwner.lifecycleScope.launch {
            val images = GarmentImages.load(ids.toList(), state.itemsById, size)
            _binding?.builderCanvas?.images = images
        }
    }

    private fun checkChip(group: com.google.android.material.chip.ChipGroup, value: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.tag == value && !chip.isChecked) chip.isChecked = true
        }
    }

    private fun occasionLabel(value: String): Int = when (value) {
        "Work" -> R.string.rw_occasion_work
        "Evening" -> R.string.rw_occasion_evening
        "Sport" -> R.string.rw_occasion_sport
        "Formal" -> R.string.rw_occasion_formal
        else -> R.string.rw_occasion_casual
    }

    private fun categoryLabel(value: String): Int = when (value) {
        "TOP" -> R.string.rw_category_top
        "BOTTOM" -> R.string.rw_category_bottom
        "OUTERWEAR" -> R.string.rw_category_outerwear
        "SHOES" -> R.string.rw_category_shoes
        else -> R.string.rw_category_other
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadedFor = emptySet()
        _binding = null
    }

    companion object {
        // Outfit Detail listens for this to reload after an edit.
        const val RESULT_KEY = "outfitEdited"
    }
}
