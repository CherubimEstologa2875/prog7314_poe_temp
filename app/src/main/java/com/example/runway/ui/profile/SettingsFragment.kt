package com.example.runway.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.runway.R
import com.example.runway.RunwayApplication
import com.example.runway.databinding.FragmentSettingsBinding
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption
import com.example.runway.ui.components.ListRowView
import com.example.runway.ui.components.RunwayDialogs
import com.example.runway.ui.components.RunwayToast
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

/** Settings screen. Changes save straight away and are restored when the app reopens. */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    private lateinit var biometricsSwitch: MaterialSwitch
    private lateinit var washSwitch: MaterialSwitch
    private lateinit var swapSwitch: MaterialSwitch
    private lateinit var weatherSwitch: MaterialSwitch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsHeader.onBackClick { findNavController().navigateUp() }

        addSwitches()
        setUpListeners()
        showVersion()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.isLoading) render(state.settings)
                    binding.settingsSyncProgress.isVisible = state.isSyncing
                    state.errorMessage?.let { message ->
                        RunwayToast.show(binding.root, message)
                        viewModel.onErrorShown()
                    }
                }
            }
        }
    }

    // The list rows take their trailing control at runtime, so the switches are made here.
    private fun addSwitches() {
        biometricsSwitch = newSwitch(binding.biometricsRow)
        washSwitch = newSwitch(binding.notifyWashRow)
        swapSwitch = newSwitch(binding.notifySwapRow)
        weatherSwitch = newSwitch(binding.notifyWeatherRow)
    }

    private fun newSwitch(row: ListRowView): MaterialSwitch {
        val switch = MaterialSwitch(requireContext())
        row.setTrailing(switch)
        return switch
    }

    private fun setUpListeners() {
        // Skipping values that already match what is saved stops the dialog reopening.
        binding.themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val picked = themeFor(checkedId)
            if (picked == savedSettings().theme) return@addOnButtonCheckedListener
            confirmThemeChange(picked)
        }

        binding.languageRow.onClick {
            findNavController().navigate(R.id.action_settings_to_language)
        }

        biometricsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == savedSettings().biometricsEnabled) return@setOnCheckedChangeListener
            if (checked) {
                setBiometrics(true)
                RunwayToast.show(binding.root, R.string.rw_settings_biometrics_on)
            } else {
                confirmBiometricsOff()
            }
        }

        washSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == savedSettings().notifyWash) return@setOnCheckedChangeListener
            viewModel.onNotifyWashChanged(checked)
            toastToggle(R.string.rw_settings_notify_wash, checked)
        }

        swapSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == savedSettings().notifySwap) return@setOnCheckedChangeListener
            viewModel.onNotifySwapChanged(checked)
            toastToggle(R.string.rw_settings_notify_swap, checked)
        }

        weatherSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == savedSettings().notifyWeather) return@setOnCheckedChangeListener
            viewModel.onNotifyWeatherChanged(checked)
            toastToggle(R.string.rw_settings_notify_weather, checked)
        }

        binding.wearLimitMinus.setOnClickListener { stepWearLimit(-1) }
        binding.wearLimitPlus.setOnClickListener { stepWearLimit(1) }

        binding.resetButton.setOnClickListener { confirmReset() }
    }

    private fun render(settings: RunwaySettings) {
        binding.themeGroup.check(buttonFor(settings.theme))
        binding.languageRow.value = getString(languageLabel(settings.language))
        biometricsSwitch.isChecked = settings.biometricsEnabled
        washSwitch.isChecked = settings.notifyWash
        swapSwitch.isChecked = settings.notifySwap
        weatherSwitch.isChecked = settings.notifyWeather
        binding.wearLimitValue.text = settings.defaultWearLimit.toString()
    }

    private fun savedSettings() = viewModel.uiState.value.settings

    private fun confirmThemeChange(theme: ThemeOption) {
        RunwayDialogs.confirm(
            context = requireContext(),
            titleRes = R.string.rw_settings_theme_confirm_title,
            bodyRes = themeConfirmBody(theme),
            confirmLabelRes = R.string.rw_settings_theme_apply,
            // Put the buttons back if they change their mind.
            onCancel = { render(savedSettings()) },
        ) {
            viewModel.onThemeSelected(theme)
            applyTheme(theme)
        }
    }

    private fun confirmBiometricsOff() {
        RunwayDialogs.confirm(
            context = requireContext(),
            titleRes = R.string.rw_settings_biometrics_off_title,
            bodyRes = R.string.rw_settings_biometrics_off_body,
            confirmLabelRes = R.string.rw_settings_biometrics_off_confirm,
            destructive = true,
            onCancel = { render(savedSettings()) },
        ) {
            setBiometrics(false)
        }
    }

    private fun confirmReset() {
        RunwayDialogs.confirm(
            context = requireContext(),
            titleRes = R.string.rw_settings_reset_title,
            bodyRes = R.string.rw_settings_reset_body,
            confirmLabelRes = R.string.rw_settings_reset,
            destructive = true,
        ) {
            viewModel.onResetToDefaults()
            applyTheme(RunwaySettings().theme)
            RunwayToast.show(binding.root, R.string.rw_settings_reset_done)
        }
    }

    private fun stepWearLimit(step: Int) {
        val current = savedSettings().defaultWearLimit
        viewModel.onWearLimitChanged(current + step)
    }

    // The biometric screen reads this off the session, so keep both copies the same.
    private fun setBiometrics(enabled: Boolean) {
        viewModel.onBiometricsChanged(enabled)
        val store = (requireActivity().application as RunwayApplication).container.authSessionStore
        val session = store.currentSession ?: return
        store.saveSession(session.copy(biometricEnabled = enabled))
    }

    private fun applyTheme(theme: ThemeOption) {
        Log.d(TAG, "applying theme $theme")
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                ThemeOption.DAY -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeOption.NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun toastToggle(labelRes: Int, enabled: Boolean) {
        val message = if (enabled) R.string.rw_settings_notify_on else R.string.rw_settings_notify_off
        RunwayToast.show(binding.root, getString(message, getString(labelRes)))
    }

    private fun showVersion() {
        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.versionText.text = getString(R.string.rw_settings_version, info.versionName)
    }

    private fun themeFor(buttonId: Int) = when (buttonId) {
        R.id.themeDay -> ThemeOption.DAY
        R.id.themeNight -> ThemeOption.NIGHT
        else -> ThemeOption.SYSTEM
    }

    private fun buttonFor(theme: ThemeOption) = when (theme) {
        ThemeOption.DAY -> R.id.themeDay
        ThemeOption.NIGHT -> R.id.themeNight
        ThemeOption.SYSTEM -> R.id.themeSystem
    }

    private fun themeConfirmBody(theme: ThemeOption) = when (theme) {
        ThemeOption.DAY -> R.string.rw_settings_theme_confirm_day
        ThemeOption.NIGHT -> R.string.rw_settings_theme_confirm_night
        ThemeOption.SYSTEM -> R.string.rw_settings_theme_confirm_system
    }

    private fun languageLabel(code: String) = when (code) {
        RunwaySettings.LANGUAGE_AFRIKAANS -> R.string.rw_settings_language_af
        RunwaySettings.LANGUAGE_ZULU -> R.string.rw_settings_language_zu
        else -> R.string.rw_settings_language_en
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "SettingsFragment"
    }
}
