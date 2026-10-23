package com.example.runway.ui.profile

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
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentProfileBinding
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Profile tab: who is signed in, the wardrobe totals, and where everything else lives. */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication

    private val viewModel: ProfileViewModel by viewModels { ProfileViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileInsightsRow.onClick { findNavController().navigate(R.id.action_profile_to_stats) }
        binding.profileSwapRow.onClick { findNavController().navigate(R.id.action_profile_to_swapMarket) }
        binding.profileListingsRow.onClick { findNavController().navigate(R.id.action_profile_to_myListings) }
        binding.profileNotificationsRow.onClick { findNavController().navigate(R.id.action_profile_to_notifications) }
        binding.profileLaundryRow.onClick { findNavController().navigate(R.id.action_profile_to_laundry) }
        binding.profileSettingsRow.onClick { findNavController().navigate(R.id.action_profile_to_settings) }

        binding.profileSignOutRow.onClick {
            RunwayDialogs.confirm(
                context = requireContext(),
                titleRes = R.string.rw_profile_sign_out_title,
                bodyRes = R.string.rw_profile_sign_out_body,
                confirmLabelRes = R.string.rw_profile_sign_out,
                destructive = true,
            ) {
                signOut()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(state: ProfileUiState) {
        binding.profileAvatar.initials = state.initials
        binding.profileName.text = state.name
        binding.profileName.isVisible = state.name.isNotBlank()
        binding.profileEmail.text = state.email.ifBlank { getString(R.string.rw_auth_no_session) }

        binding.profileMemberSince.isVisible = state.memberSince != null
        state.memberSince?.let {
            val month = it.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            binding.profileMemberSince.text = getString(R.string.rw_profile_member_since, month)
        }

        val summary = state.summary
        binding.profileStatItems.value = summary.itemCount.toString()
        binding.profileStatOutfits.value = summary.outfitCount?.toString() ?: getString(R.string.rw_stat_unknown)
        binding.profileStatValue.value = getString(R.string.rw_wardrobe_price, summary.totalValue)

        binding.profileLaundryRow.value = state.needsWashCount.takeIf { it > 0 }?.toString()
    }

    private fun signOut() {
        application.container.authSessionStore.clearSession()
        viewLifecycleOwner.lifecycleScope.launch {
            val cleared = application.container.googleAuthClient.signOut()
            val currentBinding = _binding
            if (!cleared && currentBinding != null) {
                RunwayToast.show(currentBinding.root, R.string.rw_auth_sign_out_failed)
            }
            if (isAdded) {
                findNavController().navigate(R.id.action_profile_signOut_to_signIn)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
