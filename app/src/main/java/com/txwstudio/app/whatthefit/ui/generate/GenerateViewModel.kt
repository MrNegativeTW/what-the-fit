package com.txwstudio.app.whatthefit.ui.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.prefs.SelectionPreferences
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class GenerateViewModel @Inject constructor(
    repository: WardrobeRepository,
    private val selectionPreferences: SelectionPreferences,
) : ViewModel() {
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    init {
        viewModelScope.launch {
            val remembered = selectionPreferences.selectedIdsOrNull.first()
            _selectedIds.value = remembered ?: run {
                // First launch: default to all categories once they have loaded.
                val cats =
                    withTimeoutOrNull(2_000) { categories.first { it.isNotEmpty() } }.orEmpty()
                cats.map { it.id }.toSet()
            }
        }
    }

    fun toggle(id: Long) {
        val updated =
            if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
        _selectedIds.value = updated
        viewModelScope.launch { selectionPreferences.setSelectedIds(updated) }
    }
}
