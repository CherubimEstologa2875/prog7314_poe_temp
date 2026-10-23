package com.example.runway.ui.additem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentTagItemBinding
import com.example.runway.domain.model.ColourPalette
import com.example.runway.domain.model.PaletteColour
import com.example.runway.domain.model.ItemDraft
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.components.ColourPickerDialog
import com.example.runway.ui.components.SwatchView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

// Final step of adding an item: name it, say what it is, and save.
// Category is a fixed list because the model preview and the API both key off it.

class TagItemFragment : Fragment() {

    private var _binding: FragmentTagItemBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: AddItemViewModel by activityViewModels { AddItemViewModel.Factory }

    private var category: String = CATEGORIES.first()
    private val colours get() =
        (requireActivity().application as RunwayApplication).container.colourRepository

    private var colour: PaletteColour? = null
    // The built-in colours plus the user's own. Re-read whenever one is added.
    private var palette: List<PaletteColour> = emptyList()
    private var wearLimit: Int = RunwaySettings.DEFAULT_WEAR_LIMIT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTagItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            category = savedInstanceState.getString(KEY_CATEGORY) ?: category
            colour = ColourPalette.parse(savedInstanceState.getString(KEY_COLOUR), colours.custom())
            wearLimit = savedInstanceState.getInt(KEY_WEAR_LIMIT, wearLimit)
        } else {
            // Starts from whatever the user picked in Settings.
            val settings = (requireActivity().application as RunwayApplication).container.settingsRepository
            wearLimit = settings.currentSettings().defaultWearLimit
        }

        binding.tagHeader.title = getString(R.string.rw_tag_item_title)
        binding.tagHeader.subtitle = getString(R.string.rw_tag_hint)
        binding.tagHeader.onBackClick { findNavController().navigateUp() }

        buildCategoryChips()
        buildPalette()
        binding.tagPreview.setImageBitmap(viewModel.state.value.chosen)

        binding.tagWearLess.setOnClickListener { changeWearLimit(-1) }
        binding.tagWearMore.setOnClickListener { changeWearLimit(1) }

        binding.tagNameInput.doAfterTextChanged { validate() }
        binding.tagPriceInput.doAfterTextChanged { validate() }
        binding.tagSaveButton.setOnClickListener { viewModel.onSave(currentDraft()) }

        showWearLimit()
        showColour()
        validate()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CATEGORY, category)
        outState.putString(KEY_COLOUR, colour?.label)
        outState.putInt(KEY_WEAR_LIMIT, wearLimit)
    }

    private fun buildCategoryChips() = CATEGORIES.forEach { value ->
        binding.tagCategoryGroup.addView(
            Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = getString(labelFor(value))
                isCheckable = true
                isChecked = value == category
                setOnCheckedChangeListener { _, checked -> if (checked) category = value }
            }
        )
    }

    private fun buildPalette() {
        val size = resources.getDimensionPixelSize(R.dimen.rw_space_32)
        val gap = resources.getDimensionPixelSize(R.dimen.rw_space_8)
        val params = LinearLayout.LayoutParams(size, size).apply { marginEnd = gap }

        palette = colours.palette()
        binding.tagColourPalette.removeAllViews()

        palette.forEach { option ->
            val swatch = SwatchView(requireContext()).apply {
                swatchColor = option.argb
                swatchSize = size
                contentDescription = option.label
                setOnClickListener {
                    // Tapping the chosen colour again clears it, since colour is optional.
                    colour = if (colour == option) null else option
                    showColour()
                }
            }
            binding.tagColourPalette.addView(swatch, params)
        }

        binding.tagColourAdd.setOnClickListener { addColour() }
    }

    // A mixed colour joins the palette, so it can be picked again and filtered on later.
    private fun addColour() {
        ColourPickerDialog.show(requireContext()) { label, argb ->
            colour = colours.add(label, argb)
            buildPalette()
            showColour()
        }
    }

    private fun showColour() {
        val swatches = binding.tagColourPalette
        palette.forEachIndexed { i, option ->
            (swatches.getChildAt(i) as SwatchView).picked = option == colour
        }
        binding.tagColourName.text = colour?.label ?: getString(R.string.rw_tag_colour_none)
    }

    private fun changeWearLimit(by: Int) {
        wearLimit = (wearLimit + by).coerceIn(RunwaySettings.MIN_WEAR_LIMIT, RunwaySettings.MAX_WEAR_LIMIT)
        showWearLimit()
    }

    private fun showWearLimit() {
        binding.tagWearLimit.text = wearLimit.toString()
        binding.tagWearLess.isEnabled = wearLimit > RunwaySettings.MIN_WEAR_LIMIT
        binding.tagWearMore.isEnabled = wearLimit < RunwaySettings.MAX_WEAR_LIMIT
    }

    private fun currentDraft() = ItemDraft(
        name = binding.tagNameInput.text?.toString().orEmpty(),
        category = category,
        colour = colour,
        brand = binding.tagBrandInput.text?.toString().orEmpty(),
        size = binding.tagSizeInput.text?.toString().orEmpty(),
        priceText = binding.tagPriceInput.text?.toString().orEmpty(),
        wearLimit = wearLimit,
    )

    // A missing name only disables Save; the other problems are shown on the field.
    private fun validate() {
        val problems = currentDraft().problems

        binding.tagNameLayout.error = if (ItemDraft.Problem.NAME_TOO_LONG in problems) {
            getString(R.string.rw_tag_error_name_long, ItemDraft.MAX_NAME)
        } else {
            null
        }
        binding.tagNameLayout.helperText = if (ItemDraft.Problem.NAME_MISSING in problems) {
            getString(R.string.rw_tag_name_required)
        } else {
            getString(R.string.rw_tag_name_example)
        }

        binding.tagPriceLayout.error = when {
            ItemDraft.Problem.PRICE_NOT_A_NUMBER in problems -> getString(R.string.rw_tag_error_price_number)
            ItemDraft.Problem.PRICE_NEGATIVE in problems -> getString(R.string.rw_tag_error_price_negative)
            else -> null
        }

        binding.tagSaveButton.isEnabled = problems.isEmpty() && !viewModel.state.value.isSaving
    }

    private fun render(state: AddItemState) {
        binding.tagSaveButton.isEnabled = currentDraft().isValid && !state.isSaving
        binding.tagSaveButton.setText(if (state.isSaving) R.string.rw_tag_saving else R.string.rw_tag_save)

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }

        if (state.saved) {
            RunwayToast.show(requireView(), getString(R.string.rw_tag_saved))
            viewModel.reset()
            // Closes the whole add-item stack and lands back on the tab the user
            // started from, where the new item is already in the list.
            findNavController().popBackStack(R.id.addItemGraph, true)
        }
    }

    private fun labelFor(value: String): Int = when (value) {
        "TOP" -> R.string.rw_category_top
        "BOTTOM" -> R.string.rw_category_bottom
        "OUTERWEAR" -> R.string.rw_category_outerwear
        "SHOES" -> R.string.rw_category_shoes
        else -> R.string.rw_category_other
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        // The four the model preview can draw, plus a catch-all.
        val CATEGORIES = listOf("TOP", "BOTTOM", "OUTERWEAR", "SHOES", "OTHER")

        const val KEY_CATEGORY = "category"
        const val KEY_COLOUR = "colour"
        const val KEY_WEAR_LIMIT = "wearLimit"
    }
}
