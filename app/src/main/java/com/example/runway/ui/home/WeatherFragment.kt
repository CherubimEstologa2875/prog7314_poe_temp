package com.example.runway.ui.home

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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.runway.R
import com.example.runway.databinding.FragmentWeatherBinding
import com.example.runway.ui.wardrobe.WardrobeAdapter
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: WeatherViewModel by viewModels { WeatherViewModel.Factory }

    private lateinit var adapter: WardrobeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.weatherHeader.onBackClick { findNavController().navigateUp() }
        binding.weatherRetry.setOnClickListener { viewModel.load() }

        adapter = WardrobeAdapter(viewLifecycleOwner.lifecycleScope, onClick = {})
        binding.weatherItems.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.weatherItems.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: WeatherUiState) {
        binding.weatherLoading.isVisible = state.isLoading
        binding.weatherFailed.isVisible = state.failed
        binding.weatherCard.isVisible = state.temperature != null

        state.temperature?.let { binding.weatherTemperature.text = getString(R.string.rw_weather_temperature, it) }
        state.condition?.let { binding.weatherCondition.setText(it) }
        state.advice?.let { binding.weatherAdvice.setText(it) }

        adapter.submitList(state.items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.weatherItems.adapter = null
        _binding = null
    }
}