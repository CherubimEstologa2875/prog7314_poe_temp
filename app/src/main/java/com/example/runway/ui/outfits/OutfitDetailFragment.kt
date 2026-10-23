package com.example.runway.ui.outfits

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentOutfitDetailBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneOffset
import com.example.runway.domain.model.RatingSummary
import com.example.runway.ui.navigation.NavArgs
import com.example.runway.ui.sheets.ConfidenceSheet
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

// One saved outfit: its layout, its name and its garments, all editable
// (IIE, 2026; Android Open Source Project, 2020c).
// Takes an [com.example.runway.ui.navigation.NavArgs.OUTFIT_ID] argument.

class OutfitDetailFragment : Fragment() {

    private var _binding: FragmentOutfitDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: OutfitDetailViewModel by viewModels { OutfitDetailViewModel.Factory }

    // Set while writing the name into the field, so restoring state does not read
    // back as the user typing.
    private var applyingState = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOutfitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.outfitDetailHeader.setShowBack(true)
        binding.outfitDetailHeader.onBackClick { findNavController().navigateUp() }

        binding.outfitNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!applyingState) viewModel.onNameChanged(s?.toString().orEmpty())
            }
        })

        binding.outfitSaveButton.setOnClickListener { viewModel.onSave() }
        binding.outfitDeleteButton.setOnClickListener { confirmDelete() }
        binding.outfitWearButton.setOnClickListener { viewModel.onWoreToday() }
        binding.outfitLoadFailed.setAction(
            com.google.android.material.button.MaterialButton(requireContext()).apply {
                setText(R.string.rw_home_retry)
                setOnClickListener { viewModel.reload() }
            }
        )
        binding.outfitScheduleButton.setOnClickListener { pickDate() }
        binding.outfitDetailHeader.addAction(R.drawable.ic_rw_edit, getString(R.string.rw_outfit_edit)) {
            val id = viewModel.uiState.value.outfit?.id ?: return@addAction
            findNavController().navigate(R.id.action_outfitDetail_to_builder, bundleOf(NavArgs.OUTFIT_ID to id))
        }

        // The builder saves the edit itself, so this screen only needs to reload.
        parentFragmentManager.setFragmentResultListener(
            OutfitBuilderFragment.RESULT_KEY, viewLifecycleOwner
        ) { _, _ -> viewModel.reload() }

        // The sheet saves the rating itself, so we just reload once it closes.
        parentFragmentManager.setFragmentResultListener(
            ConfidenceSheet.TAG, viewLifecycleOwner
        ) { _, _ -> viewModel.onRatingsChanged() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: OutfitDetailUiState) {
        if (state.deleted) {
            findNavController().navigateUp()
            return
        }

        applyingState = true

        binding.outfitDetailHeader.title = state.outfit?.name
            ?: getString(R.string.rw_outfit_loading)

        if (binding.outfitNameInput.text?.toString() != state.name) {
            binding.outfitNameInput.setText(state.name)
        }

        binding.outfitDetailHeader.subtitle = state.outfit?.occasion

        binding.outfitLoadFailed.isVisible = state.loadFailed
        if (state.loadFailed) binding.outfitDetailHeader.title = getString(R.string.rw_outfit_load_failed_title)
        binding.outfitWearButton.isEnabled = state.outfit != null
        binding.outfitDeleteButton.isEnabled = state.outfit != null

        val showCollage = state.outfit != null && state.garments.isNotEmpty()
        binding.outfitCollage.isVisible = showCollage
        if (showCollage) {
            binding.outfitCollage.bind(
                state.outfit, state.garments.associateBy { it.id }, viewLifecycleOwner.lifecycleScope
            )
        }
        binding.outfitNoGarmentsPreview.isVisible = !showCollage && !state.isLoading && !state.loadFailed
        binding.outfitScheduleButton.isEnabled = state.outfit != null

        renderGarments(state)
        renderConfidence(state)

        // A logged wear is what prompts for a rating.
        if (state.wearLoggedAt != null) {
            viewModel.onWearHandled()
            RunwayToast.show(requireView(), getString(R.string.rw_outfit_wear_logged))
            openConfidenceSheet()
        }

        binding.outfitSaveButton.isEnabled = state.hasChanges && !state.isSaving
        binding.outfitSaveButton.setText(
            if (state.isSaving) R.string.rw_outfit_saving else R.string.rw_outfit_save_changes
        )

        state.savedAt?.let {
            RunwayToast.show(requireView(), getString(R.string.rw_outfit_saved))
            viewModel.onMessageShown()
        }
        state.scheduledFor?.let { date ->
            RunwayToast.show(
                requireView(),
                getString(R.string.rw_plan_planned_for, PlanDates.label(requireContext(), date)),
            )
            viewModel.onScheduleHandled()
        }
        state.errorMessage?.let {
            RunwayToast.show(requireView(), it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }

        applyingState = false
    }

    private fun renderConfidence(state: OutfitDetailUiState) {
        val ratings = state.ratings
        binding.outfitConfidenceStars.rating = ratings.roundedAverage
        binding.outfitConfidenceLabel.text = when {
            ratings.hasRatings -> resources.getQuantityString(
                R.plurals.rw_outfit_confidence_count,
                ratings.count,
                ratings.average.toString(),
                ratings.count,
            )
            // Saying "not rated yet" when the fetch failed would be a lie.
            state.ratingsFailed -> getString(R.string.rw_outfit_confidence_failed)
            else -> getString(R.string.rw_outfit_confidence_none)
        }
    }

    // A calendar is easier to aim at than a list of the next few days.
    private fun pickDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.rw_plan_schedule_title)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            // The picker hands back UTC midnight, so read the date in UTC too.
            viewModel.onScheduled(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
        picker.show(parentFragmentManager, "schedule")
    }

    private fun openConfidenceSheet() {
        val outfitId = viewModel.uiState.value.outfit?.id ?: return
        findNavController().navigate(
            R.id.action_outfitDetail_to_confidenceSheet,
            bundleOf(NavArgs.OUTFIT_ID to outfitId),
        )
    }

    // A chip per garment; the close icon takes it out of the outfit.
    private fun renderGarments(state: OutfitDetailUiState) {
        val group = binding.outfitGarmentsGroup
        group.removeAllViews()

        binding.outfitNoGarments.isVisible = state.garments.isEmpty() && !state.isLoading

        state.garments.forEach { item ->
            group.addView(
                Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    text = if (item.size.isNullOrBlank()) item.name else getString(
                        R.string.rw_outfit_item_with_size, item.name, item.size
                    )
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { viewModel.onRemoveGarment(item) }
                }
            )
        }
    }

    private fun confirmDelete() = RunwayDialogs.confirm(
        context = requireContext(),
        titleRes = R.string.rw_outfit_delete_title,
        bodyRes = R.string.rw_outfit_delete_body,
        confirmLabelRes = R.string.rw_outfit_delete,
        destructive = true,
    ) {
        viewModel.onDelete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].
*/
