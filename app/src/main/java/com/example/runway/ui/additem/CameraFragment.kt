package com.example.runway.ui.additem

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentCameraBinding
import com.example.runway.ui.components.RunwayToast
import kotlinx.coroutines.launch

// First step of adding an item: photograph the garment, or pick a photo of it
// (IIE, 2026; Android Open Source Project, 2026).

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = requireNotNull(_binding)

    // Shared with the other two steps, so the photo survives navigation.
    private val viewModel: AddItemViewModel by activityViewModels { AddItemViewModel.Factory }

    // Where the camera app is told to write. Kept so the result can be read back.
    private var pendingCaptureUri: Uri? = null

    // No CAMERA permission is declared or needed: the system camera app takes the
    // picture and hands back only the one file this app asked it to write.
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCaptureUri
        if (saved && uri != null) viewModel.onPhotoTaken(uri) else pendingCaptureUri = null
    }

    // GetContent is supported by the system document picker on older Android
    // versions as well as newer devices, so choosing a photo cannot fail when
    // the newer Photo Picker provider is unavailable.
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onPhotoTaken(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraHeader.title = getString(R.string.rw_camera_title)
        binding.cameraHeader.onBackClick { leaveFlow() }

        binding.cameraTakeButton.setOnClickListener { takePhoto() }
        binding.cameraPickButton.setOnClickListener {
            pickPhoto.launch("image/*")
        }
        binding.cameraContinueButton.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_cutout)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun takePhoto() {
        val file = viewModel.newCaptureFile()
        val uri = FileProvider.getUriForFile(requireContext(), authority(), file)
        pendingCaptureUri = uri
        takePicture.launch(uri)
    }

    private fun render(state: AddItemState) {
        binding.cameraPreview.setImageBitmap(state.original)
        binding.cameraPreview.isVisible = state.original != null
        binding.cameraEmpty.isVisible = state.original == null
        binding.cameraContinueButton.isEnabled = state.original != null
        binding.cameraTakeButton.setText(
            if (state.original == null) R.string.rw_camera_take else R.string.rw_camera_retake
        )

        state.errorMessage?.let {
            RunwayToast.show(requireView(), it)
            viewModel.onMessageShown()
        }
    }

    // Leaving the flow discards the half-finished item rather than keeping it
    // around to surprise the user next time they tap the button.
    private fun leaveFlow() {
        viewModel.reset()
        findNavController().navigateUp()
    }

    private fun authority() = "${requireContext().packageName}.fileprovider"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2026. Photo picker. [online] Available at: <https://developer.android.com/training/data-storage/shared/photopicker> [Accessed 15 September 2026].
*/
