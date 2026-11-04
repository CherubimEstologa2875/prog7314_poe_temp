package com.example.runway.ui.wardrobe

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentTagItemBinding
import com.example.runway.domain.model.ColourPalette
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ItemDraft
import com.example.runway.domain.model.PaletteColour
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.ui.additem.TagItemFragment
import com.example.runway.ui.components.ColourPickerDialog
import com.example.runway.ui.components.Labels
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.components.SwatchView
import com.example.runway.ui.itemdetail.ItemDetailViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Edits a saved garment, using the same form as the last add-item step
class ItemEditFragment : Fragment() {

    private var _binding: FragmentTagItemBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ItemDetailViewModel by viewModels { ItemDetailViewModel.Factory }

    private val container get() = (requireActivity().application as RunwayApplication).container

    private var item: Item? = null
    private var category: String = TagItemFragment.CATEGORIES.first()
    private var colour: PaletteColour? = null
    private var palette: List<PaletteColour> = emptyList()
    private var wearLimit: Int = RunwaySettings.DEFAULT_WEAR_LIMIT
    private var saving = false

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

        binding.tagHeader.title = getString(R.string.rw_item_edit_title)
        binding.tagHeader.subtitle = getString(R.string.rw_tag_hint)
        binding.tagHeader.onBackClick { findNavController().navigateUp() }
        binding.tagSaveButton.setText(R.string.rw_item_edit_save)
        binding.tagSaveButton.isEnabled = false

        binding.tagWearLess.setOnClickListener { changeWearLimit(-1) }
        binding.tagWearMore.setOnClickListener { changeWearLimit(1) }
        binding.tagNameInput.doAfterTextChanged { validate() }
        binding.tagPriceInput.doAfterTextChanged { validate() }
        binding.tagSaveButton.setOnClickListener { save() }

        viewLifecycleOwner.lifecycleScope.launch {
            fill(viewModel.uiState.mapNotNull { it.item }.first(), savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (item == null) return
        outState.putString(KEY_CATEGORY, category)
        outState.putString(KEY_COLOUR, colour?.label)
        outState.putInt(KEY_WEAR_LIMIT, wearLimit)
    }

    // After a rotation the text fields restore themselves, so only the rest comes from the bundle
    private fun fill(loaded: Item, saved: Bundle?) {
        item = loaded
        if (saved?.containsKey(KEY_CATEGORY) == true) {
            category = saved.getString(KEY_CATEGORY) ?: loaded.category
            colour = ColourPalette.parse(saved.getString(KEY_COLOUR), container.colourRepository.custom())
            wearLimit = saved.getInt(KEY_WEAR_LIMIT, wearLimit)
        } else {
            category = loaded.category
            colour = ColourPalette.parse(loaded.colour, container.colourRepository.custom())
            wearLimit = loaded.wearLimit ?: container.settingsRepository.currentSettings().defaultWearLimit
            binding.tagNameInput.setText(loaded.name)
            binding.tagBrandInput.setText(loaded.brand.orEmpty())
            binding.tagSizeInput.setText(loaded.size.orEmpty())
            binding.tagPriceInput.setText(
                loaded.purchasePrice?.toBigDecimal()?.stripTrailingZeros()?.toPlainString().orEmpty()
            )
        }

        buildCategoryChips()
        buildPalette()
        showColour()
        showWearLimit()
        loadPhoto(loaded.imagePath)
        validate()
    }

    private fun buildCategoryChips() = TagItemFragment.CATEGORIES.forEach { value ->
        binding.tagCategoryGroup.addView(
            Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = Labels.category(requireContext(), value)
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

        palette = container.colourRepository.palette()
        binding.tagColourPalette.removeAllViews()

        palette.forEach { option ->
            val swatch = SwatchView(requireContext()).apply {
                swatchColor = option.argb
                swatchSize = size
                contentDescription = Labels.colour(requireContext(), option.label)
                setOnClickListener {
                    colour = if (colour == option) null else option
                    showColour()
                }
            }
            binding.tagColourPalette.addView(swatch, params)
        }

        binding.tagColourAdd.setOnClickListener {
            ColourPickerDialog.show(requireContext()) { label, argb ->
                colour = container.colourRepository.add(label, argb)
                buildPalette()
                showColour()
            }
        }
    }

    private fun showColour() {
        palette.forEachIndexed { i, option ->
            (binding.tagColourPalette.getChildAt(i) as SwatchView).picked = option == colour
        }
        binding.tagColourName.text =
            Labels.colour(requireContext(), colour?.label) ?: getString(R.string.rw_tag_colour_none)
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

    private fun loadPhoto(path: String?) {
        binding.tagPreview.isVisible = !path.isNullOrBlank()
        if (path.isNullOrBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                File(path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(path) }
            }
            if (bitmap != null && _binding != null) binding.tagPreview.setImageBitmap(bitmap)
        }
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
            ItemDraft.Problem.PRICE_TOO_HIGH in problems -> getString(R.string.rw_tag_error_price_too_high)
            else -> null
        }

        binding.tagSaveButton.isEnabled = item != null && problems.isEmpty() && !saving
    }

    private fun save() {
        val current = item ?: return
        val draft = currentDraft()
        if (!draft.isValid) return

        saving = true
        binding.tagSaveButton.isEnabled = false
        binding.tagSaveButton.setText(R.string.rw_tag_saving)

        viewModel.onSave(draft.applyTo(current)) {
            if (isAdded) {
                RunwayToast.show(requireView(), getString(R.string.rw_item_edit_saved))
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val KEY_CATEGORY = "category"
        const val KEY_COLOUR = "colour"
        const val KEY_WEAR_LIMIT = "wearLimit"
    }
}