package com.example.runway.ui.additem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentCutoutBinding
import kotlinx.coroutines.launch

// Second step of adding an item: lift the garment off its background (IIE, 2026; Google, 2026b).
// The cut-out is a convenience, never a requirement: if the model is still
// downloading or finds nothing, the original photo carries on through the flow.

class CutoutFragment : Fragment() {

    private var _binding: FragmentCutoutBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: AddItemViewModel by activityViewModels { AddItemViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCutoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cutoutHeader.title = getString(R.string.rw_cutout_title)
        binding.cutoutHeader.onBackClick { findNavController().navigateUp() }

        binding.cutoutToggle.setOnCheckedChangeListener { _, checked -> viewModel.onUseCutout(checked) }
        binding.cutoutRetryButton.setOnClickListener { viewModel.onCutOut() }
        binding.cutoutContinueButton.setOnClickListener {
            findNavController().navigate(R.id.action_cutout_to_tagItem)
        }

        // Run once on arrival rather than on a button, so the preview is already
        // there by the time the user has read the screen.
        if (viewModel.state.value.cutout == null && !viewModel.state.value.cutoutFailed) {
            viewModel.onCutOut()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: AddItemState) {
        binding.cutoutPreview.setImageBitmap(state.chosen)
        binding.cutoutProgress.isVisible = state.isProcessing

        val hasCutout = state.cutout != null
        binding.cutoutToggle.isVisible = hasCutout
        binding.cutoutToggle.isChecked = state.useCutout

        binding.cutoutMessage.isVisible = state.cutoutFailed
        binding.cutoutRetryButton.isVisible = state.cutoutFailed && !state.isProcessing
        binding.cutoutMessage.setText(R.string.rw_cutout_failed)

        binding.cutoutContinueButton.isEnabled = !state.isProcessing && state.chosen != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026b. Subject segmentation. [online] Available at: <https://developers.google.com/ml-kit/vision/subject-segmentation> [Accessed 15 September 2026].
*/
