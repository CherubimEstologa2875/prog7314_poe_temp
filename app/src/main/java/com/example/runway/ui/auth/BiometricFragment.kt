package com.example.runway.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication

/** Fingerprint/face unlock screen matching the returning-user mockup. */
class BiometricFragment : Fragment() {

    private var biometricView: View? = null
    private var biometricError: TextView? = null
    private var authenticationInProgress = false
    private val application get() = requireActivity().application as RunwayApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_biometric, container, false).also {
            biometricView = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = application.container.authSessionStore.currentSession
        if (session == null) {
            goToSignIn()
            return
        }

        val avatar = view.findViewById<TextView>(R.id.biometricAvatar)
        val email = view.findViewById<TextView>(R.id.biometricEmail)
        val unlockButton = view.findViewById<View>(R.id.biometricButton)
        val useAccount = view.findViewById<TextView>(R.id.biometricUseAccount)
        biometricError = view.findViewById(R.id.biometricError)

        avatar.text = initials(session.displayName, session.email)
        email.text = session.email
        unlockButton.setOnClickListener { authenticate() }
        useAccount.setOnClickListener { goToSignIn() }

        if (isBiometricAvailable()) {
            authenticate()
        } else {
            showError(getString(R.string.rw_auth_fingerprint_unavailable))
        }
    }

    private fun authenticate() {
        if (!isAdded || biometricView == null || authenticationInProgress ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) return

        authenticationInProgress = true
        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    authenticationInProgress = false
                    navigateSafely(R.id.action_biometric_to_home)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    authenticationInProgress = false
                    if (errorCode != BiometricPrompt.ERROR_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        showError(getString(R.string.rw_biometric_failed))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showError(getString(R.string.rw_biometric_failed))
                }
            },
        )

        try {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.rw_biometric_title))
                    .setSubtitle(getString(R.string.rw_biometric_helper))
                    .setNegativeButtonText(getString(R.string.rw_biometric_use_account))
                    .build()
            )
        } catch (_: Exception) {
            authenticationInProgress = false
            showError(getString(R.string.rw_biometric_failed))
        }
    }

    private fun isBiometricAvailable(): Boolean = BiometricManager.from(requireContext())
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

    private fun goToSignIn() {
        authenticationInProgress = false
        navigateSafely(R.id.action_biometric_to_signIn)
    }

    private fun navigateSafely(actionId: Int) {
        if (!isAdded || !lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.biometricFragment) return
        runCatching { navController.navigate(actionId) }
    }

    private fun showError(message: String) {
        biometricError?.text = message
    }

    private fun initials(displayName: String, email: String): String {
        val source = displayName.ifBlank { email.substringBefore('@') }
        return source.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().toString().uppercase() }
            .take(2)
    }

    override fun onDestroyView() {
        authenticationInProgress = false
        biometricError = null
        biometricView = null
        super.onDestroyView()
    }
}
