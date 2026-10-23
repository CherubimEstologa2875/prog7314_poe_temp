package com.example.runway.ui.additem

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.image.SubjectCutout
import com.example.runway.data.local.ItemImageStore
import com.example.runway.domain.model.ItemDraft
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State shared by the three add-item steps (IIE, 2026).
// Scoped to the Activity so the photo taken on step one is still there on step three;
// a ViewModel per fragment would lose it at every navigation.

data class AddItemState(
    val original: Bitmap? = null,
    val cutout: Bitmap? = null,
    val useCutout: Boolean = true,
    val isProcessing: Boolean = false,
    // Set when segmentation could not run, so the screen can explain and carry on.
    val cutoutFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
) {
    // What actually gets saved and shown.
    val chosen: Bitmap? get() = if (useCutout) cutout ?: original else original
}

class AddItemViewModel(
    private val imageStore: ItemImageStore,
    private val cutter: SubjectCutout,
    private val repository: ItemRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddItemState())
    val state: StateFlow<AddItemState> = _state.asStateFlow()

    fun newCaptureFile() = imageStore.newCaptureFile()

    //hands over whatever the camera or the picker produced.
    fun onPhotoTaken(uri: Uri) {
        _state.update { it.copy(isProcessing = true, cutoutFailed = false, errorMessage = null) }
        viewModelScope.launch {
            val bitmap = imageStore.decode(uri)
            if (bitmap == null) {
                _state.update { it.copy(isProcessing = false, errorMessage = "That photo could not be read") }
                return@launch
            }
            _state.update { it.copy(original = bitmap, cutout = null, isProcessing = false) }
        }
    }

    //A failure here is not fatal: the original photo is still usable.
    fun onCutOut() {
        val source = _state.value.original ?: return
        _state.update { it.copy(isProcessing = true, cutoutFailed = false) }
        viewModelScope.launch {
            cutter.cutOut(source)
                .onSuccess { result ->
                    _state.update { it.copy(cutout = result, useCutout = true, isProcessing = false) }
                }
                .onFailure {
                    _state.update { it.copy(isProcessing = false, cutoutFailed = true, useCutout = false) }
                }
        }
    }

    fun onUseCutout(use: Boolean) = _state.update { it.copy(useCutout = use) }

    // The photo is written first so the saved row can point at it.
    fun onSave(draft: ItemDraft) {
        // The button is disabled until the draft is valid, so this is just a safety net.
        if (!draft.isValid) {
            _state.update { it.copy(errorMessage = "Check the highlighted fields") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val path = _state.value.chosen?.let { imageStore.saveCutout(it) }
                repository.save(draft.toItem(path))
            }.onSuccess {
                // The repository assigns the id, so the flow closes back to the
                // wardrobe rather than trying to open a detail screen for an id
                // it does not have.
                _state.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, errorMessage = error.message ?: "Could not save the item") }
            }
        }
    }

    // Called when the flow is left, so a half-finished item does not reappear next time.
    fun reset() {
        _state.value = AddItemState()
    }

    fun onMessageShown() = _state.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        cutter.close()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                AddItemViewModel(
                    imageStore = app.container.itemImageStore,
                    cutter = SubjectCutout(),
                    repository = app.container.itemRepository,
                )
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
