package com.example.runway.ui.wardrobe

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runway.R
import com.example.runway.databinding.FragmentColourMatcherBinding
import com.example.runway.domain.model.Item
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Smart color-matching suggestions for one garment
// Takes a [com.example.runway.ui.navigation.NavArgs.ITEM_ID] argument.

class ColourMatcherFragment : Fragment() {

    private var _binding: FragmentColourMatcherBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ColourMatcherViewModel by viewModels { ColourMatcherViewModel.Factory }

    private lateinit var adapter: WardrobeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentColourMatcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.matcherHeader.title = getString(R.string.rw_colour_matcher_title)
        binding.matcherHeader.setShowBack(true)
        binding.matcherHeader.onBackClick { findNavController().navigateUp() }

        binding.matcherEmpty.title = getString(R.string.rw_colour_matcher_empty_title)
        binding.matcherEmpty.body = getString(R.string.rw_colour_matcher_empty_body)

        adapter = WardrobeAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            onClick = ::openItem,
            matchScore = { item -> viewModel.scoreFor(item.id) },
            onUseInOutfit = ::useInOutfit,
        )
        binding.matcherList.layoutManager = LinearLayoutManager(requireContext())
        binding.matcherList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ColourMatcherUiState) {
        adapter.submitList(state.matches.map { it.item })

        val isEmpty = state.matches.isEmpty() && !state.isLoading
        binding.matcherList.isVisible = !isEmpty
        binding.matcherEmpty.isVisible = isEmpty
        binding.matcherEmpty.title = getString(
            if (state.reference != null && state.referenceColour == null) {
                R.string.rw_colour_matcher_unknown_colour
            } else R.string.rw_colour_matcher_empty_title
        )
        binding.matcherEmpty.body = getString(
            if (state.reference != null && state.referenceColour == null) {
                R.string.rw_colour_matcher_unknown_body
            } else R.string.rw_colour_matcher_empty_body
        )

        val reference = state.reference
        if (reference == null) {
            binding.matcherReferenceName.text = getString(
                if (state.isLoading) R.string.rw_item_detail_loading else R.string.rw_item_detail_not_found
            )
            binding.matcherReferenceColour.text = null
            binding.matcherReferenceSwatch.isVisible = false
            binding.matcherLead.text = null
            return
        }

        binding.matcherReferenceName.text = reference.name
        binding.matcherLead.text = getString(R.string.rw_colour_matcher_lead, reference.name)

        // The swatch only appears once the tag resolves to a colour we can score.
        val colour = state.referenceColour
        binding.matcherReferenceSwatch.isVisible = colour != null
        colour?.let { binding.matcherReferenceSwatch.swatchColor = it.argb }
        binding.matcherReferenceColour.text = colour?.label
            ?: getString(R.string.rw_colour_matcher_unknown_colour)

        loadPhoto(reference)
    }

    private fun loadPhoto(item: Item) {
        val path = item.imagePath
        if (path.isNullOrBlank()) {
            binding.matcherReferenceImage.setImageResource(R.drawable.ic_rw_plus)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                if (File(path).exists()) BitmapFactory.decodeFile(path) else null
            }
            if (bitmap != null) binding.matcherReferenceImage.setImageBitmap(bitmap)
        }
    }

    private fun openItem(item: Item) {
        findNavController().navigate(
            R.id.action_colourMatcher_to_itemDetail,
            Bundle().apply { putString(NavArgs.ITEM_ID, item.id) },
        )
    }

    // Starts a new outfit with this garment already on the canvas.
    private fun useInOutfit(item: Item) {
        findNavController().navigate(
            R.id.action_colourMatcher_to_builder,
            Bundle().apply { putString(NavArgs.ITEM_ID, item.id) },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.matcherList.adapter = null
        _binding = null
    }
}
