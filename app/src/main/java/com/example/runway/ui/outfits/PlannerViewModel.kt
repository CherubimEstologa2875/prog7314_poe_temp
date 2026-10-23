package com.example.runway.ui.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitPlan
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.PlanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class PlannerViewModel(
    private val planRepository: PlanRepository,
    private val outfitRepository: OutfitRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState(month = YearMonth.from(today())))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onPreviousMonth() = showMonth(_uiState.value.month.minusMonths(1))

    fun onNextMonth() = showMonth(_uiState.value.month.plusMonths(1))

    // Also called when a sheet plans or clears a day.
    fun refresh() {
        val month = _uiState.value.month
        val grid = gridFor(month)
        val start = today()

        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val outfits = async { outfitRepository.list() }
            val shown = async { planRepository.between(grid.first(), grid.last()) }
            val ahead = async { planRepository.between(start, start.plusDays(UPCOMING_DAYS)) }

            val outfitList = outfits.await().getOrNull()
            val shownPlans = shown.await().getOrNull()
            val aheadPlans = ahead.await().getOrNull()

            if (outfitList == null || shownPlans == null || aheadPlans == null) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            val byId = outfitList.associateBy { it.id }
            _uiState.update {
                it.copy(
                    days = grid.map { date -> dayFor(date, month, shownPlans, byId) },
                    upcoming = aheadPlans
                        .filter { plan -> byId.containsKey(plan.outfitId) }
                        .sortedBy { plan -> plan.date }
                        .take(UPCOMING_LIMIT)
                        .map { plan -> dayFor(plan.date, YearMonth.from(plan.date), aheadPlans, byId) },
                    isLoading = false,
                )
            }
        }
    }

    private fun showMonth(month: YearMonth) {
        _uiState.update { it.copy(month = month, days = emptyList()) }
        refresh()
    }

    private fun dayFor(date: LocalDate, month: YearMonth, plans: List<OutfitPlan>, outfits: Map<String, Outfit>) =
        PlannerDay(
            date = date,
            inMonth = YearMonth.from(date) == month,
            isToday = date == today(),
            // A plan whose outfit was since deleted just shows as an empty day.
            outfit = plans.firstOrNull { it.date == date }?.let { outfits[it.outfitId] },
        )

    companion object {
        const val UPCOMING_LIMIT = 4
        const val UPCOMING_DAYS = 30L

        // Six full weeks starting on the Monday on or before the 1st, so every month fits.
        fun gridFor(month: YearMonth): List<LocalDate> {
            val first = month.atDay(1)
            val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
            return (0 until 42).map { start.plusDays(it.toLong()) }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                PlannerViewModel(app.container.planRepository, app.container.outfitRepository)
            }
        }
    }
}
