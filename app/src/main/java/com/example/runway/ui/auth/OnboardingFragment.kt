package com.example.runway.ui.auth

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.ui.StubFragment
import com.google.android.material.button.MaterialButton

/** Three-slide intro. Still a placeholder - see [StubFragment]. */
class OnboardingFragment : StubFragment() {
    override val titleRes = R.string.rw_onboarding_title
    override val bodyRes = R.string.rw_onboarding_body
    override val showBack = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyState.setAction(
            MaterialButton(requireContext()).apply {
                setText(R.string.rw_onboarding_continue)
                setOnClickListener {
                    findNavController().navigate(R.id.action_onboarding_to_signIn)
                }
            }
        )
    }
}
