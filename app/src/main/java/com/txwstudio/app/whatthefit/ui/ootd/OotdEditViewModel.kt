package com.txwstudio.app.whatthefit.ui.ootd

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.OotdPhotoStore
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.OotdRecord
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.ui.navigation.WtfRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OotdEditViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val photoStore: OotdPhotoStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val ootdId: Long = savedStateHandle[WtfRoutes.ARG_OOTD_ID] ?: 0L
    val isEditMode: Boolean = ootdId != 0L

    /** Parts to choose items for, in display order. */
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Available items per category, for the per-part pickers. */
    var itemsByCategory by mutableStateOf<Map<Long, List<ClothingItem>>>(emptyMap())
        private set

    var epochDay by mutableStateOf(LocalDate.now().toEpochDay())
        private set

    /** Chosen item per part: categoryId to itemId. */
    var selectedItems by mutableStateOf<Map<Long, Long>>(emptyMap())
        private set

    var notes by mutableStateOf("")
        private set

    var photoPath by mutableStateOf<String?>(null)
        private set

    // Internal file the camera is about to write to; promoted to [photoPath] on a successful capture.
    private var pendingCameraPath: String? = null

    private var savedSnapshot = snapshot()

    /** True once the form differs from its loaded state (for add mode, its initial empty state). */
    val isDirty: Boolean get() = snapshot() != savedSnapshot

    private fun snapshot() = OotdSnapshot(epochDay, selectedItems, notes, photoPath)

    init {
        viewModelScope.launch {
            categories.collect { cats ->
                itemsByCategory = cats.associate { it.id to repository.getAvailableItems(it.id) }
            }
        }
        if (isEditMode) {
            viewModelScope.launch {
                repository.getOotd(ootdId)?.let { ootd ->
                    epochDay = ootd.record.epochDay
                    notes = ootd.record.notes
                    photoPath = ootd.record.photoPath
                    selectedItems = ootd.slots.mapNotNull { slot ->
                        val categoryId = slot.category?.id ?: return@mapNotNull null
                        val itemId = slot.item?.id ?: return@mapNotNull null
                        categoryId to itemId
                    }.toMap()
                    savedSnapshot = snapshot()
                }
            }
        }
    }

    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
    val canSave: Boolean get() = selectedItems.isNotEmpty()

    fun onDateChange(newEpochDay: Long) { epochDay = newEpochDay }
    fun onNotesChange(value: String) { notes = value }
    fun selectItem(categoryId: Long, itemId: Long) { selectedItems = selectedItems + (categoryId to itemId) }
    fun clearItem(categoryId: Long) { selectedItems = selectedItems - categoryId }

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch { photoPath = photoStore.copyIntoInternal(uri) }
    }

    /** Returns the FileProvider Uri the camera should write the capture to. */
    fun prepareCameraOutput(): Uri {
        val output = photoStore.newCameraOutput()
        pendingCameraPath = output.path
        return output.uri
    }

    fun onPhotoCaptured(success: Boolean) {
        if (success) {
            photoPath = pendingCameraPath
        } else {
            photoStore.delete(pendingCameraPath)
        }
        pendingCameraPath = null
    }

    fun removePhoto() { photoPath = null }

    fun save(onDone: () -> Unit) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            repository.saveOotd(
                id = ootdId,
                epochDay = epochDay,
                notes = notes.trim(),
                photoPath = photoPath,
                slots = selectedItems.map { (categoryId, itemId) -> categoryId to itemId },
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (!isEditMode) return
        viewModelScope.launch {
            repository.deleteOotd(OotdRecord(id = ootdId, epochDay = epochDay))
            photoStore.delete(photoPath)
            onDone()
        }
    }
}

private data class OotdSnapshot(
    val epochDay: Long,
    val selectedItems: Map<Long, Long>,
    val notes: String,
    val photoPath: String?,
)
