package com.example.runway.ui.outfits

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.runway.R
import com.example.runway.databinding.FragmentOutfitsBinding
import com.example.runway.domain.model.Outfit
import com.example.runway.ui.components.ListRowView
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.navigation.NavArgs
import com.example.runway.ui.sheets.PickOutfitSheet
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// The Outfits tab: saved outfits in a grid, and the planner calendar.
class OutfitsFragment : Fragment() {

    private var _binding: FragmentOutfitsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: OutfitsViewModel by viewModels { OutfitsViewModel.Factory }
    // Only created the first time the Planner tab is opened, so it doesn't load before then.
    private val planner: PlannerViewModel by viewModels { PlannerViewModel.Factory }
    private var plannerStarted = false

    private lateinit var adapter: OutfitsAdapter
    private lateinit var dayAdapter: PlannerDayAdapter

    // Upcoming rows are only rebuilt when they change.
    private var shownUpcoming: List<PlannerDay>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOutfitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.outfitsHeader.addAction(R.drawable.ic_rw_plus, getString(R.string.rw_outfits_new)) { openBuilder() }

        adapter = OutfitsAdapter(viewLifecycleOwner.lifecycleScope, ::open)
        binding.outfitsList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.outfitsList.adapter = adapter

        setUpPlanner()

        binding.outfitsSegments.addOnButtonCheckedListener { _, id, checked ->
            if (checked) showPlanner(id == R.id.outfitsSegmentPlanner)
        }
        showPlanner(savedInstanceState?.getBoolean(KEY_PLANNER) == true)

        // Either planner sheet changed a day, so the calendar needs reloading.
        setFragmentResultListener(PickOutfitSheet.RESULT_KEY) { _, _ -> if (plannerStarted) planner.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.let { outState.putBoolean(KEY_PLANNER, it.plannerPane.isVisible) }
    }

    // An outfit may have been saved or edited while this tab was off screen.
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        if (plannerStarted) planner.refresh()
    }

    private fun showPlanner(show: Boolean) {
        binding.outfitsPane.isVisible = !show
        binding.plannerPane.isVisible = show
        val wanted = if (show) R.id.outfitsSegmentPlanner else R.id.outfitsSegmentOutfits
        if (binding.outfitsSegments.checkedButtonId != wanted) binding.outfitsSegments.check(wanted)

        if (show && !plannerStarted) {
            plannerStarted = true
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    planner.uiState.collect(::renderPlanner)
                }
            }
        }
    }

    private fun render(state: OutfitsUiState) {
        adapter.submitList(state.outfits)
        adapter.itemsById = state.itemsById
        dayAdapter.itemsById = state.itemsById

        binding.outfitsProgress.isVisible = state.isLoading && state.outfits.isEmpty()

        val empty = binding.outfitsEmpty
        val nothingSaved = !state.isLoading && !state.loadFailed && state.outfits.isEmpty()
        empty.isVisible = state.loadFailed || nothingSaved
        binding.outfitsList.isVisible = !empty.isVisible
        if (state.loadFailed) {
            empty.setIcon(R.drawable.ic_rw_wifi_off)
            empty.title = getString(R.string.rw_outfits_failed_title)
            empty.body = getString(R.string.rw_outfits_failed_body)
            empty.setAction(actionButton(R.string.rw_home_retry) { viewModel.refresh() })
        } else if (nothingSaved) {
            empty.setIcon(R.drawable.ic_rw_sparkles)
            empty.title = getString(R.string.rw_outfits_empty_title)
            empty.body = getString(R.string.rw_outfits_empty_body_builder)
            empty.setAction(actionButton(R.string.rw_outfits_new) { openBuilder() })
        }

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }
    }

    private fun setUpPlanner() {
        // Monday first, to match the grid.
        DayOfWeek.entries.forEach { day ->
            binding.plannerWeekdays.addView(
                TextView(requireContext()).apply {
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    gravity = Gravity.CENTER
                    setTextAppearance(R.style.TextAppearance_Runway_Micro)
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

        dayAdapter = PlannerDayAdapter(viewLifecycleOwner.lifecycleScope) { day ->
            findNavController().navigate(
                R.id.action_outfits_to_dayDetailSheet,
                bundleOf(NavArgs.DATE_ISO to day.date.toString()),
            )
        }
        val usableWidth = resources.displayMetrics.widthPixels -
            2 * resources.getDimensionPixelSize(R.dimen.rw_screen_padding_horizontal)
        dayAdapter.cellHeight = usableWidth / PlannerDayAdapter.COLUMNS * 4 / 3
        binding.plannerGrid.layoutManager = GridLayoutManager(requireContext(), PlannerDayAdapter.COLUMNS)
        binding.plannerGrid.adapter = dayAdapter

        binding.plannerPrevious.setOnClickListener { planner.onPreviousMonth() }
        binding.plannerNext.setOnClickListener { planner.onNextMonth() }
    }

    private fun renderPlanner(state: PlannerUiState) {
        binding.plannerMonth.text = PlanDates.monthTitle(state.month)
        binding.plannerLoading.isVisible = state.isLoading
        binding.plannerFailed.isVisible = state.loadFailed
        binding.plannerGrid.isVisible = !state.loadFailed
        dayAdapter.submitList(state.days)

        if (state.loadFailed && binding.plannerFailed.tag == null) {
            binding.plannerFailed.tag = true
            binding.plannerFailed.setAction(actionButton(R.string.rw_home_retry) { planner.refresh() })
        }

        if (state.isLoading || state.upcoming == shownUpcoming) return
        shownUpcoming = state.upcoming
        binding.plannerUpcoming.removeAllViews()
        state.upcoming.forEach { day ->
            val outfit = day.outfit ?: return@forEach
            binding.plannerUpcoming.addView(ListRowView(requireContext()).apply {
                setIcon(R.drawable.ic_rw_calendar)
                label = outfit.name
                value = PlanDates.label(requireContext(), day.date)
                showChevron = true
                onClick { open(outfit) }
            })
        }
        binding.plannerUpcomingCard.isVisible = state.upcoming.isNotEmpty()
        binding.plannerNothingUpcoming.isVisible = state.upcoming.isEmpty() && !state.loadFailed
    }

    private fun actionButton(label: Int, onClick: () -> Unit) =
        MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonStyle).apply {
            setText(label)
            setOnClickListener { onClick() }
        }

    private fun openBuilder() = findNavController().navigate(R.id.action_outfits_to_builder)

    private fun open(outfit: Outfit) {
        findNavController().navigate(
            R.id.action_outfits_to_outfitDetail,
            bundleOf(NavArgs.OUTFIT_ID to outfit.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        plannerStarted = false
        shownUpcoming = null
        _binding = null
    }

    private companion object {
        const val KEY_PLANNER = "showPlanner"
    }
}
