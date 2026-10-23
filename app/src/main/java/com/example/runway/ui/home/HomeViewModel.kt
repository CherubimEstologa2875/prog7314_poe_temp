package com.example.runway.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.auth.AuthSession
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.domain.model.firstNameOf
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.PlanRepository
import com.example.runway.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeViewModel(
    private val itemRepository: ItemRepository,
    private val outfitRepository: OutfitRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val planRepository: PlanRepository,
    private val currentSession: () -> AuthSession?,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var items: List<Item> = emptyList()
    private var outfits: List<Outfit>? = null
    private var liveSummary: WardrobeSummary? = null
    // Today's planned outfit, if there is one.
    private var planned: Outfit? = null
    // How many times "suggest another" has been tapped.
    private var skip = 0

    init {
        val session = currentSession()
        _uiState.update {
            it.copy(
                greeting = Greeting.forHour(clock().hour),
                firstName = firstNameOf(session?.displayName, session?.email),
            )
        }

        // Room works offline, so the item parts of Home never wait on the network.
        viewModelScope.launch {
            itemRepository.observeItems().collect { latest ->
                items = latest.filterNot { it.archived }
                _uiState.update {
                    it.copy(
                        recentItems = items.sortedByDescending { item -> item.addedAt() }.take(RECENT_LIMIT),
                        itemsById = items.associateBy { item -> item.id },
                        hasItems = items.isNotEmpty(),
                        summary = currentSummary(),
                    )
                }
            }
        }
    }

    // Called every time Home comes back into view.
    fun refresh() {
        _uiState.update { it.copy(greeting = Greeting.forHour(clock().hour)) }
        loadOutfits()
        loadSummary()
    }

    fun onSuggestAnother() {
        skip++
        _uiState.update { it.copy(todaysPick = pickForToday()) }
    }

    fun onWearToday() {
        val pick = _uiState.value.todaysPick ?: return
        if (_uiState.value.isWearing) return

        _uiState.update { it.copy(isWearing = true) }
        viewModelScope.launch {
            outfitRepository.logWear(pick.id)
                .onSuccess {
                    _uiState.update { it.copy(isWearing = false, wornOutfitId = pick.id) }
                    // Wear counts changed on the server, so pull them back down.
                    itemRepository.refresh()
                    loadSummary()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isWearing = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    fun onWearHandled() = _uiState.update { it.copy(wornOutfitId = null) }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun loadOutfits() {
        _uiState.update { it.copy(isLoadingOutfits = outfits == null, outfitsFailed = false) }
        viewModelScope.launch {
            outfitRepository.list()
                .onSuccess { loaded ->
                    outfits = loaded
                    // A planner failure just means falling back to the daily rotation.
                    val today = clock().toLocalDate()
                    val plan = planRepository.between(today, today).getOrNull()?.firstOrNull()
                    planned = plan?.let { p -> loaded.firstOrNull { it.id == p.outfitId } }
                    _uiState.update {
                        it.copy(
                            isLoadingOutfits = false,
                            todaysPick = pickForToday(),
                            isPlanned = planned != null,
                            // The plan is the plan, so there's nothing else to suggest.
                            canSuggestAnother = planned == null && wearable().size > 1,
                            summary = currentSummary(),
                        )
                    }
                }
                .onFailure {
                    // Keep showing the last pick if we already had one.
                    _uiState.update { it.copy(isLoadingOutfits = false, outfitsFailed = outfits == null) }
                }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            liveSummary = wardrobeRepository.summary().getOrNull()
            _uiState.update { it.copy(summary = currentSummary()) }
        }
    }

    // The server's numbers when we have them, otherwise what Room can tell us.
    private fun currentSummary(): WardrobeSummary =
        liveSummary ?: WardrobeSummary.fromItems(items, outfits?.size)

    // The API refuses to log a wear for an outfit with nothing in it.
    private fun wearable(): List<Outfit> =
        outfits.orEmpty().filter { it.itemIds.isNotEmpty() }.sortedBy { it.id }

    // Same outfit all day, a different one tomorrow.
    private fun pickForToday(): Outfit? {
        planned?.let { return it }
        val choices = wearable()
        if (choices.isEmpty()) return null
        return choices[(clock().dayOfYear + skip) % choices.size]
    }

    // Items saved before createdAt existed fall back to their last change.
    private fun Item.addedAt(): Long = if (createdAt > 0) createdAt else updatedAt

    companion object {
        const val RECENT_LIMIT = 8

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val container = app.container
                HomeViewModel(
                    itemRepository = container.itemRepository,
                    outfitRepository = container.outfitRepository,
                    wardrobeRepository = container.wardrobeRepository,
                    planRepository = container.planRepository,
                    currentSession = { container.authSessionStore.currentSession },
                )
            }
        }
    }
}
