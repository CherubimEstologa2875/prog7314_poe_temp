package com.example.runway.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.auth.AuthSession
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.WardrobeSummary
import com.example.runway.domain.model.initialsOf
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.ProfileRepository
import com.example.runway.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class ProfileViewModel(
    private val itemRepository: ItemRepository,
    private val wardrobeRepository: WardrobeRepository,
    private val profileRepository: ProfileRepository,
    currentSession: () -> AuthSession?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var items: List<Item> = emptyList()
    private var liveSummary: WardrobeSummary? = null

    init {
        // The session is on the device, so the card fills in straight away even offline.
        val session = currentSession()
        _uiState.update {
            it.copy(
                name = session?.displayName.orEmpty(),
                email = session?.email.orEmpty(),
                initials = initialsOf(session?.displayName, session?.email),
            )
        }

        viewModelScope.launch {
            itemRepository.observeItems().collect { latest ->
                items = latest
                _uiState.update { it.copy(summary = currentSummary()) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // A failed profile call just leaves the session details showing.
            profileRepository.profile().onSuccess { profile ->
                _uiState.update {
                    val name = profile.displayName.ifBlank { it.name }
                    val email = profile.email.ifBlank { it.email }
                    it.copy(
                        name = name,
                        email = email,
                        initials = initialsOf(name, email),
                        memberSince = parseMonth(profile.createdAt),
                    )
                }
            }
        }
        viewModelScope.launch {
            liveSummary = wardrobeRepository.summary().getOrNull()
            _uiState.update { it.copy(summary = currentSummary()) }
        }
    }

    private fun currentSummary(): WardrobeSummary =
        liveSummary ?: WardrobeSummary.fromItems(items)

    private fun parseMonth(iso: String?): YearMonth? =
        iso?.let { runCatching { YearMonth.from(Instant.parse(it).atZone(zone)) }.getOrNull() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val container = app.container
                ProfileViewModel(
                    itemRepository = container.itemRepository,
                    wardrobeRepository = container.wardrobeRepository,
                    profileRepository = container.profileRepository,
                    currentSession = { container.authSessionStore.currentSession },
                )
            }
        }
    }
}
