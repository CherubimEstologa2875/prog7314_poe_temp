package com.example.runway.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.data.auth.SignInOutcome
import com.example.runway.databinding.FragmentSigninBinding
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import kotlinx.coroutines.launch

// Google sign-in screen for the Runway app (IIE, 2026; Android Open Source Project, 2020c).

// Google sign-in screen with validation, safe errors and session persistence.
class SignInFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val application get() = requireActivity().application as RunwayApplication
    private val sessionStore get() = application.container.authSessionStore
    private val googleAuthClient get() = application.container.googleAuthClient
    private var biometricAvailable = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricAvailable = isBiometricAvailable()
        binding.authFingerprintSwitch.isChecked = biometricAvailable
        binding.authFingerprintSwitch.isEnabled = biometricAvailable
        // Say why the switch is off rather than just greying it out.
        binding.authFingerprintUnavailable.isVisible = !biometricAvailable

        binding.authGoogleButton.setOnClickListener { startSignIn() }
    }

    private fun startSignIn() {
        Log.d(TAG, "sign in started")
        binding.authError.isVisible = false
        setBusy(true)

        viewLifecycleOwner.lifecycleScope.launch {
            // requireActivity() rather than the fragment: the account sheet needs a window.
            val outcome = googleAuthClient.signIn(requireActivity())
            if (_binding == null) return@launch
            setBusy(false)
            Log.d(TAG, "sign in finished: ${outcome::class.simpleName}")

            when (outcome) {
                is SignInOutcome.Success -> onSignedIn(outcome)
                SignInOutcome.Failure.Cancelled -> showError(R.string.rw_auth_cancelled)
                SignInOutcome.Failure.NoAccount -> showError(R.string.rw_auth_no_account)
                SignInOutcome.Failure.Unregistered -> showError(R.string.rw_auth_unregistered)
                SignInOutcome.Failure.Configuration -> showError(R.string.rw_auth_config_error)
                SignInOutcome.Failure.Network -> showError(R.string.rw_auth_network_error)
                SignInOutcome.Failure.IncompleteAccount ->
                    showError(R.string.rw_auth_invalid_account)
                SignInOutcome.Failure.Failed -> showError(R.string.rw_auth_sign_in_failed)
            }
        }
    }

    private fun onSignedIn(outcome: SignInOutcome.Success) {
        val useBiometric = binding.authFingerprintSwitch.isChecked && biometricAvailable
        sessionStore.saveSession(outcome.session.copy(biometricEnabled = useBiometric))

        val destination = if (useBiometric) {
            R.id.action_signIn_to_biometric
        } else {
            R.id.action_signIn_to_home
        }
        Log.d(TAG, if (useBiometric) "signed in, going to biometric" else "signed in, going home")
        findNavController().navigate(destination)
    }

    // Swaps the Google logo for a spinner while the account sheet is up, like the mockup.
    private fun setBusy(busy: Boolean) {
        val button = binding.authGoogleButton
        button.isEnabled = !busy
        if (busy) {
            val spec = CircularProgressIndicatorSpec(requireContext(), null)
            spec.indicatorSize = resources.getDimensionPixelSize(R.dimen.rw_space_20)
            spec.trackThickness = resources.getDimensionPixelSize(R.dimen.rw_border_width) * 2
            button.icon = IndeterminateDrawable.createCircularDrawable(requireContext(), spec)
            button.setText(R.string.rw_auth_signing_in)
        } else {
            button.setIconResource(R.drawable.ic_rw_google)
            button.setText(R.string.rw_auth_google_button)
        }
    }

    private fun showError(messageRes: Int) {
        if (_binding == null) return
        binding.authError.setText(messageRes)
        binding.authError.isVisible = true
    }

    private fun isBiometricAvailable(): Boolean = BiometricManager.from(requireContext())
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "SignInFragment"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].
*/
