package com.example.runway.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentHomeBinding
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.ui.components.RunwayToast
import com.example.runway.ui.navigation.NavArgs
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    private lateinit var recentAdapter: RecentItemsAdapter
    // Which empty message is showing, so its button isn't rebuilt on every update.
    private var shownEmpty: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.homeHeader.addAction(R.drawable.ic_rw_bell, getString(R.string.rw_home_notifications)) {
            findNavController().navigate(R.id.action_home_to_notifications)
        }
        binding.homeWeatherCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_weather)
        }
        binding.homeLaundryCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_laundry)
        }

        binding.homeSuggestButton.setOnClickListener { viewModel.onSuggestAnother() }
        binding.homeWearButton.setOnClickListener { viewModel.onWearToday() }

        binding.homeStatsHeader.onActionClick {
            findNavController().navigate(R.id.action_home_to_stats)
        }
        binding.homeRecentHeader.onActionClick { switchTab(R.id.wardrobeFragment) }

        recentAdapter = RecentItemsAdapter(viewLifecycleOwner.lifecycleScope) { item ->
            findNavController().navigate(R.id.action_home_to_itemDetail, bundleOf(NavArgs.ITEM_ID to item.id))
        }
        binding.homeRecentList.adapter = recentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    // Outfits and totals can change on other screens, so reload on every return.
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(state: HomeUiState) {
        binding.homeHeader.title = state.firstName
        binding.homeHeader.subtitle = getString(
            when (state.greeting) {
                Greeting.MORNING -> R.string.rw_home_greeting_morning
                Greeting.AFTERNOON -> R.string.rw_home_greeting_afternoon
                Greeting.EVENING -> R.string.rw_home_greeting_evening
            }
        )

        binding.homeLaundryCard.isVisible = state.needsWashCount > 0
        binding.homeLaundryTitle.text = resources.getQuantityString(
            R.plurals.rw_home_laundry_alert, state.needsWashCount, state.needsWashCount
        )

        renderPick(state)
        renderStats(state.summary)

        binding.homeRecentHeader.isVisible = state.recentItems.isNotEmpty()
        binding.homeRecentList.isVisible = state.recentItems.isNotEmpty()
        recentAdapter.submitList(state.recentItems)

        state.wornOutfitId?.let { outfitId ->
            viewModel.onWearHandled()
            RunwayToast.show(binding.root, R.string.rw_outfit_wear_logged)
            // Wearing something is what asks how confident you felt in it.
            findNavController().navigate(
                R.id.action_home_to_confidenceSheet,
                bundleOf(NavArgs.OUTFIT_ID to outfitId),
            )
        }

        state.errorMessage?.let {
            RunwayToast.show(binding.root, it, R.drawable.ic_rw_alert)
            viewModel.onMessageShown()
        }
    }

    private fun renderPick(state: HomeUiState) {
        binding.homePickHeader.title = getString(
            if (state.isPlanned) R.string.rw_home_planned_today else R.string.rw_home_todays_pick
        )
        val pick = state.todaysPick
        binding.homePickLoading.isVisible = state.isLoadingOutfits && pick == null
        binding.homePickCard.isVisible = pick != null
        binding.homePickEmptyCard.isVisible = !state.isLoadingOutfits && pick == null

        if (pick != null) {
            bindPick(pick, state)
            shownEmpty = 0
            return
        }
        if (state.isLoadingOutfits) return

        val mode = when {
            state.outfitsFailed -> EMPTY_FAILED
            !state.hasItems -> EMPTY_WARDROBE
            else -> EMPTY_OUTFITS
        }
        if (mode == shownEmpty) return
        shownEmpty = mode

        val empty = binding.homePickEmpty
        when (mode) {
            EMPTY_FAILED -> {
                empty.setIcon(R.drawable.ic_rw_wifi_off)
                empty.title = getString(R.string.rw_home_outfits_failed_title)
                empty.body = getString(R.string.rw_home_outfits_failed_body)
                empty.setAction(actionButton(R.string.rw_home_retry, R.drawable.ic_rw_sync) { viewModel.refresh() })
            }
            EMPTY_WARDROBE -> {
                empty.setIcon(R.drawable.ic_rw_hanger)
                empty.title = getString(R.string.rw_home_empty_wardrobe_title)
                empty.body = getString(R.string.rw_home_empty_wardrobe_body)
                // Same place the + button goes.
                empty.setAction(actionButton(R.string.rw_home_add_first_item, R.drawable.ic_rw_camera) {
                    findNavController().navigate(R.id.addItemGraph)
                })
            }
            else -> {
                empty.setIcon(R.drawable.ic_rw_sparkles)
                empty.title = getString(R.string.rw_home_no_outfits_title)
                empty.body = getString(R.string.rw_home_no_outfits_body)
                empty.setAction(actionButton(R.string.rw_home_go_to_outfits, null) { switchTab(R.id.outfitsFragment) })
            }
        }
    }

    private fun bindPick(pick: Outfit, state: HomeUiState) {
        binding.homePickName.text = pick.name
        binding.homePickPieces.text = resources.getQuantityString(
            R.plurals.rw_home_pieces, pick.itemIds.size, pick.itemIds.size
        )
        binding.homePickOpen.setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_outfitDetail,
                bundleOf(NavArgs.OUTFIT_ID to pick.id),
            )
        }

        binding.homeSuggestButton.isVisible = state.canSuggestAnother
        binding.homeWearButton.isEnabled = !state.isWearing
        binding.homeWearButton.setText(if (state.isWearing) R.string.rw_home_wearing else R.string.rw_home_wear_this)

        binding.homePickThumb.bind(pick, state.itemsById, viewLifecycleOwner.lifecycleScope)
    }

    private fun renderStats(summary: WardrobeSummary) {
        binding.homeStatItems.value = summary.itemCount.toString()
        binding.homeStatOutfits.value = summary.outfitCount?.toString() ?: getString(R.string.rw_stat_unknown)
        binding.homeStatWears.value = summary.totalWears.toString()
        binding.homeStatValue.value = getString(R.string.rw_wardrobe_price, summary.totalValue)
    }

    private fun actionButton(@StringRes label: Int, @DrawableRes icon: Int?, onClick: () -> Unit): MaterialButton =
        MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonStyle).apply {
            setText(label)
            icon?.let { setIconResource(it) }
            setOnClickListener { onClick() }
        }

    // Goes through the bottom bar so the tab highlight and back stack stay right.
    private fun switchTab(tabId: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = tabId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownEmpty = 0
        _binding = null
    }

    private companion object {
        const val EMPTY_FAILED = 1
        const val EMPTY_WARDROBE = 2
        const val EMPTY_OUTFITS = 3
    }
}
