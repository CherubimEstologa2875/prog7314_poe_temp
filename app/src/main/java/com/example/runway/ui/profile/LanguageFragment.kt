package com.example.runway.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.databinding.FragmentLanguageBinding
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.ui.AppLanguage
import kotlinx.coroutines.launch

// Lets the user pick English, Afrikaans or isiZulu. The choice is saved and the app switches straight away.
class LanguageFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = requireNotNull(_binding)

    // Shares the settings store with the main settings screen, so both always agree.
    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    private lateinit var ticks: Map<String, ImageView>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.languageHeader.onBackClick { findNavController().navigateUp() }

        val rows = mapOf(
            RunwaySettings.LANGUAGE_ENGLISH to binding.languageEnglish,
            RunwaySettings.LANGUAGE_AFRIKAANS to binding.languageAfrikaans,
            RunwaySettings.LANGUAGE_ZULU to binding.languageZulu,
        )
        // Every row gets a tick and only the chosen one is visible, so all the rows stay the same height.
        ticks = rows.mapValues { (code, row) ->
            row.setOnClickListener { onLanguagePicked(code) }
            tick().also { row.setTrailing(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.isLoading) showSelected(state.settings.language)
                }
            }
        }
    }

    private fun onLanguagePicked(code: String) {
        if (code == viewModel.uiState.value.settings.language) return
        Log.d(TAG, "language picked: $code")
        viewModel.onLanguageSelected(code)
        // This recreates the screen, so it comes back already in the new language.
        AppLanguage.apply(code)
    }

    private fun showSelected(code: String) {
        ticks.forEach { (rowCode, tick) -> tick.isInvisible = rowCode != code }
    }

    private fun tick() = ImageView(requireContext()).apply {
        setImageResource(R.drawable.ic_rw_check)
        contentDescription = getString(R.string.rw_language_selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "LanguageFragment"
    }
}
