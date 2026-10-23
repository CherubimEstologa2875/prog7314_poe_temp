package com.example.runway.ui.itemdetail

import android.graphics.BitmapFactory
import android.os.Bundle
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
import com.example.runway.databinding.FragmentItemDetailBinding
import com.example.runway.domain.model.Item
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Detail screen for one garment (IIE, 2026; Android Open Source Project, 2020c).
// Takes an [com.example.runway.ui.navigation.NavArgs.ITEM_ID] argument.

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ItemDetailViewModel by viewModels { ItemDetailViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailHeader.setShowBack(true)
        binding.detailHeader.onBackClick { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ItemDetailUiState) {
        val item = state.item
        if (item == null) {
            binding.detailHeader.title = getString(
                if (state.isLoading) R.string.rw_item_detail_loading else R.string.rw_item_detail_not_found
            )
            binding.detailUseInOutfitButton.isEnabled = false
            binding.detailFindMatchesButton.isEnabled = false
            binding.detailDeleteButton.isEnabled = false
            return
        }

        binding.detailUseInOutfitButton.isEnabled = true
        binding.detailFindMatchesButton.isEnabled = true
        binding.detailDeleteButton.isEnabled = true

        binding.detailHeader.title = item.name
        binding.detailName.text = item.name

        binding.detailMeta.text = listOfNotNull(
            item.category.lowercase().replaceFirstChar { it.uppercase() },
            item.brand?.takeIf { it.isNotBlank() },
            item.colour?.takeIf { it.isNotBlank() },
            item.size?.takeIf { it.isNotBlank() }
                ?.let { getString(R.string.rw_item_detail_size, it) },
        ).joinToString(" · ")

        binding.detailPending.isVisible = item.pendingSync

        binding.detailWearsTile.label = getString(R.string.rw_item_detail_wears)
        binding.detailWearsTile.value = item.wearCount.toString()

        binding.detailCostTile.label = getString(R.string.rw_item_detail_cost_per_wear)
        binding.detailCostTile.value = item.costPerWear
            ?.let { getString(R.string.rw_item_detail_rand, it) }
            ?: getString(R.string.rw_item_detail_no_cost)
        binding.detailCostTile.subLabel = item.purchasePrice
            ?.let { getString(R.string.rw_item_detail_paid, it) }

        loadPhoto(item)

        binding.detailUseInOutfitButton.setOnClickListener { useInOutfit(item) }
        binding.detailFindMatchesButton.setOnClickListener { findMatches(item) }
        binding.detailDeleteButton.setOnClickListener { confirmDelete(item) }
    }


    private fun loadPhoto(item: Item) {
        val path = item.imagePath
        if (path.isNullOrBlank()) {
            binding.detailImage.setImageResource(R.drawable.ic_rw_plus)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                File(path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(path) }
            }
            if (bitmap != null && _binding != null) binding.detailImage.setImageBitmap(bitmap)
        }
    }

    // Starts a new outfit with this garment already on the canvas.
    private fun useInOutfit(item: Item) {
        findNavController().navigate(
            R.id.action_itemDetail_to_builder,
            bundleOf(NavArgs.ITEM_ID to item.id),
        )
    }

    // This garment becomes the reference the colour matcher scores everything else against.
    private fun findMatches(item: Item) {
        findNavController().navigate(
            R.id.action_itemDetail_to_colourMatcher,
            bundleOf(NavArgs.ITEM_ID to item.id),
        )
    }

    private fun confirmDelete(item: Item) = RunwayDialogs.confirm(
        context = requireContext(),
        titleRes = R.string.rw_item_detail_delete_title,
        bodyRes = R.string.rw_item_detail_delete_body,
        confirmLabelRes = R.string.rw_item_detail_delete,
        destructive = true,
    ) {
        viewModel.onDelete { if (isAdded) findNavController().navigateUp() }
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
