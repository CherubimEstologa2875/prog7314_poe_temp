package com.example.runway.ui.outfits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.model.OutfitCanvas
import com.example.runway.domain.model.OutfitLayer
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OutfitBuilderViewModel(
    // Null for a new outfit, set when editing one.
    private val outfitId: String?,
    // Set when the builder was opened from a garment, so it starts on the canvas.
    startItemId: String? = null,
    private val outfitRepository: OutfitRepository,
    itemRepository: ItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutfitBuilderUiState(isEditing = outfitId != null))
    val uiState: StateFlow<OutfitBuilderUiState> = _uiState.asStateFlow()

    // The outfit being edited, so a save updates it instead of making a new one.
    private var original: Outfit? = null

    init {
        viewModelScope.launch {
            itemRepository.observeItems().collect { items ->
                _uiState.update { it.copy(wardrobe = items.filterNot { item -> item.archived }) }
            }
        }
        if (outfitId != null) loadExisting(outfitId)
        else if (startItemId != null) onAdd(startItemId)
    }

    fun loadExisting(id: String) {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            outfitRepository.get(id)
                .onSuccess { outfit ->
                    original = outfit
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            layers = OutfitCanvas.autoLayout(OutfitCanvas.layersFor(outfit)),
                            name = outfit.name,
                            occasion = outfit.occasion ?: OCCASIONS.first(),
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
        }
    }

    fun onAdd(itemId: String) = _uiState.update {
        it.copy(layers = OutfitCanvas.add(it.layers, itemId), selectedId = itemId)
    }

    fun onSelect(itemId: String?) = _uiState.update { it.copy(selectedId = itemId) }

    fun onMove(itemId: String, x: Float, y: Float) = changeLayers { OutfitCanvas.moveTo(it, itemId, x, y) }

    fun onScale(scale: Float) = changeSelected { layers, id -> OutfitCanvas.setScale(layers, id, scale) }

    fun onRotate(degrees: Float) = changeSelected { layers, id -> OutfitCanvas.setRotation(layers, id, degrees) }

    fun onBringForward() = changeSelected { layers, id -> OutfitCanvas.bringForward(layers, id) }

    fun onSendBackward() = changeSelected { layers, id -> OutfitCanvas.sendBackward(layers, id) }

    fun onRemoveSelected() {
        val id = _uiState.value.selectedId ?: return
        _uiState.update { it.copy(layers = OutfitCanvas.remove(it.layers, id), selectedId = null) }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun onOccasionChanged(occasion: String) = _uiState.update { it.copy(occasion = occasion) }

    fun onTrayCategoryChanged(category: String) = _uiState.update { it.copy(trayCategory = category) }

    // fallbackName is used when the name box is left empty.
    fun onSave(fallbackName: String) {
        val state = _uiState.value
        if (!state.canSave) return

        val ordered = OutfitCanvas.drawOrder(state.layers)
        val itemIds = ordered.map { it.itemId }
        val existing = original
        val outfit = Outfit(
            id = existing?.id.orEmpty(),
            name = state.name.trim().ifEmpty { existing?.name ?: fallbackName },
            itemIds = itemIds,
            occasion = state.occasion,
            layers = ordered,
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (existing == null) outfitRepository.create(outfit) else outfitRepository.update(outfit)
            result
                .onSuccess { saved -> _uiState.update { it.copy(isSaving = false, savedOutfitId = saved.id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) } }
        }
    }

    fun onSavedHandled() = _uiState.update { it.copy(savedOutfitId = null) }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun changeLayers(change: (List<OutfitLayer>) -> List<OutfitLayer>) =
        _uiState.update { it.copy(layers = change(it.layers)) }

    private fun changeSelected(change: (List<OutfitLayer>, String) -> List<OutfitLayer>) {
        val id = _uiState.value.selectedId ?: return
        changeLayers { change(it, id) }
    }

    companion object {
        // Stored as-is on the outfit. The screen shows a translated label for each.
        val OCCASIONS = listOf("Casual", "Work", "Evening", "Sport", "Formal")

        // Same categories the tag screen offers.
        val CATEGORIES = listOf("TOP", "BOTTOM", "OUTERWEAR", "SHOES", "OTHER")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                OutfitBuilderViewModel(
                    outfitId = handle.get<String>(NavArgs.OUTFIT_ID),
                    startItemId = handle.get<String>(NavArgs.ITEM_ID),
                    outfitRepository = app.container.outfitRepository,
                    itemRepository = app.container.itemRepository,
                )
            }
        }
    }
}
