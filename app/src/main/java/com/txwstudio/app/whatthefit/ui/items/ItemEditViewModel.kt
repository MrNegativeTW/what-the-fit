package com.txwstudio.app.whatthefit.ui.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.Season
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.navigation.WtfRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val itemId: Long = savedStateHandle[WtfRoutes.ARG_ITEM_ID] ?: 0L
    val isEditMode: Boolean = itemId != 0L

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val brands: StateFlow<List<Tag>> = repository.observeTags(TagKind.BRAND)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val colors: StateFlow<List<Tag>> = repository.observeTags(TagKind.COLOR)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val occasions: StateFlow<List<Tag>> = repository.observeTags(TagKind.OCCASION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val fits: StateFlow<List<Tag>> = repository.observeTags(TagKind.FIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var name by mutableStateOf("")
        private set
    var selectedCategoryIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var selectedTagIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var selectedSeasons by mutableStateOf<Set<Int>>(emptySet())
        private set
    var notes by mutableStateOf("")
        private set
    var isAvailable by mutableStateOf(true)
        private set

    private var savedSnapshot = snapshot()

    /** True once the form differs from its loaded state (for add mode, its initial empty state). */
    val isDirty: Boolean get() = snapshot() != savedSnapshot

    private fun snapshot() = FormSnapshot(
        name, notes, selectedCategoryIds, selectedTagIds, selectedSeasons, isAvailable,
    )

    init {
        if (isEditMode) {
            viewModelScope.launch {
                repository.getItemWithDetails(itemId)?.let { details ->
                    val item = details.item
                    name = item.name
                    isAvailable = item.isAvailable
                    selectedSeasons = Season.toList(item.seasons).toSet()
                    notes = item.notes
                    selectedCategoryIds = details.categories.map { it.id }.toSet()
                    selectedTagIds = details.tags.map { it.id }.toSet()
                    savedSnapshot = snapshot()
                }
            }
        }
    }

    val canSave: Boolean get() = name.isNotBlank()

    fun onNameChange(value: String) {
        name = value
    }

    fun onNotesChange(value: String) {
        notes = value
    }

    fun toggleCategory(id: Long) {
        selectedCategoryIds = selectedCategoryIds.toggled(id)
    }

    fun toggleTag(id: Long) {
        selectedTagIds = selectedTagIds.toggled(id)
    }

    fun toggleSeason(season: Int) {
        selectedSeasons = selectedSeasons.toggled(season)
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveItem(
                ClothingItem(
                    id = itemId,
                    name = name.trim(),
                    isAvailable = isAvailable,
                    seasons = Season.fromList(selectedSeasons),
                    notes = notes.trim(),
                ),
                selectedCategoryIds.toList(),
                selectedTagIds.toList(),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (!isEditMode) return
        viewModelScope.launch {
            repository.deleteItem(ClothingItem(id = itemId, name = name))
            onDone()
        }
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value

private data class FormSnapshot(
    val name: String,
    val notes: String,
    val categoryIds: Set<Long>,
    val tagIds: Set<Long>,
    val seasons: Set<Int>,
    val available: Boolean,
)
